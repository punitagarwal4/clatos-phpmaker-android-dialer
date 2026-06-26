# Project Status — What's Complete and What's Left

This tracks the gap between the implemented app and a production-ready release, per the backlog in
`docs/USER_STORIES.md`.

## ✅ Implemented (all 6 sprints)

- **Auth & session:** CRM login, token validation (`GET /api/me`), 401 → re-login, logout, encrypted
  session storage, state-driven gating.
- **Onboarding & permissions:** guided runtime-permission flow with rationale + re-prompt, default-dialer
  role request, resilience checks.
- **Dialer & calls:** dialpad, outgoing calls, incoming answer/reject, in-call UI (mute/speaker/DTMF/
  hang up), lock-screen full-screen incoming UI, caller ID, tap-to-call everywhere.
- **Recording:** pluggable engine + first-run self-test (MediaRecorder VOICE_CALL/VOICE_RECOGNITION/MIC),
  foreground-service capture, capability persisted + reported to CRM, in-call indicator, manual re-test.
- **Call-log sync:** offline Room queue, multipart upload worker, exponential backoff, periodic +
  manual sync, retention pruning.
- **Contacts:** unified device + CRM list (deduped, searchable), live CRM profile + recent calls,
  create → CRM, no delete; incremental CRM sync.
- **Security/diag:** TLS-only network config, device compatibility matrix, telemetry, CI build.

## ✅ Completed in this pass (previously stubbed/TODO)

- Incoming-call notification **Answer/Decline action buttons** (via `CallActionReceiver`).
- **Contact create** now surfaces saving/error state instead of failing silently.
- **Incremental contact sync** persists and sends a `since` timestamp, paging through all results.
- **Unit tests** for phone-number normalization + CI now runs `testDebugUnitTest`.
- **Runtime-correctness fixes** from an audit: recording finalized before the mic FGS is torn
  down; failed uploads retry (instead of being stuck `FAILED`); sync uses `KEEP` (no stacking);
  mic FGS not held when nothing records; mic-permission crash guard; caller-ID in notification.
- **Self-contained build**: committed the Gradle wrapper (`gradlew` + `gradle-wrapper.jar`).
- **Release signing wired**: optional `keystore.properties` at the repo root enables a signed
  release build (nothing secret committed; debug builds unaffected).

## ⛔ Requires external input / hardware — cannot be finished in-repo

These are genuinely blocked on decisions or resources only the project owner can provide:

1. **Both-party recording on modern Android (PRIVILEGED strategy).** Needs **MDM / Android Enterprise
   Device-Owner** enrollment (or platform signing). The engine has the hook and degrades gracefully;
   the privileged capture itself is enabled by provisioning, not app code. → Provide MDM details.
2. **Accessibility-assisted recording (ACCESSIBILITY strategy).** Device/OEM-specific and disallowed on
   Play Store; left as a documented enhancement rather than dead code. → Decide per target devices.
3. **Real PHPMaker API mapping.** `core/network` codes against `docs/API_CONTRACT.md` (assumed paths/
   fields/auth). → Provide real endpoints / Swagger / Postman to finalize DTOs + paths.
4. **Release keystore.** The signing config is wired; to produce a signed release APK, drop a
   `keystore.properties` (storeFile/storePassword/keyAlias/keyPassword) at the repo root or supply
   it via CI secrets. → Provide/sanction a signing keystore.
5. **Real-device QA.** Acceptance criteria in `USER_STORIES.md` need on-device validation across the
   `DEVICE_MATRIX.md` handsets (recording fidelity, lock-screen incoming UI, default-dialer behavior).
   This is inherently a hardware step and cannot be done in CI.

## Decisions still open (from the original plan)
- Recording retention window after successful upload (currently prune-on-success).
- Whether agent-created contacts also write to the device address book (currently CRM-only).
- Call-recording consent/disclosure per jurisdiction (in-call notification is in place as the hook).
