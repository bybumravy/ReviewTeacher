# Phase 0 Research: Anonymous Teacher Review Platform

All items below are gap-remediation decisions for an **existing** codebase, not greenfield
technology choices — the stack itself (Spring Boot, PostgreSQL/Flyway, React/Vite) is fixed by
the constitution and is not reconsidered here. Findings are from a full read-through audit of
the backend and frontend performed this session (file:line citations preserved for traceability
into `tasks.md`).

---

### 1. IP hashing (Constitution Principle I, NON-NEGOTIABLE — currently violated)

- **Decision**: Hash the client IP with SHA-256 (`java.security.MessageDigest`) at the single
  point of capture — `ReviewController.getClientIp(...)` — and pass only the hashed value
  downstream. Apply the same treatment anywhere else a raw IP is read for anti-abuse purposes
  (currently only this one call site feeds both `ReviewService` and `GateService`).
- **Rationale**: SHA-256 is already named as the intended algorithm in `project_specification.md`
  §3.1/§7.3 and in `ReviewController`'s existing column names (`ip_hash`) — the columns and
  intent already exist, only the hashing step is missing. No new dependency needed (JDK
  built-in). One-way hash satisfies FR-019/Constitution Principle I without adding reversibility.
- **Alternatives considered**: A dedicated hashing library (e.g., Guava `Hashing`) — rejected,
  adds a dependency for something the JDK does natively in ~5 lines. Salting the hash — rejected
  for v1: a per-request salt would break the rate-limiting use case (Decision 2 below), which
  needs the *same* IP to hash to the *same* value within a day; a fixed, unsalted SHA-256 is the
  documented design in the source spec and is consistent with "not reversible," even though it
  is technically subject to a rainbow-table attack on IPv4 space — acceptable for this feature's
  threat model (deterring casual spam, not defeating a targeted deanonymization attempt), and
  changing it later is a non-breaking, isolated change.
- **Note**: existing rows already in `reviewers.ip_hash`/`reviews.ip_hash` (if any, from local
  dev/testing) remain plaintext. Out of scope: this is pre-launch dev data, not production user
  data: no migration/backfill task is warranted.

### 2. Per-IP rate limiting (FR-017 — currently missing)

- **Decision**: Implement as a repository query, not a new library or in-memory counter —
  `ReviewRepository.countByIpHashAndCreatedAtAfter(hashedIp, cutoffTimestamp)` — called from
  `ReviewService.submitReview` before the content-filter step, comparing against the fixed
  business rule of 3/day (from `project_specification.md` §7.2, carried into spec.md's
  Assumptions). Reject with a 429-style/`4xx` outcome analogous to the existing custom
  exceptions (new `RateLimitExceededException` mapped in `GlobalExceptionHandler`) if the count
  is already ≥ 3.
- **Rationale**: The `reviews` table already stores `ip_hash` and `created_at` per row — no
  schema change needed, no new dependency (e.g., Bucket4j) needed for a simple daily cap at this
  scale. Reuses the same hashed value from Decision 1, so this only works correctly once IP
  hashing is deterministic (confirms Decision 1 must land first / together).
- **Alternatives considered**: Bucket4j or a Redis-backed limiter — rejected as
  over-engineering for a 3/day/IP cap with no current Redis dependency in the stack; would add
  infrastructure for no measurable benefit at this scale. A Spring `Filter`/interceptor applied
  globally — rejected because the limit is specifically scoped to review *submission*, not all
  writes; a targeted service-layer check is simpler and keeps the rule visible in
  `ReviewService` next to the other submission-time checks (duplicate-review, captcha).

### 3. Admin auth filter wiring (currently broken — blocks all of US5/US6)

- **Decision**: Register `AdminAuthFilter` in `SecurityConfig.filterChain` via
  `http.addFilterBefore(adminAuthFilter, UsernamePasswordAuthenticationFilter.class)` (standard
  Spring Security pattern for a custom pre-auth filter reading a bearer JWT).
- **Rationale**: The filter, `JwtTokenProvider`, and the `hasRole("ADMIN")` matcher all already
  exist and are individually correct per the audit — this is a one-line wiring omission, not a
  design gap. Confirmed no other filter registration exists anywhere in `SecurityConfig`.
- **Alternatives considered**: None — this is a defect fix with one obvious correct fix, not a
  design decision with tradeoffs.

### 4. Admin seed password hash (looks malformed — blocks admin login even after fix #3)

- **Decision**: Generate a real BCrypt hash for the documented seed password (`admin123`) using
  Spring Security's `BCryptPasswordEncoder` (already a transitive dependency via
  `spring-boot-starter-security`) and ship it via the new `V2` migration (see `data-model.md`)
  as an `UPDATE` statement against the existing seeded row, rather than editing the
  already-applied `V1` migration file.
- **Rationale**: Editing a Flyway migration that may have already run against a local dev
  database breaks Flyway's checksum validation; an additive `V2` migration is the safe, standard
  fix. `admin123` is already the documented value in `unireview-backend/README.md`; regenerating
  its hash is a bug fix, not a new credential design.

### 5. CSV roster upsert-on-match (FR-023, per clarification — currently always-insert)

- **Decision**: Match an incoming CSV row to an existing teacher by the combination of
  `(full_name, faculty)` (case-insensitive), since neither the CSV format nor the `teachers`
  table currently defines any other stable external identifier (no `teacher_code` column
  exists). If a match is found, update the mutable fields (`title`, `department`,
  `avatar_url`) on the existing row; if not, insert a new row. Rating/review-derived fields
  (`avg_rating`, `total_reviews`) are never touched by import, matching the clarified
  requirement to preserve existing ratings/reviews on update.
- **Rationale**: `(full_name, faculty)` is the closest thing to a natural key already implied by
  the existing schema and CSV format; no spec source defines a teacher ID/code field for import
  matching, and adding one would be scope creep beyond the confirmed gap. This is a reasonable,
  low-risk default (Section "For AI Generation" guidance in the spec template) — not something
  requiring another round of user clarification, since the alternative (adding a new import-key
  column) has no signal from any source document.
- **Alternatives considered**: Match by `id` in the CSV — rejected, the CSV format per
  `project_specification.md` doesn't include a stable pre-existing `id` for new imports to
  reference. Fuzzy/normalized name matching (trimming titles, diacritics-insensitive) — noted
  as a reasonable future refinement but out of scope; exact case-insensitive match on the two
  fields is sufficient for the "re-import the same roster" scenario the clarification was about.
- **Also required**: `CsvImportService` must collect and return failed/skipped rows (row number
  + reason) instead of silently dropping them, per FR-023's "MUST report which records failed" —
  this is a direct requirement, not a design choice.

### 6. Admin hide/remove with credit clawback (FR-022 — currently missing entirely)

- **Decision**: Add `PUT /api/admin/reviews/{id}/hide` → `AdminService.hideReview(id)`, which
  sets `ReviewStatus.HIDDEN`, deducts 1 credit from the review's author (`reviewerToken`) if
  their `credit_balance > 0` (reuse the same guarded-decrement pattern already used elsewhere
  for credit spend), and calls the existing, already-correct
  `ReviewService.recalculateTeacherRating(teacherId)` to keep `avg_rating`/`total_reviews`
  consistent — reusing that method rather than duplicating its logic.
- **Rationale**: `ReviewStatus.HIDDEN` already exists as an unused enum value; the recompute
  method already exists and is proven (used by submit-approve and admin-approve paths). This is
  additive: one new endpoint/service method, no changes to existing ones.
- **Alternatives considered**: Soft-delete via a separate `deleted_at` column — rejected, the
  schema already models this state via the `status` enum (`HIDDEN`), consistent with how
  `REJECTED`/`FLAGGED` are already handled; introducing a second mechanism for the same concept
  would be inconsistent with the existing design.

### 7. Report-identity capture (FR-016 — currently anonymous with no verification)

- **Decision**: Add a `reporter_token VARCHAR(50)` column to `review_reports` (nullable=false,
  FK-style reference to `reviewers.token`, no DB-level FK constraint needed since `reviewers`
  rows are created lazily — consistent with how `reviewer_token` is already handled on
  `reviews`), and require `X-Reviewer-Token` on `POST /api/reviews/{id}/report`, mirroring the
  pattern already used for vote.
- **Rationale**: Matches spec.md Key Entities ("Report: a flag raised by a reviewer identity")
  and FR-016/FR-018 (report is a write action requiring bot verification and, implicitly, an
  identity to attribute it to — otherwise nothing stops unlimited anonymous report spam against
  a review).
- **Also required**: `ReviewController.reportReview`/`voteReview` must call
  `captchaService.verify(...)` before proceeding, exactly like `submitReview` already does
  (FR-018 applies to all three write actions, not just submission).

### 8. Vote-without-token NPE → uncaught 500 (bug, not a design gap)

- **Decision**: `ReviewService.voteReview` must validate `voterToken` is present up front and
  throw the existing `NoReviewerTokenException` (already used elsewhere for exactly this case)
  instead of letting a null flow into the DB insert and blow up as a generic 500.
- **Rationale**: Reuses an exception type that already exists and is already mapped to the
  correct 403/`NO_REVIEWER_TOKEN` shape in `GlobalExceptionHandler` — no new exception type
  needed, this is a missing guard clause, not a new pattern.

### 9. `ModerationService` fail-open constant mismatch (bug, not a design gap)

- **Decision**: Change the mock-key check in `ModerationService.evaluateContent` from the
  literal `"mock_key"` to read the same default via `@Value` as `apiKey`'s own default
  (`mock_perspective_key`, matching `application.yml`), so the intended short-circuit actually
  fires and avoids an unnecessary outbound network call on every review submission in
  environments without a real key configured.
- **Rationale**: One-line constant fix; the surrounding fail-open behavior (catch-block fallback
  to `APPROVED`) is already correct and stays as-is, matching FR-009 and the constitution's
  explicit "fail open, do not change to fail closed without an amendment" rule.
- **Also required**: `RestTemplateConfig` should set explicit connect/read timeouts (e.g., 3s
  connect / 5s read) on the `RestTemplate` bean so that even when a real key *is* configured
  later, a slow/unreachable Perspective API can't stall a review submission — directly supports
  spec.md SC-002 ("published within a few seconds").

### 10. Frontend test tooling (currently none)

- **Decision**: Add Vitest + `@testing-library/react` + `@testing-library/jest-dom` as dev
  dependencies (Vite's own recommended pairing — zero extra config beyond a `vitest.config`
  referencing the existing `vite.config.js`), scoped to testing the rewired hooks
  (`useTeachers`, `useGate`, `useReviews`) and the gate-unlock UI flow, matching the backend's
  test-scope decision (core logic, not exhaustive coverage).
- **Rationale**: Vitest is the de facto standard for Vite projects (shares config/transform
  pipeline, no separate bundler needed) and is what `frontend-design`/most current React+Vite
  guidance recommends; no other test runner is already implied by any existing file.
- **Alternatives considered**: Jest — rejected, requires extra config to work with Vite's
  ESM/JSX transform that Vitest gets for free; Cypress/Playwright E2E — out of scope for this
  round (stakeholder decision was "core logic" tests, not end-to-end browser tests).

---

**Output of this phase**: all gaps identified in `plan.md`'s Summary now have a concrete,
justified remediation decision. Proceeding to Phase 1.
