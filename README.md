# Clatos Dialer

A native Android **call-recording dialer** for a **PHPMaker CRM**. Field agents install the app,
set it as the device's default phone app, log in with CRM credentials, make/receive calls, and the
app records each call and pushes **call logs + recordings** to the CRM. Contacts are a **unified
view** of device + CRM contacts; agents can create and open (CRM profile) contacts but **cannot delete**.

> Distributed via **sideload / MDM** to a controlled device fleet (not the Play Store).

## Documentation

- [`docs/USER_STORIES.md`](docs/USER_STORIES.md) — agile backlog: epics, stories, acceptance criteria, sprint plan.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — modules, tech stack, recording strategy, data model, security.
- [`docs/API_CONTRACT.md`](docs/API_CONTRACT.md) — the CRM REST endpoints the app expects (confirm vs real API).
- [`docs/PRODUCTION_READINESS.md`](docs/PRODUCTION_READINESS.md) — step-by-step guide to take this to production.
- [`docs/PROJECT_STATUS.md`](docs/PROJECT_STATUS.md) — what's done vs. what needs external input.
- [`docs/DEVICE_MATRIX.md`](docs/DEVICE_MATRIX.md) — per-device recording compatibility tracker.

## ⚠️ Recording reality (read this)

Since Android 9/10, non-system apps generally **cannot capture the remote party's audio** via the
`VOICE_CALL` source. The app uses a **pluggable `RecordingEngine`** with prioritized strategies
(`PRIVILEGED` → `VOICE_CALL` → `VOICE_RECOGNITION` → `ACCESSIBILITY` → `MIC`) and a **first-run
self-test** that picks the best working method and reports capability to the CRM. On the strictest
modern devices, only the **Device-Owner / privileged** path (via MDM provisioning) reliably records
both parties. The app always degrades gracefully: the call completes and the log is captured even
when recording isn't possible.

## Tech stack

Kotlin · Jetpack Compose (Material 3) · MVVM + Clean Architecture · Hilt · Retrofit/OkHttp +
Kotlinx Serialization · Room · WorkManager · DataStore + Keystore-backed encryption ·
`android.telecom` (default-dialer role, `InCallService`).

## Project structure

This scaffold is a single `app` module whose packages mirror the planned modules (see
`docs/ARCHITECTURE.md`). It can be split into Gradle modules later without moving package boundaries.

```
app/src/main/java/com/clatos/dialer/
├── core/common        # phone-number normalization, shared utils
├── core/network       # Retrofit CrmApi, DTOs, AuthInterceptor, DI
├── core/database      # Room entities, DAOs, database, DI
├── core/datastore     # encrypted SessionStore
├── feature/auth       # login + session gate
├── feature/onboarding # permissions + default-dialer role
├── feature/dialer     # dialpad + place call
├── feature/calllog    # history + sync status
├── feature/contacts   # unified list, create, profile
├── telephony          # InCallService, CallScreeningService, in-call bridge
├── recording          # RecordingEngine + strategies + self-test + foreground service
└── sync               # repositories + CallSyncWorker (WorkManager)
```

## Build

Requires the Android SDK and JDK 17.

```bash
# Generate the Gradle wrapper jar if missing (first time):
gradle wrapper --gradle-version 8.11.1

# Configure the CRM base URL (defaults to a placeholder in app/build.gradle.kts):
#   buildConfigField("String", "CRM_BASE_URL", "\"https://your-crm/\"")

./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk` — sideload onto a test device.

## Configuration

- **CRM base URL:** `CRM_BASE_URL` build config field in `app/build.gradle.kts`.
- **API field names/auth:** adjust `core/network/dto/CrmDtos.kt` + `core/network/CrmApi.kt` to match
  the real PHPMaker API (see `docs/API_CONTRACT.md`).

## Status

- **Sprint 0 (done):** architecture, navigation, DI, networking, Room, WorkManager sync, telephony
  service skeletons, recording-engine abstraction, stub screens.
- **Sprint 1 (done):** CRM login + session validation (`GET /api/me`) + 401 handling + logout;
  state-driven gating (login → onboarding → dialer); guided permission onboarding with rationale and
  default-dialer role request; runtime-permission resilience on the dialer; settings screen; CI build.
- **Sprint 2 (done):** dialer in-call UI — `CallManager` (call state, answer/reject/hangup,
  mute/speaker/DTMF, caller-ID via CRM cache + device PhoneLookup); `InCallService` driving it;
  `InCallActivity` shown over the lock screen (full-screen-intent notification for incoming calls);
  tap-to-call from the dialpad, contacts, and history.
- **Sprint 3 (done):** recording engine end-to-end — first-run self-test wired (app start +
  onboarding), capability persisted and reported to the CRM (`DiagnosticsReporter`), mic-permission
  guard, recording indicator in the in-call UI, recording capability shown in Settings.
- **Sprint 4 (done):** call-log sync hardening — exponential backoff, periodic safety-net sync,
  manual "Sync now", offline queue + retention pruning (already in the worker).
- **Sprint 5 (done):** unified contacts — device (ContactsContract) + CRM merged & deduped by number,
  search, live CRM profile fetch (`GET /api/contacts/{id}`) with recent local calls, create pushes to
  CRM; deletion disabled by policy.
- **Sprint 6 (done):** hardening — network security config (TLS-only, no cleartext), encrypted session
  storage, manual recording re-test + telemetry, device compatibility matrix (`docs/DEVICE_MATRIX.md`).

All planned sprints (per `docs/USER_STORIES.md`) are implemented. Remaining work is real-device QA,
the PHPMaker API mapping (`docs/API_CONTRACT.md`), and release signing for distribution.

## CI

`.github/workflows/android-ci.yml` sets up JDK 17 + the Android SDK, generates the Gradle wrapper,
runs `assembleDebug` + `lintDebug`, and uploads the debug APK as a build artifact on every push/PR.

## Open items to confirm

1. Exact PHPMaker endpoints, auth scheme, and field names.
2. Recording retention window after successful upload.
3. Whether agent-created contacts also write to the device address book.
4. Call-recording consent/disclosure requirements per jurisdiction.
5. MDM / Android Enterprise availability for Device-Owner provisioning.
