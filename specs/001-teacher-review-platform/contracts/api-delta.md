# Phase 1 Contracts: REST API — Existing Surface + Delta

The full existing REST contract (all currently-working endpoints, request/response shapes) is
already documented in `unireview-backend/README.md` §7 and `project_specification.md` §6 — this
file does not repeat that; it exists to make the **changes** required by this plan explicit and
reviewable, since those are what `tasks.md` will implement.

## Endpoints that stay contractually identical

`GET /api/teachers`, `GET /api/teachers/{id}`, `GET /api/teachers/{id}/reviews`,
`POST /api/reviews`, `GET /api/gate/status`, `POST /api/admin/login`,
`GET /api/admin/reviews/flagged`, `PUT /api/admin/reviews/{id}/approve`,
`PUT /api/admin/reviews/{id}/reject` — no request/response shape changes. (Their *behavior* may
change per `research.md` — e.g., `POST /api/reviews` now rate-limits and hashes IPs — but callers
observe no contract difference: same request shape, same success/error shape, just a new
possible error case for rate limiting, see below.)

## Changed contract: `POST /api/reviews/{id}/vote`

- **No shape change.** Behavior fix only: a request with no resolvable reviewer identity (no
  `X-Reviewer-Token` header and no `reviewer_token` cookie) now returns the existing
  `403 NO_REVIEWER_TOKEN` `ErrorResponse` shape (already used by other endpoints) instead of an
  uncaught `500`.

## Changed contract: `POST /api/reviews/{id}/report`

**Before** (current code):
```
Headers: (none required)
Body:    { "reason": "string", "description": "string" }
```

**After**:
```
Headers: X-Reviewer-Token: <uuid>   (or reviewer_token cookie — same fallback pattern as vote/submit)
Body:    { "reason": "string", "description": "string" }   (unchanged)
```
- New failure mode: `403 NO_REVIEWER_TOKEN` if no identity is resolvable — same shape as the
  existing error on `GET /api/teachers/{id}/reviews`.
- New failure mode: `403 CAPTCHA_FAILED` (existing `CaptchaFailedException`/error code, currently
  only returned by submit) if captcha verification fails — same shape, new call site only, since
  captcha stays disabled by default per the stakeholder's "keep mock for now" decision, this
  failure mode won't trigger in the current dev config, but the code path must exist.

## New endpoint: `PUT /api/admin/reviews/{id}/hide`

Mirrors the existing `approve`/`reject` admin endpoints exactly in shape and auth requirements.

```
Auth:     Admin JWT (Authorization: Bearer <token>) — same as approve/reject
Path:     PUT /api/admin/reviews/{id}/hide
Body:     (none)
Response: 200 OK, empty body — same as approve/reject
Errors:   404 (review not found) — same ResourceNotFoundException shape already used elsewhere
```
Side effects (server-side, not part of the wire contract but required behavior per FR-022):
review's `status` → `HIDDEN`; if the author's `credit_balance > 0`, decrement by 1; recompute
the affected teacher's `avg_rating`/`total_reviews`.

## Changed contract: `POST /api/admin/teachers/import-csv`

**Before** (current code):
```json
{ "message": "Đã import thành công N giảng viên", "importedCount": N }
```

**After** (adds failure reporting + reflects upsert, per FR-023/clarification):
```json
{
  "message": "Đã import thành công N giảng viên",
  "importedCount": N,
  "updatedCount": M,
  "failedRows": [
    { "row": 4, "reason": "Thiếu họ tên (full_name)" },
    { "row": 9, "reason": "Thiếu khoa (faculty)" }
  ]
}
```
- `importedCount` now means net-new rows only; `updatedCount` is new, counting rows matched to
  an existing teacher and updated in place (Decision 5 in `research.md`).
- `failedRows` is new — previously-silent skips are now reported with a 1-based row number and a
  human-readable reason, satisfying FR-023's "MUST report which records failed and why."
- No change to the request shape (`multipart/form-data`, field name `file`).

## New (implicit) contract: per-IP rate limit on `POST /api/reviews`

No request shape change. New possible failure response, following the same `ErrorResponse` JSON
shape as every other business-rule rejection in the codebase (`project_specification.md` §6):
```json
{
  "status": 429,
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Bạn đã gửi quá 3 review trong hôm nay, vui lòng thử lại vào ngày mai.",
  "timestamp": "2026-08-14T11:10:17Z"
}
```
(429 chosen over 400/403 since this is specifically a rate-limit condition; consistent with the
`ErrorResponse` shape's `status` field already being a plain int passthrough of the HTTP status,
not restricted to the 3 codes currently in use.)
