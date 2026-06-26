# Recording Compatibility Matrix (US-10.2)

Both-party call recording is **device- and OS-dependent** on modern Android. Use this matrix to
track which handsets in the fleet reliably capture the remote party. The app self-tests on first run
and reports the selected strategy + a `degraded` flag to the CRM (`POST /api/devices/report`), so this
table can be populated from CRM telemetry.

## Strategy legend
- **PRIVILEGED** — Device Owner / platform-signed (MDM). Best fidelity; both parties.
- **VOICE_CALL** — `MediaRecorder` `VOICE_CALL` source. Works on some OEMs/older OS.
- **VOICE_RECOGNITION** — alternative source usable on some devices.
- **ACCESSIBILITY** — accessibility-assisted capture where the OEM permits.
- **MIC** — local side reliable; remote side speaker/OEM dependent (degraded).
- **NONE** — no viable strategy; recording disabled (call + metadata still captured).

## How to fill this in
1. Sideload the debug APK on the target device.
2. Complete onboarding (grant mic + set default dialer).
3. Place a test call to a phone you can hear; end it.
4. In **Settings**, read the selected strategy; in the CRM, confirm the recording uploaded and is audible.
5. Record the result below.

| OEM | Model | Android / API | Selected strategy | Remote party audible? | Notes |
|-----|-------|---------------|-------------------|-----------------------|-------|
| _e.g. Samsung_ | _Galaxy A14_ | _14 / 34_ | _MIC_ | _partial_ | _speaker only_ |
|     |       |               |                   |                       |       |

## Recommendation
For guaranteed both-party recording across the fleet, enroll devices via **MDM / Android Enterprise**
as **Device Owner** so the app can use the PRIVILEGED path. Without MDM, expect MIC/degraded results
on many current handsets — the app degrades gracefully and flags those devices to the CRM.
