# Clatos Dialer — Technical Architecture

## Overview

Clatos Dialer is a native Android **default phone app** that records calls and pushes call logs +
recordings to a **PHPMaker CRM** over REST. It is distributed via **sideload / MDM** to a controlled
fleet, which lets us use the default-dialer role, an accessibility-assisted recording fallback, and
(where available) Device-Owner privileges.

- **Language/UI:** Kotlin, Jetpack Compose, Material 3
- **Architecture:** MVVM + Clean Architecture (UI → ViewModel → UseCase/Repository → data sources)
- **DI:** Hilt
- **Networking:** Retrofit + OkHttp + Kotlinx Serialization
- **Persistence:** Room (queue + caches), DataStore + Keystore-backed encryption (tokens)
- **Background work:** WorkManager (sync), foreground service (active call + recording)
- **minSdk 26**, **targetSdk = latest stable** (e.g., 35/36; forward-compatible)

---

## Module Map

```
clatos-dialer/
├── app/              # Application class, Hilt graph, MainActivity, navigation host
├── core/
│   ├── common/       # Result/Either, dispatchers, time, phone-number normalization
│   ├── designsystem/ # Compose theme, shared components
│   ├── network/      # Retrofit, OkHttp, AuthInterceptor, CrmApi, DTOs, mappers
│   ├── database/     # Room db, entities, DAOs (CallLog, Contact, Session, RecordingCapability)
│   └── datastore/    # encrypted session/token store, settings
├── feature/
│   ├── auth/         # login screen + session gate
│   ├── onboarding/   # permissions + default-dialer role flow
│   ├── dialer/       # dialpad, in-call screen, in-call controller
│   ├── calllog/      # history list + sync status
│   └── contacts/     # unified list, profile, create
├── telephony/        # InCallService, CallScreeningService, CallStateObserver
├── recording/        # RecordingEngine + strategies + RecordingSelfTest + foreground service
└── sync/             # repositories + WorkManager workers (CallSyncWorker, ContactSyncWorker)
```

Dependency rule: `feature/*` and `telephony`/`recording`/`sync` depend on `core/*`; `core/*` modules
do not depend on features. `app` wires everything.

---

## Telephony Layer

The app holds **`RoleManager.ROLE_DIALER`** to become the default phone app, which is the prerequisite
for an `InCallService`-driven in-call experience.

- **`ClatosInCallService`** (`telephony/`) — receives `Call` objects, exposes call state to the
  in-call UI, and emits start/end events that drive recording and call-log creation.
- **`ClatosCallScreeningService`** — observes incoming calls for early caller-ID resolution.
- **`CallStateObserver`** — `TelephonyCallback` (API 31+) with `PhoneStateListener` fallback to
  detect RINGING/OFFHOOK/IDLE transitions and **missed** calls.
- **Outgoing calls** placed via `TelecomManager.placeCall(...)`.
- **Contacts** read via `ContactsContract`; optional write for created contacts (decision pending).

Call lifecycle → on call connected: start recording (if capable) + create in-memory call record;
on call ended: stop recording, finalize file, persist `CallLog(PENDING)`, enqueue sync.

---

## Recording Engine

The single biggest technical risk. Since Android 9/10, non-system apps generally cannot capture the
**remote party's** audio via `VOICE_CALL`. We therefore use a **pluggable engine with prioritized
strategies** plus a runtime **self-test**, and we always degrade gracefully.

```kotlin
interface RecordingEngine {
    val strategy: RecordingStrategy
    suspend fun isAvailable(): Boolean         // cheap capability probe
    fun start(output: File)                    // begin recording active call
    fun stop(): RecordingResult                // finalize; returns file + quality
}

enum class RecordingStrategy { PRIVILEGED, VOICE_CALL, VOICE_RECOGNITION, ACCESSIBILITY, MIC, NONE }
```

**Selection priority** (best → fallback):
1. **PRIVILEGED** — Device Owner / platform-signed / privileged permission grant on managed devices.
2. **VOICE_CALL** — `MediaRecorder` with `AudioSource.VOICE_CALL` (works on some OEMs/older versions).
3. **VOICE_RECOGNITION** — alternative source that captures usable audio on certain devices.
4. **ACCESSIBILITY** — accessibility-service-assisted capture where the OEM permits it.
5. **MIC** — `AudioSource.MIC` (local side reliable; remote side OEM/speakerphone dependent).
6. **NONE** — no viable strategy; recording disabled, capability reported as degraded.

**Recording Self-Test (`RecordingSelfTest`)** runs on first launch and after OS/app updates:
records a short sample with each strategy in order, measures audio energy/validity, selects the first
viable one, and persists the result in `RecordingCapability` (also reported to the CRM for admin
visibility). Output format: **AAC in `.m4a`**, app-private storage.

Active recording runs inside a **foreground service** with `FOREGROUND_SERVICE_MICROPHONE` and a
persistent notification (also supports call-recording disclosure requirements).

> **Honest constraint:** on the strictest current/future Android versions, only the PRIVILEGED path
> reliably captures both parties. The architecture makes that path first-class for MDM fleets while
> giving best-effort capture (and clear reporting) everywhere else.

---

## Sync Layer (offline-first)

1. Each ended call → `CallLog(syncStatus = PENDING)` + recording file path persisted in Room.
2. **`CallSyncWorker`** (WorkManager, `NetworkType.CONNECTED`, exponential backoff) uploads each
   pending row as **multipart** (`metadata` JSON part + `recording` file part) to `POST /api/calllogs`.
3. On success → `SYNCED` + store CRM `remoteId`; prune local audio per retention policy.
4. On failure → increment `attempts`, retry with backoff; partial-batch safe (per-item commit).
5. **`ContactSyncWorker`** pulls CRM contacts incrementally (`GET /api/contacts?since=`) into cache.

Sync is also triggered opportunistically on connectivity regained and on a periodic WorkManager schedule.

---

## Authentication & Session

- Login → `POST /api/login` → bearer token (PHPMaker JWT or API key — to confirm).
- Token stored via **DataStore + EncryptedSharedPreferences** (Android Keystore master key).
- **`AuthInterceptor`** attaches `Authorization: Bearer <token>` to every request.
- **401** → attempt refresh (if supported) else clear session and force re-login. Pending sync data is preserved.
- App routing is gated: unauthenticated → login; authenticated but un-onboarded → onboarding; else → home.

---

## Data Model (Room)

```
UserSession(userId, displayName, token[encrypted ref], expiresAt)
Contact(id, source: DEVICE|CRM, crmId?, deviceLookupKey?, name, primaryNumber,
        numbers[], company?, photoUri?, updatedAt)
CallLog(id, direction: IN|OUT, number, normalizedNumber, displayName?, startedAt, durationSec,
        status: INCOMING|OUTGOING|MISSED, recordingPath?, recordingStrategy, recordingState: OK|FAILED|NONE,
        syncStatus: PENDING|SYNCED|FAILED, attempts, remoteId?)
RecordingCapability(deviceModel, osVersion, selectedStrategy, lastTestedAt, degraded: Boolean)
```

Phone numbers are normalized (E.164 where possible) for matching/dedupe across device & CRM.

---

## Security & Privacy

- Tokens & session encrypted at rest (Keystore).
- Recordings in app-private storage; pruned after upload.
- TLS for all CRM traffic; certificate handling per environment.
- In-call notification supports recording disclosure; legal consent policy to confirm per jurisdiction.
- No analytics SDKs that exfiltrate PII; telemetry limited to device/recording health to the CRM.

---

## Tech Stack & Key Libraries

| Concern | Choice |
|---|---|
| UI | Jetpack Compose, Material 3, Navigation-Compose |
| DI | Hilt |
| Async | Kotlin Coroutines + Flow |
| Networking | Retrofit, OkHttp, Kotlinx Serialization converter |
| DB | Room (+ KSP) |
| Background | WorkManager |
| Secure storage | DataStore + Security-Crypto (EncryptedSharedPreferences) |
| Telephony | `android.telecom` (InCallService, TelecomManager), TelephonyCallback |
| Recording | MediaRecorder / AudioRecord per strategy; foreground service |

---

## Build & Distribution

- Multi-module Gradle (Kotlin DSL) with a **version catalog** (`gradle/libs.versions.toml`).
- Build variants: `debug`, `release` (signed for sideload/MDM).
- Future: CI to assemble & lint; managed-config (Android Enterprise) for Device-Owner provisioning.

---

## Open Items (confirm during implementation)

1. Exact PHPMaker endpoints, auth scheme, field names, multipart support (see `API_CONTRACT.md`).
2. Recording retention window after successful upload.
3. Whether created contacts also write to the device address book.
4. Call-recording consent/disclosure requirements per jurisdiction.
5. MDM / Android Enterprise availability for Device-Owner provisioning (enables PRIVILEGED recording).
