# Production Readiness Guide — Clatos Dialer

This is the step-by-step checklist to take the app from "feature-complete on `main`" to a
production rollout on your agents' phones. Items are ordered by dependency. Each phase has concrete
steps and an exit criterion.

> Current state: all app features + CI build are done on `main`. What remains is integration with
> your real systems, a recording-strategy decision, signing, compliance, and on-device QA — the
> things that need your infrastructure and hardware. See `PROJECT_STATUS.md` for the gap list.

---

## Phase 0 — Decisions to lock first (blockers)

These three decisions drive everything else. Make them before writing config or buying hardware.

1. **Recording strategy / MDM** — *the single most important decision.*
   - Modern Android blocks 3rd-party apps from recording the **remote party**. Reliable both-party
     recording requires the app to run as **Device Owner** (via MDM / Android Enterprise) or be
     platform-signed.
   - **Decide:** (a) enroll company-owned devices in an MDM as Device Owner (recommended), or
     (b) accept best-effort/MIC-only recording with per-device variance.
   - Exit: chosen path + (if MDM) the MDM product selected.
2. **Device ownership model** — company-owned (fully managed) vs. agent BYOD. Company-owned is
   strongly recommended (enables Device Owner + consistent behavior + remote management).
3. **Legal/consent** — call-recording consent law varies by jurisdiction (one-party vs all-party
   consent). Confirm with counsel; decide on an audible/visible disclosure and agent training.
   - Exit: written policy + the in-call recording notification copy approved.

---

## Phase 1 — CRM API integration

Goal: the app talks to the real PHPMaker CRM, not the assumed contract.

Steps:
1. Export the real API surface (PHPMaker REST API docs, Swagger/Postman, or list the endpoints).
2. Map them to `docs/API_CONTRACT.md`. Confirm for each: path, method, auth scheme (JWT bearer vs
   API key vs session), request/response field names, pagination, and multipart support for the
   call-log upload.
3. Update the app:
   - DTOs/field names: `app/src/main/java/com/clatos/dialer/core/network/dto/CrmDtos.kt`
   - Endpoints: `core/network/CrmApi.kt`
   - Auth header scheme (if not bearer): `core/network/AuthInterceptor.kt` and
     `core/network/SessionAuthenticator.kt`
   - Base URL: `CRM_BASE_URL` build-config field in `app/build.gradle.kts` (per build variant).
4. Decide the **call-log upload** shape: confirm `POST /api/calllogs` accepts multipart (JSON
   metadata + `.m4a` file). If not, add a PHPMaker custom API action, or a two-step
   (upload file → POST metadata with file id) and adjust `CrmApi.uploadCallLog` + `CallSyncWorker`.
5. Ensure the server **dedupes on `client_call_id`** (idempotency) so retries don't duplicate logs.
6. Add server endpoints if missing: `GET /api/me`, contacts pagination + `since`, device telemetry.

Exit: login, contact list/profile/create, and a call-log+recording upload all succeed against a
staging CRM.

---

## Phase 2 — Backend & storage readiness

Goal: the CRM can receive and store recordings at scale.

Steps:
1. **Storage sizing** — estimate `agents × calls/day × avg minutes × ~0.75 MB/min` (AAC ~96 kbps).
   Provision storage + backups accordingly.
2. **Upload limits** — raise server `upload_max_filesize` / `post_max_size` (PHP) and any reverse-proxy
   body-size limits above the largest expected recording; the app handles `413` but you want headroom.
3. **Retention policy** — define how long recordings are kept server-side; align the app's local
   prune (currently prune-on-success) and document it.
4. **Access control** — ensure recordings are only accessible to authorized CRM roles (PII/voice data).
5. **Throughput** — confirm the API can handle burst uploads when many agents reconnect after being
   offline (the app batches + backs off, but the server must absorb it).

Exit: load-tested upload path; storage + retention configured.

---

## Phase 3 — Build, signing & versioning

Goal: reproducible, signed release artifacts.

Steps:
1. Create an upload/release **keystore** (`keytool -genkeypair -v -keystore release.jks -keyalg RSA
   -keysize 4096 -validity 10000 -alias clatos`). Store it securely (secret manager / CI secret).
2. Provide signing inputs without committing secrets — create `keystore.properties` at the repo root:
   ```
   storeFile=/abs/path/release.jks
   storePassword=…
   keyAlias=clatos
   keyPassword=…
   ```
   (Already wired in `app/build.gradle.kts`; `keystore.properties` is gitignored.)
3. Set the production `CRM_BASE_URL` (consider a `productionRelease` flavor/variant if you have
   multiple environments).
4. Bump `versionCode`/`versionName` per release in `app/build.gradle.kts`.
5. Build: `./gradlew assembleRelease` → signed APK at `app/build/outputs/apk/release/`.
6. (CI) Add the keystore as encrypted CI secrets and a release job if you want CI-built signed APKs.

Exit: a signed release APK installs and runs.

---

## Phase 4 — Device provisioning & MDM (if chosen)

Goal: devices set up so the app is the default dialer with all permissions, ideally Device Owner.

Steps (MDM / Android Enterprise path — recommended):
1. Choose an EMM/MDM (e.g., Android Enterprise via an EMM provider).
2. Enroll devices as **fully managed (Device Owner)** — typically via QR/zero-touch on a factory-reset
   device.
3. Push the signed APK as a **managed app**.
4. Use **managed configuration / app permissions policy** to auto-grant runtime permissions
   (phone, mic, contacts, notifications) and set Clatos as the **default dialer role**.
5. Validate the app reports the `PRIVILEGED` (or best available) recording strategy in Settings and
   in CRM telemetry.

Steps (no-MDM / sideload path):
1. Distribute the APK (MDM-less): secure download or manual install; enable "install unknown apps".
2. On first run, the in-app onboarding requests permissions + the default-dialer role (already built).
3. Expect MIC/degraded recording on many devices; track results in `docs/DEVICE_MATRIX.md`.

Exit: a provisioned reference device shows correct permissions, default-dialer status, and recording
capability.

---

## Phase 5 — QA & acceptance (on-device)

Goal: validate behavior on real hardware against the acceptance criteria in `docs/USER_STORIES.md`.

Steps:
1. Build the **device test matrix** from your fleet's OEM/OS spread (`docs/DEVICE_MATRIX.md`).
2. For each representative device, run the smoke flow:
   - Login → onboarding (grant all, set default dialer)
   - Outgoing call: connects, in-call UI, **recording produced** (`.m4a`), call-log row created
   - Incoming call: full-screen/lock-screen UI, Answer/Decline (incl. from notification)
   - Missed call captured
   - Recording uploaded to CRM (multipart) and is **audible**; remote party present? (record result)
   - Offline: place calls with no network → queue → auto-upload on reconnect; "Sync now"
   - Contacts: device+CRM merged, search, open CRM profile, create → appears in CRM, no delete
   - Logout clears session; re-login resumes pending sync
3. Verify backgrounded/screen-off recording survives (foreground service).
4. Capture per-device recording fidelity in the matrix; flag/replace devices that can't record.

Exit: matrix filled; all Must-have acceptance criteria pass on the target devices.

---

## Phase 6 — Security & privacy review

Steps:
1. Confirm token + session encrypted at rest (Keystore) and recordings in app-private storage.
2. Confirm TLS-only (network security config); pin certs if required by policy.
3. Run `/security-review` (or a manual review) on the diff; address findings.
4. Data-handling review: recordings are PII/voice — confirm consent capture, server access control,
   and retention comply with the Phase 0 legal decision.
5. Remove debug logging of sensitive data in release (OkHttp logging is already debug-only).

Exit: sign-off from security/privacy owner.

---

## Phase 7 — Observability & support

Steps:
1. Confirm device/recording **telemetry** reaches the CRM (`POST /api/devices/report`) and build an
   admin view to spot devices with degraded/failed recording.
2. Add crash/error reporting if desired (e.g., a privacy-reviewed crash reporter).
3. Define support runbook: how an agent re-runs the recording self-test (Settings), forces "Sync now",
   re-grants permissions, and re-sets the default dialer.

Exit: admins can see fleet health; agents have a support path.

---

## Phase 8 — Rollout

Steps:
1. **Pilot** with a small agent group on the chosen provisioning path; monitor telemetry + CRM data.
2. Fix issues; update the device matrix.
3. **Staged rollout** to the full fleet via MDM (or controlled APK distribution).
4. Establish an update cadence (versioning + MDM managed-app updates).

Exit: full fleet live; recordings + logs flowing reliably to the CRM.

---

## Go-live checklist (quick reference)

- [ ] Recording strategy decided (MDM/Device-Owner vs best-effort)
- [ ] Legal consent policy + disclosure approved
- [ ] Real PHPMaker endpoints mapped; app wired; staging verified
- [ ] CRM accepts multipart recording upload, dedupes on `client_call_id`
- [ ] Server storage, upload limits, retention, access control configured
- [ ] Release keystore created; signed `assembleRelease` builds
- [ ] Production `CRM_BASE_URL` set; version bumped
- [ ] Devices provisioned (default dialer + permissions; Device Owner if MDM)
- [ ] Device test matrix passed; recording fidelity acceptable per device
- [ ] Security/privacy review signed off
- [ ] Telemetry/admin visibility + support runbook in place
- [ ] Pilot complete → staged rollout

---

## Engineering reference (where things live)

| Concern | File(s) |
|---|---|
| CRM endpoints / DTOs / auth | `core/network/CrmApi.kt`, `core/network/dto/CrmDtos.kt`, `core/network/AuthInterceptor.kt` |
| Base URL / signing / versions | `app/build.gradle.kts` |
| Recording engine + self-test | `recording/` (`RecordingEngine`, `MediaRecorderEngine`, `RecordingSelfTest`, `CallRecorder`, `RecordingService`) |
| Call handling / in-call UI | `telephony/` (`ClatosInCallService`, `CallManager`, `CallNotifications`), `feature/incall/` |
| Offline sync | `sync/` (`CallSyncWorker`, `CallLogRepository`) |
| Contacts | `core/contacts/DeviceContactsDataSource.kt`, `sync/ContactRepository.kt`, `feature/contacts/` |
| Permissions/onboarding | `core/common/PermissionUtils.kt`, `feature/onboarding/` |
| Telemetry | `sync/DiagnosticsReporter.kt` |
