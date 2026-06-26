# Clatos Dialer — CRM API Contract (assumed)

> This is the contract the Android app codes against. It is based on a typical **PHPMaker REST API**.
> **Confirm the real endpoint paths, auth scheme, and field names** against your CRM and adjust the
> `core/network` adapter (`CrmApi`, DTOs, mappers) where they differ. PHPMaker can expose a JWT or
> API-key based REST API and supports custom API actions, so any gaps can be closed by adding APIs.

All requests are JSON over HTTPS unless noted. Authenticated requests send:

```
Authorization: Bearer <token>
```

Base URL is configured per environment (e.g., `https://crm.example.com`).

---

## 1. Authentication

### POST /api/login
Authenticate an agent.

Request:
```json
{ "username": "agent01", "password": "••••••" }
```
Response `200`:
```json
{
  "token": "eyJhbGciOi...",      // JWT or API key
  "expires_in": 86400,            // optional
  "user": { "id": 12, "name": "Agent One", "username": "agent01" }
}
```
Errors: `401` invalid credentials, `5xx` server error.

### GET /api/me
Validate token / fetch current user.
Response `200`: `{ "id": 12, "name": "Agent One", "username": "agent01" }`
`401` → token invalid/expired.

> PHPMaker note: the built-in login API path may differ (e.g., `/api/login` with `username`/`password`
> form fields, returning a JWT). Adjust field names as needed.

---

## 2. Contacts

### GET /api/contacts
List CRM contacts (for the unified list + incremental sync).

Query params: `since` (ISO-8601, optional, incremental), `page`, `per_page`.

Response `200`:
```json
{
  "data": [
    { "id": 101, "name": "Acme Buyer", "phone": "+15551230001",
      "phones": ["+15551230001"], "company": "Acme", "updated_at": "2026-06-01T10:00:00Z" }
  ],
  "page": 1, "per_page": 100, "total": 540
}
```

### GET /api/contacts/{id}
Full CRM profile (the contact-profile screen).
Response `200`: full record with all CRM fields the app should display
(name, phones, email, company, address, owner, notes, custom fields…).

### POST /api/contacts
Create an agent-entered contact.
Request:
```json
{ "name": "New Lead", "phone": "+15559876543", "company": "Beta Co", "notes": "met at expo" }
```
Response `201`: `{ "id": 777, "name": "New Lead", "phone": "+15559876543", ... }`

> **No delete endpoint is used by the app** (deletion is disabled by product requirement).

---

## 3. Call Logs + Recordings

### POST /api/calllogs  (multipart/form-data)
Upload one call log with its recording.

Parts:
- `metadata` (application/json):
```json
{
  "agent_id": 12,
  "direction": "OUTGOING",          // INCOMING | OUTGOING | MISSED
  "number": "+15551230001",
  "contact_id": 101,                 // optional, if resolved
  "display_name": "Acme Buyer",      // optional
  "started_at": "2026-06-26T09:15:00Z",
  "duration_sec": 142,
  "status": "OUTGOING",
  "recording_state": "OK",           // OK | FAILED | NONE
  "recording_strategy": "VOICE_CALL",
  "client_call_id": "uuid-local"     // idempotency key
}
```
- `recording` (audio/mp4, `.m4a`): present when `recording_state = OK`; omitted otherwise.

Response `201`:
```json
{ "id": 99001, "client_call_id": "uuid-local", "status": "stored" }
```

Idempotency: server should dedupe on `client_call_id` so retries don't create duplicates.
Errors: `401` (re-auth), `413` (file too large → app may compress/chunk), `5xx` (retry w/ backoff).

> If the PHPMaker table for call logs doesn't accept multipart directly, two options:
> (a) a custom PHPMaker API action that accepts the file + fields, or
> (b) upload the audio to a file endpoint first, then POST the metadata with the returned file id.

---

## 4. Device / Recording Telemetry (optional)

### POST /api/devices/report
Report device + recording capability for admin visibility.
Request:
```json
{
  "agent_id": 12,
  "device_model": "Pixel 8",
  "os_version": "Android 15",
  "selected_strategy": "VOICE_CALL",
  "recording_degraded": false,
  "app_version": "1.0.0",
  "last_sync_at": "2026-06-26T09:20:00Z"
}
```
Response `200`: `{ "status": "ok" }`

---

## Mapping to the App

| App component | Endpoint(s) |
|---|---|
| `AuthRepository` | `POST /api/login`, `GET /api/me` |
| `ContactRepository` | `GET /api/contacts`, `GET /api/contacts/{id}`, `POST /api/contacts` |
| `CallSyncWorker` | `POST /api/calllogs` |
| `DiagnosticsReporter` | `POST /api/devices/report` |

## Confirmations Needed
1. Real paths & HTTP methods (PHPMaker default API vs custom actions).
2. Auth scheme: JWT bearer vs API key header vs session cookie.
3. Exact field names & required fields for contacts and call logs.
4. Multipart support for recordings vs separate file-upload endpoint.
5. Pagination + incremental (`since`) support for contacts.
6. Idempotency handling for `client_call_id`.
