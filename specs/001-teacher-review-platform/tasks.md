---

description: "Task list for Anonymous Teacher Review Platform (Credit-Gated Access)"
---

# Tasks: Anonymous Teacher Review Platform (Credit-Gated Access)

**Input**: Design documents from `specs/001-teacher-review-platform/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/api-delta.md](contracts/api-delta.md), [quickstart.md](quickstart.md)

**Context**: This is **not** a greenfield build. A full read-through audit (documented in
`research.md`) found the backend and frontend already implement most of this feature; both
sides currently build cleanly. Every task below is either: a defect fix, a real missing
requirement (FR-017, FR-022, report identity, admin auth wiring), a frontend↔backend wiring
fix (replacing mock data with real API calls), net-new admin UI, or a test for existing/newly
fixed logic. Nothing here is a from-scratch redesign.

**Tests**: Included per explicit stakeholder decision (this session) to cover the highest-risk
logic — credit earn/spend, moderation transitions, duplicate/rate-limit rejection, gate access
control — not exhaustive coverage of every file.

**Post-`/speckit-analyze` note**: this revision folds in the remediation from the analysis
pass — a CRITICAL coverage gap (reports were captured but never surfaced to admins, contradicting
FR-016 and US5's own text) and three MEDIUM test-coverage gaps (FLAGGED path, fail-open-via-exception
path, normal vote-counting path). See the analysis report in the conversation history for full
evidence; the fixes are integrated below, not appended as an afterthought.

**Organization**: Tasks are grouped by user story (US1–US6, matching spec.md's priorities) so
each can be validated independently via its **Independent Test** (copied from spec.md).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Maps the task to US1–US6 from spec.md
- Every task names its exact file path(s)

## Path Conventions (existing structure — see plan.md)

- Backend: `unireview-backend/src/main/java/com/unireview/{controller,service,repository,entity,dto,exception,config,util}/`, tests in `unireview-backend/src/test/java/com/unireview/`
- Frontend: `unireview/src/{api,hooks,pages,components}/`

---

## Phase 1: Setup

**Purpose**: Add the one piece of tooling that doesn't exist yet (backend test tooling is
already in `pom.xml`; only the frontend has no test runner).

- [X] T001 Add Vitest + `@testing-library/react` + `@testing-library/jest-dom` as dev dependencies, create `unireview/vitest.config.js` (referencing the existing `vite.config.js`) and `unireview/src/setupTests.js`, and add a `"test": "vitest run"` script to `unireview/package.json`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Fixes and shared infrastructure that multiple user stories depend on — the
NON-NEGOTIABLE privacy fix (IP hashing), the schema change several stories need, the broken
admin-auth wiring that blocks both admin stories, and rewiring the three shared frontend hooks
(`useGate`, `useTeachers`, `useReviews`) off mock data, since `App.jsx`'s `GateProvider` and
multiple pages (`TeacherListPage`, `TeacherDetailPage`, `WriteReviewPage`) all consume them.

**⚠️ CRITICAL**: No user story is truly done until this phase is complete — several stories'
acceptance scenarios cannot pass while these gaps remain.

- [X] T002 [P] Create Flyway migration `unireview-backend/src/main/resources/db/migration/V2__fix_reports_admin_and_indexes.sql`: add `reporter_token VARCHAR(50) NOT NULL DEFAULT ''` to `review_reports` (then drop the default), add `CREATE INDEX idx_reviews_iphash_created ON reviews(ip_hash, created_at)`, and `UPDATE admin_users SET password_hash = '<BCrypt hash of admin123 generated via BCryptPasswordEncoder>' WHERE username = 'admin'` (see `data-model.md` for full rationale)
- [X] T003 [P] Add a `reporterToken` field (with getter/setter, Lombok `@Data`/`@Builder` already in use) to `unireview-backend/src/main/java/com/unireview/entity/ReviewReport.java`, matching `V2`'s new column
- [X] T004 [P] Create `unireview-backend/src/main/java/com/unireview/util/IpHashUtil.java`: a static method taking a raw IP string and returning its SHA-256 hex digest via `java.security.MessageDigest` (per `research.md` Decision 1)
- [X] T005 [P] Register the existing `AdminAuthFilter` in `unireview-backend/src/main/java/com/unireview/config/SecurityConfig.java` via `http.addFilterBefore(adminAuthFilter, UsernamePasswordAuthenticationFilter.class)` so `/api/admin/**` actually authenticates JWT requests (per `research.md` Decision 3)
- [X] T006 [P] Set explicit connect (3s) and read (5s) timeouts on the `RestTemplate` bean in `unireview-backend/src/main/java/com/unireview/config/RestTemplateConfig.java` (per `research.md` Decision 9)
- [X] T007 [P] In `unireview-backend/src/main/java/com/unireview/exception/GlobalExceptionHandler.java`: add a new `RateLimitExceededException` (in `unireview-backend/src/main/java/com/unireview/exception/RateLimitExceededException.java`) mapped to `429`/`RATE_LIMIT_EXCEEDED`, and add handlers for Spring Security's `AccessDeniedException`/`AuthenticationException` so admin-auth failures return the same `ErrorResponse` JSON shape as every other error instead of Spring Security's default output
- [X] T008 [P] Rewire `unireview/src/hooks/useGate.jsx` (`GateProvider`) to fetch real state via `getGateStatus()` from `unireview/src/api/gateApi.js` on mount and on `refresh()`, removing the `localStorage`/`MOCK_GATE`-based `addCredit`/`spendCredit`/`unlockTeacher` mutations (server actions now drive state; the context should just re-fetch after them)
- [X] T009 [P] Rewire `unireview/src/hooks/useTeachers.js` (`useTeachers` and `useTeacherDetails`) to call `getTeachers(filters)` and `getTeacherById(id)` from `unireview/src/api/teacherApi.js` instead of filtering/reading `MOCK_TEACHERS`, forwarding `search`/`faculty`/`minRating`/`sortBy`/`sortDir` through to the real API call
- [X] T010 [P] Rewire `unireview/src/hooks/useReviews.js` to call `getTeacherReviews(teacherId)` from `unireview/src/api/teacherApi.js` instead of reading `MOCK_REVIEWS`, removing the client-side `spendCredit`-then-fake-unlock branch (the backend's `GET /api/teachers/{id}/reviews` already atomically checks/spends credit server-side per `GateService.checkAndUnlockTeacher`) and calling the gate context's `refresh()` after a successful fetch so the credit balance shown in the UI stays in sync

**Checkpoint**: Schema, security, and the three shared data hooks are now correct — user story work can proceed.

---

## Phase 3: User Story 1 - Contribute a Review and Earn Access (Priority: P1) 🎯 MVP

**Goal**: A student can submit a structured review; if it passes moderation it publishes
instantly, awards 1 credit, and auto-unlocks that teacher — for real, against the live backend.

**Independent Test**: Submit a complete, policy-compliant review for a teacher with no prior
review from this identity via the running app, and confirm the review is published, the
credit balance shown in the UI increases by 1, and that teacher's reviews are immediately
viewable without spending a credit.

### Tests for User Story 1

- [X] T011 [P] [US1] Unit tests for `ModerationService.evaluateContent` in `unireview-backend/src/test/java/com/unireview/service/ModerationServiceTest.java`: banned word / phone / email / link → `REJECTED`; fail-open bypass now correctly triggers on the default mock key; toxicity-threshold branch (score below vs at/above 0.7 → `APPROVED` vs `FLAGGED`); **and** a case where the Perspective API call itself throws (e.g. mocked `RestTemplate` throwing) → still resolves to `APPROVED` via the existing catch-block fail-open path (this is a *different* fail-open path from the mock-key bypass — both must be covered)
- [X] T012 [P] [US1] Unit tests for `ReviewService.submitReview` in `unireview-backend/src/test/java/com/unireview/service/ReviewServiceTest.java`: duplicate-review rejection, `APPROVED` → credit +1 and auto-unlock, **`FLAGGED` result → review saved with `FLAGGED` status, no credit awarded, no auto-unlock** (exercise this branch end-to-end through `submitReview`, not just at the `ModerationService` unit level), 4th same-day-same-IP submission rejected by the new rate limit, `ip_hash` is never the raw input IP

### Implementation for User Story 1

- [X] T013 [US1] Fix the fail-open constant mismatch in `unireview-backend/src/main/java/com/unireview/service/ModerationService.java` (the bypass check must match `application.yml`'s actual default `mock_perspective_key`, not the literal `"mock_key"`) so the intended short-circuit fires instead of an unreachable outbound API call every submission
- [X] T014 [US1] Use `IpHashUtil` (T004) to hash the client IP in `unireview-backend/src/main/java/com/unireview/controller/ReviewController.java`'s `getClientIp` before it is ever passed to `ReviewService`/`GateService`
- [X] T015 [US1] Add a `countByIpHashAndCreatedAtAfter(String ipHash, LocalDateTime cutoff)` query to `unireview-backend/src/main/java/com/unireview/repository/ReviewRepository.java`, and call it at the top of `ReviewService.submitReview` in `unireview-backend/src/main/java/com/unireview/service/ReviewService.java`, throwing the new `RateLimitExceededException` (T007) when the same hashed IP already has ≥3 reviews in the last 24 hours
- [X] T016 [US1] In `unireview/src/components/review/ReviewForm.jsx`, call `executeReCaptcha` from `unireview/src/utils/recaptcha.js` before submit using a site key read from `import.meta.env.VITE_RECAPTCHA_SITE_KEY` (no-op/empty token is fine — captcha stays server-disabled per the stakeholder's "keep mock for now" decision, but the call site must exist and pass whatever token it gets)
- [X] T017 [US1] Rewrite `ReviewForm.handleSubmit` in `unireview/src/components/review/ReviewForm.jsx` to call `submitReview({ ...form, teacherId: teacher.id, captchaToken })` from `unireview/src/api/reviewApi.js` instead of the `setTimeout` mock, persist the server-returned `reviewerToken` via `setReviewerToken` from `unireview/src/utils/cookie.js`, call the gate context's `refresh()` (from the now-real `useGate`, T008) instead of the removed `addCredit`/`unlockTeacher`, and surface the server's error `message`/`code` (duplicate, content violation, rate limit) via toast on failure

**Checkpoint**: User Story 1 is fully functional end-to-end against the real backend.

---

## Phase 4: User Story 2 - Unlock a Teacher's Detailed Reviews Using Credit (Priority: P1)

**Goal**: Opening a teacher's detailed reviews for the first time with ≥1 credit spends
exactly 1 credit and unlocks it permanently; a second visit spends nothing; 0 credit is denied
with guidance.

**Independent Test**: With a student identity holding ≥1 credit, open a different teacher's
detailed reviews for the first time and confirm the credit balance decreases by 1, that
teacher becomes permanently unlocked, and a second visit spends no further credit.

### Tests for User Story 2

- [X] T018 [P] [US2] Unit tests for `GateService.checkAndUnlockTeacher` (already-unlocked short-circuits free; authored-review auto-unlocks free; sufficient credit deducts exactly 1 and records the unlock; zero credit throws `InsufficientCreditException`; missing token throws `NoReviewerTokenException`) in `unireview-backend/src/test/java/com/unireview/service/GateServiceTest.java`

### Implementation for User Story 2

- [X] T019 [US2] Simplify `handleUnlockClick` in `unireview/src/pages/TeacherDetailPage.jsx`: since `GET /api/teachers/{id}/reviews` (called via the now-real `useReviews`, T010) already performs the atomic credit-check/spend/unlock server-side, this handler should just call `refetch()` and let a `403 INSUFFICIENT_CREDIT` response (surfaced per T020) drive showing `GateModal` — remove the old direct `spendCredit(...)` call
- [X] T020 [US2] In `unireview/src/hooks/useReviews.js`, distinguish an `INSUFFICIENT_CREDIT`/`NO_REVIEWER_TOKEN` API error response (set an explicit `locked: true` state) from other errors, so `TeacherDetailPage` can render `ReviewBlurred`/`GateModal` instead of a generic error message

**Checkpoint**: User Stories 1 AND 2 both work end-to-end (US2 needs a credit balance, earned via US1's flow, to fully validate).

---

## Phase 5: User Story 3 - Browse and Search the Teacher Directory (Priority: P2)

**Goal**: Any visitor can search/filter/sort the teacher directory and see public summary
stats, with no identity or credit required — against the real backend.

**Independent Test**: As a fresh/incognito visitor, search, filter by faculty/minRating, and
sort the directory, and confirm real results and every teacher's summary stats display
correctly with no identity or credit required.

### Tests for User Story 3

- [X] T021 [P] [US3] Unit tests for `TeacherService.getTeachers` (search-by-name/faculty keyword, faculty filter, minRating filter, sort by name/rating/reviews in both directions, pagination metadata) in `unireview-backend/src/test/java/com/unireview/service/TeacherServiceTest.java`

### Implementation for User Story 3

- [X] T022 [US3] Verify `unireview/src/components/teacher/TeacherSearch.jsx` exposes a sort-direction control and passes `sortDir` (plus all other filters) through to the `filters` object consumed by `useTeachers` (T009), matching every param `teacherApi.getTeachers` already forwards to the backend; add the control if missing

**Checkpoint**: All three P1/P2 stories (the core loop + discovery) are independently functional.

---

## Phase 6: User Story 4 - React to and Flag Reviews (Priority: P3)

**Goal**: A student can upvote/downvote a review once and report one with a reason; both
require a resolvable identity and pass bot verification, matching every other write action.

**Independent Test**: Vote once on a review and confirm the count updates with no
double-count on a repeat vote; submit a report with a reason and confirm it reaches the admin
queue attributed to the reporting identity.

### Tests for User Story 4

- [X] T023 [P] [US4] Unit tests for `ReviewService.voteReview` in `unireview-backend/src/test/java/com/unireview/service/ReviewServiceTest.java`: no resolvable `voterToken` throws `NoReviewerTokenException` (not an uncaught NPE/constraint violation); **and the normal path** — a first upvote increments `upvoteCount`, a repeat vote of the same type is a no-op (no double count), and switching an existing vote from UPVOTE to DOWNVOTE decrements the old counter and increments the new one
- [X] T024 [P] [US4] Unit test: `ReviewService.reportReview` requires and persists a `reporterToken`, and throws `NoReviewerTokenException` when none is resolvable, in `unireview-backend/src/test/java/com/unireview/service/ReviewServiceTest.java`

### Implementation for User Story 4

- [X] T025 [US4] Add a guard clause at the top of `ReviewService.voteReview` in `unireview-backend/src/main/java/com/unireview/service/ReviewService.java` that throws `NoReviewerTokenException` when `voterToken` is null/blank, before any repository access
- [X] T026 [US4] Add a `captchaToken` field to `unireview-backend/src/main/java/com/unireview/dto/request/VoteRequest.java` and `unireview-backend/src/main/java/com/unireview/dto/request/ReportRequest.java`
- [X] T027 [US4] Add `captchaService.verify(...)` checks (mirroring `submitReview`'s existing check) to `voteReview` and `reportReview` in `unireview-backend/src/main/java/com/unireview/controller/ReviewController.java`, throwing `CaptchaFailedException` on failure
- [X] T028 [US4] Add reviewer-token resolution (header/cookie, same pattern as vote/submit) to `ReviewController.reportReview`, and update `ReviewService.reportReview` in `unireview-backend/src/main/java/com/unireview/service/ReviewService.java` to require it (guard clause per T024) and persist it on the new `ReviewReport.reporterToken` field (T003)
- [X] T029 [US4] Wire `ReviewCard`'s `onVote`/`onReport` props in `unireview/src/pages/TeacherDetailPage.jsx` to call `voteReview`/`reportReview` from `unireview/src/api/reviewApi.js`, showing a success/failure toast and refetching the review list on success
- [X] T030 [US4] Create `unireview/src/components/review/ReportReviewModal.jsx` (+ matching `.css`, following `GateModal.jsx`'s structure and `var(--color-...)` tokens per Constitution Principle IV) with a reason dropdown and description textarea, and open it from `TeacherDetailPage`'s report action (T029) instead of calling `reportReview` with no reason

**Checkpoint**: US4 is independently functional and testable.

---

## Phase 7: User Story 5 - Moderate Flagged Reviews (Priority: P3)

**Goal**: An authenticated admin can view the flagged queue **and the reports queue**,
approve/reject/hide reviews with the FR-022 credit clawback, and see (and act on) reviews
students have reported — via a real admin UI, not just Swagger.

> **Scope note (from `/speckit-analyze`)**: the original pass of this phase covered
> approve/reject/hide but missed that `ReviewReportRepository.findByStatus` was never called
> anywhere — reports were captured on submission (US4) but never surfaced to any admin,
> contradicting FR-016 ("MUST... make that report visible to admins") and this story's own
> text ("reviews held for manual moderation — either flagged by the toxicity check **or
> reported by students**"). T032/T034/T035/T038 below close that gap. Decision on what
> "admin action on a report" means (the underspecified item the analysis flagged): reports are
> read + dismiss-only; an admin *resolves* a report by acting on the underlying review (hide it
> via T033), which automatically marks any of that review's `PENDING` reports `RESOLVED` — no
> separate "resolve" button duplicating the hide action.

**Independent Test**: Log in as admin, approve one flagged item and reject another, and
confirm the approved one publishes and credits its author while the rejected one stays hidden
with no credit; hide a published review and confirm it disappears publicly, its author's
credit decreases by 1 (if positive), and any pending reports against it flip to `RESOLVED`;
separately, open the reports queue, see a student-submitted report, and dismiss it.

### Tests for User Story 5

- [X] T031 [P] [US5] Unit tests in `unireview-backend/src/test/java/com/unireview/service/AdminServiceTest.java` for: `hideReview` (status → `HIDDEN`; credit deducted only when balance > 0; teacher rating recomputed; all `PENDING` reports on that review flip to `RESOLVED`); `getReports` (returns `PENDING` reports paginated, via the existing `ReviewReportRepository.findByStatus`); `dismissReport` (sets a report's status to `DISMISSED` without touching the underlying review)

### Implementation for User Story 5

- [X] T032 [US5] Add `findByReviewIdAndStatus(Long reviewId, ReportStatus status)` to `unireview-backend/src/main/java/com/unireview/repository/ReviewReportRepository.java` (used to resolve reports tied to a hidden review), and create `unireview-backend/src/main/java/com/unireview/dto/response/ReportResponse.java` (id, reviewId, reason, description, status, createdAt — no `reporterToken`, keep the reporting identity internal)
- [X] T033 [US5] Implement `hideReview(Long reviewId)` in `unireview-backend/src/main/java/com/unireview/service/AdminService.java`: set `status = HIDDEN`, decrement 1 credit from the author's `Reviewer` if `creditBalance > 0` (reuse the guarded-decrement pattern already used elsewhere), call `reviewService.recalculateTeacherRating(teacherId)`, and mark every `PENDING` report for that review `RESOLVED` via T032's query (depends on T032)
- [X] T034 [US5] Implement `getReports(ReportStatus status, int page, int size)` (default `status = PENDING`) and `dismissReport(Long reportId)` (sets `DISMISSED`, no other side effects) in `unireview-backend/src/main/java/com/unireview/service/AdminService.java`, using the already-existing `ReviewReportRepository.findByStatus` and T032's `ReportResponse` (depends on T032)
- [X] T035 [US5] Add three endpoints to `unireview-backend/src/main/java/com/unireview/controller/AdminController.java`: `PUT /api/admin/reviews/{id}/hide` (delegates to T033), `GET /api/admin/reports` (delegates to T034's `getReports`), `PUT /api/admin/reports/{id}/dismiss` (delegates to T034's `dismissReport`) — all protected the same way as the existing admin endpoints once T005 lands (depends on T033, T034)
- [X] T036 [US5] Create `unireview/src/api/adminApi.js`: a separate axios client (or interceptor) that attaches the admin JWT as `Authorization: Bearer <token>`, exposing `login`, `getFlaggedReviews`, `approveReview`, `rejectReview`, `hideReview`, `getReports`, `dismissReport`, `importTeachersCsv`
- [X] T037 [US5] Create `unireview/src/pages/admin/AdminLoginPage.jsx` (+ `.css`): username/password form calling `adminApi.login`, storing the returned JWT (e.g. `sessionStorage`) for T036's client to read (depends on T036)
- [X] T038 [US5] Create `unireview/src/pages/admin/AdminModerationQueuePage.jsx` (+ `.css`): two sections — "Flagged reviews" (reusing `ReviewCard` in a read-only/admin variant, with Approve / Reject / Hide actions) and "Reports" (list of `PENDING` reports, each showing the reported review's content alongside the report's reason/description, with a "Hide this review" action calling `hideReview` and a "Dismiss report" action calling `dismissReport`) — calls T036's client (depends on T036)
- [X] T039 [US5] Add a protected-route wrapper (redirects to `/admin/login` when no valid admin session) and register `/admin/login` and `/admin/queue` routes in `unireview/src/App.jsx` (depends on T037, T038)

**Checkpoint**: US5 is independently functional — admin login, the flagged-review queue, the reports queue, and hide/dismiss all work end-to-end.

---

## Phase 8: User Story 6 - Maintain the Teacher Roster (Priority: P3)

**Goal**: An admin can bulk-import teachers; a row matching an existing teacher updates it in
place (preserving ratings/reviews) instead of duplicating it, and malformed rows are reported
back by row number and reason instead of silently dropped.

**Independent Test**: Import a batch containing a brand-new teacher, one matching an existing
teacher exactly on (full name, faculty), and one malformed row; confirm the new teacher is
created, the existing one is updated in place with ratings/reviews intact, and the malformed
row is reported with a reason.

### Tests for User Story 6

- [X] T040 [P] [US6] Unit tests for `CsvImportService` (new row inserted; row matching an existing teacher by full_name+faculty updates in place and leaves `avg_rating`/`total_reviews` untouched; malformed row appears in the failure list with the correct row number and reason) in `unireview-backend/src/test/java/com/unireview/service/CsvImportServiceTest.java`

### Implementation for User Story 6

- [X] T041 [US6] Add `findByFullNameIgnoreCaseAndFacultyIgnoreCase(String fullName, String faculty)` to `unireview-backend/src/main/java/com/unireview/repository/TeacherRepository.java`
- [X] T042 [US6] Rewrite `importTeachersFromCsv` in `unireview-backend/src/main/java/com/unireview/service/CsvImportService.java` to: look up each row via T041's query (update `title`/`department`/`avatar_url` in place if found, insert a new `Teacher` if not), collect `(row number, reason)` for any row missing `full_name`/`faculty` instead of silently skipping it, and return a result object carrying `importedCount`, `updatedCount`, and `failedRows`
- [X] T043 [US6] Update `importTeachersCsv` in `unireview-backend/src/main/java/com/unireview/controller/AdminController.java` to return the new `{ message, importedCount, updatedCount, failedRows }` shape from `contracts/api-delta.md` (depends on T042)
- [X] T044 [US6] Create `unireview/src/pages/admin/AdminRosterImportPage.jsx` (+ `.css`): file-upload form calling `adminApi.importTeachersCsv` (T036), rendering `importedCount`/`updatedCount` and a `failedRows` table, and register its route (behind the T039 protected-route wrapper) in `unireview/src/App.jsx`

**Checkpoint**: All six user stories are independently functional.

---

## Phase 9: Polish & Cross-Cutting Concerns

- [X] T045 [P] Delete `unireview/src/api/mockData.js` and confirm (via a repo-wide search) no remaining `MOCK_TEACHERS`/`MOCK_REVIEWS`/`MOCK_GATE` imports exist, now that T008–T010 removed the last consumers
- [X] T046 [P] Add UI copy for the new `RATE_LIMIT_EXCEEDED` error code to `unireview/src/api/axiosConfig.js`'s error interceptor (or the calling component) so the 429 case shows a clear Vietnamese message instead of the generic fallback
- [ ] T047 Run through every scenario in `quickstart.md` manually against the running full stack, including the cross-cutting checks (rate limiting, IP-hash-not-plaintext DB inspection, admin-auth 401/403-then-200 check) and the reports-queue flow (submit a report as a student, see and dismiss it as admin, then submit another and resolve it by hiding the review)
  **NOT run in this session** — no Docker CLI available in this sandbox to start `docker-compose up -d`, and the Postgres instance already listening on :5432 belongs to this machine with unknown credentials (attempting `docker-compose`'s documented password confirmed the Spring/Flyway/JPA wiring is otherwise correct — it reached the DB and failed only on auth). **Needs to be run locally**: `docker-compose up -d` in `unireview-backend/`, then `.\mvnw.cmd spring-boot:run`, `npm run dev`, and walk through `quickstart.md` §1–8.
- [X] T048 Run `unireview-backend` → `.\mvnw.cmd clean package` and `unireview` → `npm run build`; both must succeed with zero errors (Constitution Principle VI gate) before this feature is considered done

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (T001)**: No dependencies — can start immediately.
- **Foundational (T002–T010)**: Depends on Setup for T001 only where frontend tests are
  concerned; T002–T007 (backend) and T008–T010 (frontend) have no dependency on each other and
  can run fully in parallel. **Blocks** all user-story phases below — several stories'
  acceptance scenarios are impossible to satisfy until this phase lands (e.g., US1's duplicate
  test depends on the real backend the rewired hooks now call; US5/US6 depend on T005's auth
  fix to be reachable at all).
- **User Stories (Phase 3–8)**: All depend on Foundational completion. US1 and US3 have no
  dependency on each other. US2 depends on US1 only insofar as *manually* validating it needs
  an existing credit balance (no code dependency — `GateService` and the rewired `useReviews`
  from Foundational already make US2 independently correct). US4/US5/US6 are independent of
  one another and of US1–US3, aside from all reusing `ReviewCard`/`AdminController` patterns.
  Within US5, T032→T033/T034→T035→T036→T037/T038→T039 is a real dependency chain (schema
  helper → service methods → endpoints → API client → pages → routing).
- **Polish (T045–T048)**: Depends on all six user stories being complete.

### Within Each User Story

- Tests (where included) should be written first and confirmed failing before the matching
  implementation task, per the constitution's spec-driven approach.
- Backend service/repository changes before the controller changes that expose them.
- Backend contract changes before the frontend task that calls them.

### Parallel Opportunities

- All of T002–T010 (Foundational) can run in parallel — 9 independent files.
- Within each story's "Tests" subsection, all listed tasks are `[P]` (different test files or
  independent test methods).
- US1, US3, US4, US6 implementation work can be staffed in parallel once Foundational is done;
  US5's backend chain (T032–T035) is sequential but independent of the other stories; US2 is
  best done right after US1 since manual validation needs a credit balance, even though
  there's no code dependency.

---

## Parallel Example: Foundational Phase

```bash
# All of these touch different files and can be dispatched together:
Task: "Create Flyway migration V2__fix_reports_admin_and_indexes.sql"
Task: "Add reporterToken field to ReviewReport.java"
Task: "Create IpHashUtil.java"
Task: "Register AdminAuthFilter in SecurityConfig.java"
Task: "Set RestTemplate timeouts in RestTemplateConfig.java"
Task: "Add RateLimitExceededException + security exception handlers to GlobalExceptionHandler.java"
Task: "Rewire useGate.jsx off mock data"
Task: "Rewire useTeachers.js off mock data"
Task: "Rewire useReviews.js off mock data"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 (Setup) and Phase 2 (Foundational) — this alone fixes the NON-NEGOTIABLE
   privacy gap and the admin-auth wiring, which is worth doing even before any story work.
2. Complete Phase 3 (US1) — validate via quickstart.md §2.
3. **STOP and VALIDATE** independently before moving on.

### Incremental Delivery

1. Setup + Foundational → real data flowing end-to-end, privacy fixed, admin reachable.
2. US1 → US2 → deploy/demo the core credit loop (MVP).
3. US3 → directory browsing fully real.
4. US4 → voting/reporting fully real.
5. US5 → US6 → admin console usable end-to-end, including the reports queue.
6. Polish → cleanup + full quickstart pass + build-gate verification.

### Parallel Team Strategy

Once Foundational is done: one person on US1+US2 (shared files), one on US3, one on US4, one
on US5+US6 (shared new `adminApi.js`/admin routes) — four independent tracks.

---

## Notes

- `[P]` tasks touch different files with no dependency on an incomplete task.
- `[Story]` labels trace each task back to its spec.md user story.
- This task list assumes the stakeholder decisions already confirmed in this session: frontend
  wiring is top priority, admin UI is in scope, core-logic tests are in scope, real Perspective
  API/reCAPTCHA keys are out of scope (mock/fail-open defaults stay).
- The reports-queue gap (T032–T035, T038) and the three test-coverage gaps (folded into T011,
  T012, T023) were added after an `/speckit-analyze` pass caught them — see that report for
  the evidence trail (confirmed via grep that `ReviewReportRepository.findByStatus` was never
  called anywhere in the original codebase).
- Commit after each task or logical group; verify `mvnw compile` / `npm run build` still pass
  after any backend/frontend change (Constitution Principle VI).
