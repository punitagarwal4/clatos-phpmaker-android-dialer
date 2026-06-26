# Clatos Dialer — Agile User Stories & Product Backlog

> Android call-recording dialer for a PHPMaker CRM. Field agents install the app, set it as the
> device's default phone app, log in with CRM credentials, and the app pushes every call log +
> recording to the CRM. Contacts are a unified view of device + CRM contacts.

**Legend**
- **Priority (MoSCoW):** Must / Should / Could / Won't (this release)
- **Estimate:** story points (Fibonacci)
- **AC:** acceptance criteria (Given / When / Then)
- **Roles:** *Agent* (field user of the app), *Admin* (CRM operator), *System* (the app itself)

---

## Product Goals & Non-Goals

**Goals**
- A fully functional default dialer (place/receive calls) for field agents.
- Automatic recording of incoming & outgoing calls; capture of missed-call metadata.
- Reliable, offline-first delivery of call logs + recordings to the CRM.
- Login gated by the CRM API.
- Unified contacts (device + CRM); create & open (CRM profile); **no deletion**.

**Non-Goals (this release)**
- VoIP/SIP calling (uses native cellular telephony).
- Multi-tenant admin console inside the app (admin lives in the CRM).
- Editing CRM schema from the app.

---

## Personas

- **Field Agent** — uses an Android phone provisioned by the company. Makes many calls daily.
  Wants minimal friction: log in once, calls just work, recordings handled automatically.
- **CRM Admin** — manages agents and reviews call activity/recordings in the CRM. Needs every
  call reliably attributed to the right agent with its recording.

---

## EPIC 1 — Authentication & Session

**Goal:** the app is inert until an agent authenticates against the CRM; sessions persist securely.

### US-1.1 — Login with CRM credentials  *(Must, 5)*
**As an** Agent, **I want** to log in with my CRM username and password, **so that** the app
becomes functional and my calls are attributed to me.
- **AC1** Given valid credentials, When I submit login, Then the app calls `POST /api/login`,
  stores the returned token encrypted, and routes me to onboarding/home.
- **AC2** Given invalid credentials, When I submit, Then I see a clear inline error and no token is stored.
- **AC3** Given no network, When I submit, Then I see "No connection — login requires internet."
- **AC4** Given a server/5xx error, When I submit, Then I see a retryable error message.

### US-1.2 — Persistent session  *(Must, 3)*
**As an** Agent, **I want** to stay logged in across app restarts, **so that** I don't re-enter
credentials daily.
- **AC1** Given a valid stored token, When I reopen the app, Then I skip the login screen.
- **AC2** Given a stored token, When the app starts, Then it validates via `GET /api/me` in the background.

### US-1.3 — Token expiry & gating  *(Must, 3)*
**As the** System, **I want** to handle expired/invalid tokens, **so that** the app never operates unauthenticated.
- **AC1** Given an expired token, When any API returns 401, Then the app attempts refresh (if supported) or forces re-login.
- **AC2** Given no valid session, When the app is open, Then dialer/contacts/sync features are gated behind login.
- **AC3** Pending (un-synced) call logs are preserved across re-login.

### US-1.4 — Logout  *(Should, 2)*
**As an** Agent, **I want** to log out, **so that** my data is cleared on a shared device.
- **AC1** Given I confirm logout, When it completes, Then token + cached CRM data are wiped and sync stops.
- **AC2** Given there are unsynced logs, When I log out, Then I'm warned and given a chance to sync first.

---

## EPIC 2 — Default Dialer & Calling

**Goal:** the app behaves as a complete phone app.

### US-2.1 — Become the default phone app  *(Must, 5)*
**As an** Agent, **I want** to set this app as my default dialer, **so that** all calls route through it and can be recorded.
- **AC1** Given the app isn't the default dialer, When onboarding runs, Then it requests `ROLE_DIALER` via `RoleManager`.
- **AC2** Given I grant the role, When I return, Then the app confirms it's the default phone app.
- **AC3** Given I decline, When I continue, Then the app explains recording won't work and lets me retry.

### US-2.2 — Outgoing call via dialpad  *(Must, 5)*
**As an** Agent, **I want** to dial a number and place a call, **so that** I can reach contacts.
- **AC1** Given a number on the dialpad, When I tap call, Then an outgoing call is placed via `TelecomManager`.
- **AC2** Given an active call, When connected, Then I see the in-call screen with duration.

### US-2.3 — Incoming call handling & in-call UI  *(Must, 8)*
**As an** Agent, **I want** to answer/reject incoming calls with full controls, **so that** I can manage calls.
- **AC1** Given an incoming call, When the phone rings, Then I see an answer/reject screen with caller ID.
- **AC2** Given an active call, Then I can mute, toggle speaker, open the dialpad (DTMF), and hang up.
- **AC3** In-call state is driven by `InCallService`.

### US-2.4 — Caller ID from unified contacts  *(Should, 3)*
**As an** Agent, **I want** incoming/outgoing numbers resolved to names, **so that** I know who I'm talking to.
- **AC1** Given a known number (device or CRM), When a call occurs, Then the matching name/photo is shown.
- **AC2** Given an unknown number, Then the raw number is shown.

### US-2.5 — Tap-to-call  *(Must, 3)*
**As an** Agent, **I want** to call from contacts and history, **so that** I avoid retyping numbers.
- **AC1** From a contact or history entry, When I tap call, Then the outgoing-call flow starts.

---

## EPIC 3 — Permissions & Onboarding

**Goal:** acquire every runtime permission with clear rationale; degrade gracefully if revoked.

### US-3.1 — Guided permission onboarding  *(Must, 5)*
**As an** Agent, **I want** a guided setup that requests all permissions, **so that** the app works fully on first use.
- **AC1** Given first launch (post-login), When onboarding runs, Then it requests, with rationale:
  phone/call, read call log, read/write contacts, microphone, notifications, read media audio.
- **AC2** Given any permission denied, Then the screen explains the impact and offers re-request / open settings.
- **AC3** Onboarding also drives the default-dialer role (US-2.1).

### US-3.2 — Runtime permission resilience  *(Must, 3)*
**As the** System, **I want** to detect revoked permissions, **so that** features fail safely.
- **AC1** Given a required permission is revoked, When the related feature is used, Then a clear prompt to restore it is shown.
- **AC2** Given microphone is revoked, Then recording is disabled and the agent is warned, but calls still work.

### US-3.3 — Managed/Device-Owner provisioning  *(Could, 5)*
**As an** Admin, **I want** managed devices to auto-grant permissions and the dialer role, **so that** agents need zero setup.
- **AC1** Given an MDM-enrolled (Device Owner) device, When provisioned, Then permissions + dialer role are pre-granted.
- **AC2** Provisioning steps are documented for the chosen MDM/Android Enterprise flow.

---

## EPIC 4 — Call Recording  *(highest technical risk)*

**Goal:** record both parties where the device allows; always degrade gracefully and report capability.

### US-4.1 — Recording self-test & strategy selection  *(Must, 8)*
**As the** System, **I want** to test recording capability on first run, **so that** I pick the best working method per device.
- **AC1** Given first run (and after OS updates), When the self-test runs, Then it tries strategies in priority order
  (privileged/Device-Owner → `VOICE_CALL`/`VOICE_RECOGNITION` → accessibility-assisted → `MIC`).
- **AC2** Given a strategy produces audio with sufficient energy, Then it's selected and persisted.
- **AC3** Given no strategy captures the remote party, Then capability is marked degraded and reported to the CRM.

### US-4.2 — Record outgoing calls  *(Must, 8)*
**As an** Agent, **I want** outgoing calls recorded automatically, **so that** every call is captured without action.
- **AC1** Given an outgoing call connects, When audio starts, Then recording begins using the selected strategy.
- **AC2** Given the call ends, Then recording stops and the file is finalized and linked to the call log.

### US-4.3 — Record incoming calls  *(Must, 8)*
**As an** Agent, **I want** incoming calls recorded automatically, **so that** inbound conversations are captured.
- **AC1** Given an incoming call is answered, Then recording begins automatically.
- **AC2** Given the call ends, Then recording stops, finalizes, and links to the log.

### US-4.4 — Recording survives backgrounding  *(Must, 5)*
**As the** System, **I want** recording to continue when the screen is off or the app is backgrounded.
- **AC1** Given an active recording, When the app is backgrounded or screen off, Then a foreground service keeps recording.
- **AC2** A persistent notification indicates an active recording (compliance/UX).

### US-4.5 — Recording storage & linkage  *(Must, 3)*
**As the** System, **I want** recordings stored efficiently and linked, **so that** uploads attach the right audio.
- **AC1** Recordings are saved as `.m4a` (AAC) in app-private storage.
- **AC2** Each recording is referenced by its `CallLog` row id.

### US-4.6 — Recording failure handling  *(Must, 5)*
**As an** Agent, **I want** calls to work even if recording fails, **so that** I'm never blocked.
- **AC1** Given recording can't start/fails, When I'm on a call, Then the call proceeds normally.
- **AC2** Given a recording failure, Then the call log is still created and flagged `recording: failed`, and reported to the CRM.

---

## EPIC 5 — Call Log Capture & CRM Sync

**Goal:** offline-first capture and reliable delivery of all call activity to the CRM.

### US-5.1 — Capture every call (incl. missed)  *(Must, 5)*
**As the** System, **I want** to record metadata for every call, **so that** the CRM has a complete log.
- **AC1** Given a call ends (incoming/outgoing), Then a `CallLog` row is created with direction, number,
  resolved name, start time, duration, and status.
- **AC2** Given an unanswered incoming call, Then a `MISSED` entry is created.
- **AC3** Each row starts `syncStatus = PENDING`.

### US-5.2 — Background upload with retry  *(Must, 8)*
**As the** System, **I want** to upload pending logs + recordings in the background, **so that** delivery is reliable.
- **AC1** Given pending logs and connectivity, When `CallSyncWorker` runs, Then it uploads each as multipart
  (metadata JSON + audio) to `POST /api/calllogs`.
- **AC2** Given a successful upload, Then the row is marked `SYNCED` with the CRM remote id.
- **AC3** Given a failure, Then it retries with exponential backoff; `attempts` increments.

### US-5.3 — Offline queue  *(Must, 5)*
**As an** Agent, **I want** calls made offline to upload later automatically, **so that** I don't lose data in the field.
- **AC1** Given no connectivity, When calls occur, Then logs + recordings queue locally.
- **AC2** Given connectivity returns, Then queued items upload automatically (WorkManager network constraint).

### US-5.4 — Sync visibility & manual sync  *(Should, 3)*
**As an** Agent, **I want** to see sync status and trigger a sync, **so that** I can confirm delivery.
- **AC1** Each history entry shows pending/synced/failed.
- **AC2** A "Sync now" action forces an immediate sync attempt.

### US-5.5 — Local retention/pruning  *(Should, 3)*
**As the** System, **I want** to prune uploaded recordings, **so that** device storage stays healthy.
- **AC1** Given a recording uploads successfully, Then the local file is pruned per the retention policy.
- **AC2** The retention window is configurable (default to confirm with user).

---

## EPIC 6 — Contacts: Unified List

### US-6.1 — Unified contacts list  *(Must, 8)*
**As an** Agent, **I want** device + CRM contacts in one list, **so that** I have one place to find people.
- **AC1** Given device and CRM contacts, When I open contacts, Then a merged, source-tagged, deduped list is shown.
- **AC2** Duplicates (same normalized number) are merged with both sources indicated.

### US-6.2 — Search & filter  *(Should, 3)*
**As an** Agent, **I want** to search the unified list, **so that** I find contacts fast.
- **AC1** Given a query, When I type, Then results filter by name/number across both sources.

### US-6.3 — Incremental CRM sync & offline cache  *(Must, 5)*
**As the** System, **I want** to sync CRM contacts incrementally and cache them, **so that** contacts load fast and offline.
- **AC1** Given a prior sync, When syncing, Then only changes since last sync are fetched (`?since=`).
- **AC2** Given no network, Then cached CRM contacts are shown.

---

## EPIC 7 — Contact Profile (CRM-backed)

### US-7.1 — Open contact = CRM profile  *(Must, 5)*
**As an** Agent, **I want** opening a contact to show its CRM profile, **so that** I see rich CRM data.
- **AC1** Given I open a CRM-linked contact, When the profile loads, Then data is fetched live from `GET /api/contacts/{id}`.
- **AC2** Given a device-only contact, Then a basic profile (device fields) is shown with an option to link/create in CRM.

### US-7.2 — Recent call history on profile  *(Should, 3)*
**As an** Agent, **I want** recent calls with this contact on the profile, **so that** I have context.
- **AC1** Given a contact, When I view the profile, Then recent local call history with that number is listed.

### US-7.3 — Offline profile  *(Could, 3)*
**As an** Agent, **I want** a cached profile when offline, **so that** I still get info.
- **AC1** Given no network, Then the last cached profile is shown with a "may be out of date" indicator.

---

## EPIC 8 — Contact Creation & No-Delete Policy

### US-8.1 — Create contact (push to CRM)  *(Must, 5)*
**As an** Agent, **I want** to create a contact, **so that** new people are captured in the CRM.
- **AC1** Given I fill the create form, When I save, Then the contact is sent to `POST /api/contacts` and appears in the list.
- **AC2** Given the create fails (offline), Then it queues and retries (or shows an error per chosen policy — to confirm).

### US-8.2 — No deletion anywhere  *(Must, 2)*
**As an** Admin, **I want** deletion disabled, **so that** records are never lost from the device app.
- **AC1** Given any contact (device or CRM), When viewing/editing, Then no delete action exists in the UI.

### US-8.3 — Write to device address book?  *(Could, 3 — decision needed)*
**As an** Agent, **I want** (optionally) created contacts saved to the phone too, **so that** they appear in other apps.
- **AC1** Decision pending: CRM-only vs CRM + device `ContactsContract` write.

---

## EPIC 9 — Security, Settings & Diagnostics

### US-9.1 — Encrypted storage  *(Must, 5)*
**As the** System, **I want** sensitive data encrypted, **so that** tokens/recordings are protected at rest.
- **AC1** Token/session stored via Keystore-backed encryption.
- **AC2** Recordings live in app-private storage (not world-readable).

### US-9.2 — Settings screen  *(Should, 3)*
**As an** Agent, **I want** a settings screen, **so that** I can see account, sync status, storage, and recording capability.

### US-9.3 — Diagnostics + telemetry  *(Could, 3)*
**As an** Admin, **I want** device/recording telemetry reported, **so that** I can spot incompatible handsets.
- **AC1** Device model, OS version, selected recording strategy, and last sync are reported to `POST /api/devices/report`.

---

## EPIC 10 — Build, Distribution & QA

### US-10.1 — Signed APK & versioning  *(Should, 3)*
**As an** Admin, **I want** signed, versioned APKs, **so that** I can distribute via sideload/MDM.

### US-10.2 — Recording compatibility matrix  *(Should, 5)*
**As an** Admin, **I want** a tested device/OEM/OS matrix, **so that** I know which handsets record reliably.

---

## Sprint Plan (proposed)

| Sprint | Theme | Stories |
|---|---|---|
| 0 | Foundations / scaffold | Architecture, modules, DI, networking, Room, CI, API contract; US-10.1 (partial) |
| 1 | Auth + Onboarding + Permissions | EPIC 1, EPIC 3 |
| 2 | Dialer core | EPIC 2 |
| 3 | Recording engine | EPIC 4 |
| 4 | Call log + sync | EPIC 5 |
| 5 | Contacts | EPIC 6, 7, 8 |
| 6 | Security, diagnostics, hardening, device matrix | EPIC 9, 10 |

## Definition of Ready
- Story has clear AC, estimate, priority, and any API contract dependency identified.

## Definition of Done
- Code reviewed & merged; AC met; unit tests for logic; manual QA on ≥1 real device; no new lint/build errors;
  docs updated (API contract / device matrix where relevant).

## Key Risks
- **R1 (High):** remote-party audio capture restricted on modern Android → mitigated by multi-strategy engine,
  Device-Owner path, and self-test/telemetry.
- **R2 (Med):** OEM fragmentation of telephony/recording → device compatibility matrix (US-10.2).
- **R3 (Med):** legal call-recording consent requirements → in-call notification + policy confirmation.
- **R4 (Low):** PHPMaker API shape differs from assumed contract → thin adapter layer in `core/network`.
