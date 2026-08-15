# Implementation Plan: Anonymous Teacher Review Platform (Credit-Gated Access)

**Branch**: `001-teacher-review-platform` | **Date**: 2026-08-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-teacher-review-platform/spec.md`

## Summary

UniReview lets students anonymously review teachers and unlocks detailed reviews through a
credit system (write an approved review → earn 1 credit → spend it to unlock another
teacher). **This is not a greenfield build**: the backend (Spring Boot) and frontend (React)
already contain a substantial implementation of this exact feature — most controllers,
services, entities, the Flyway schema, and the core UI pages already exist and both sides
currently build cleanly. A full read-through audit (this session) found the remaining work is
**closing real, specific gaps** between what's built and what the spec requires, not
designing from scratch. The approach for this plan is therefore: keep the existing
architecture and stack as-is, and scope Phase 0/1 outputs around the concrete list of gaps
below rather than inventing a new design.

**Confirmed gaps to close** (grouped by theme; full detail with file:line citations in
`research.md`):
1. **Frontend↔backend wiring** (top priority per stakeholder decision): `useTeachers.js`,
   `useGate.jsx`, `useReviews.js`, and `ReviewForm.jsx` currently run on mock data /
   `localStorage` instead of calling the already-implemented `api/*.js` clients.
2. **Admin frontend** (in scope per stakeholder decision): no login, moderation-queue, or
   CSV-import UI exists yet, even though the backend `AdminController` supports all three.
3. **Privacy defect (Constitution Principle I, NON-NEGOTIABLE)**: IP addresses are persisted
   in plaintext (`ip_hash` columns are never actually hashed) — MUST be fixed, not deferred.
4. **Admin auth is wired incorrectly**: `AdminAuthFilter` exists but is never registered in
   `SecurityConfig`, so `/api/admin/**` rejects every request, valid JWT or not.
5. **Missing functional requirements**: FR-017 (per-IP rate limiting), FR-022 (admin
   hide/remove with credit clawback), report-identity capture (FR-016), CSV upsert-on-match
   (FR-023), captcha verification on vote/report (FR-018).
6. **Correctness bugs**: vote endpoint NPEs to a raw 500 when no reviewer token is sent;
   `ModerationService`'s fail-open bypass constant doesn't match `application.yml`'s default
   (harmless in outcome today, but makes every submission attempt a live external API call
   instead of the intended short-circuit); admin-seed password hash looks malformed.
7. **Test coverage** (in scope per stakeholder decision): zero automated tests exist on either
   side today, despite test dependencies already present in `pom.xml`.

Real API keys for Perspective API / reCAPTCHA are explicitly **out of scope** for this round
(stakeholder decision: keep the existing mock/fail-open/disabled defaults).

## Technical Context

**Language/Version**: Java 17 (backend), JavaScript/JSX via React 19 + Vite (frontend) — both
already in use, not a new choice.

**Primary Dependencies**: Backend — Spring Boot 3.2.3 (Web, Data JPA, Security, Validation),
Flyway, PostgreSQL JDBC driver, jjwt 0.12.5, Lombok, springdoc-openapi 2.3.0, Apache Commons
CSV 1.10.0. Frontend — react-router-dom, react-hot-toast, axios (via `axiosConfig.js`). All
already declared in `pom.xml` / `package.json`; no new dependency is required for any
confirmed gap **except** a rate-limiting/IP-hashing utility, which will use the JDK's built-in
`java.security.MessageDigest` (SHA-256) — no new library needed.

**Storage**: PostgreSQL 18, schema owned exclusively by Flyway migrations under
`unireview-backend/src/main/resources/db/migration`. Existing baseline: `V1__initial_schema.sql`.
This plan requires one additive migration (`V2__...sql`, see `data-model.md`) — no destructive
schema change.

**Testing**: Backend — JUnit 5 + Mockito + `spring-boot-starter-test` /
`spring-security-test` (already in `pom.xml`, unused so far). Frontend — Vitest + React
Testing Library (not yet installed; will be added as a dev dependency since the frontend has
no test runner configured at all). Scope: unit/integration tests for the highest-risk logic
(credit earn/spend, duplicate-review prevention, moderation status transitions, gate access
control, rate limiting) per stakeholder decision — not full coverage of every file.

**Target Platform**: Web application — backend serves a REST API on `localhost:8080`,
frontend is a Vite dev server on `localhost:5173` (per constitution CORS config), no separate
deployment target defined yet (out of scope for this feature).

**Project Type**: Web application (existing separate `unireview/` frontend +
`unireview-backend/` backend, per constitution's Technology Stack & Environment section — not
a new structural decision).

**Performance Goals**: No new targets beyond spec.md's Success Criteria (SC-001–SC-007), which
are UX-latency framed (e.g., "review published and credited within a few seconds"). No
concrete req/s or concurrency target has been set by the business; this plan does not invent
one, since the existing stack (single Spring Boot instance + PostgreSQL) already comfortably
serves the implied scale of a single university's student body without special tuning.

**Constraints**: Must preserve the existing entity/table structure (additive migration only);
must preserve existing public REST contracts used by the frontend/README (`GET /api/teachers`,
etc.) — the only contract changes are additive (a new admin hide endpoint) or tightening an
existing one to require an already-documented header (`X-Reviewer-Token` on report).

**Scale/Scope**: Single-university deployment scope, matching spec.md's assumptions (no
multi-tenant, no multi-language). Six user stories from spec.md, all touched by this plan
except none are net-new end-to-end (US1–US3 need wiring/bug fixes, US4–US6 need both backend
gap-filling and net-new frontend UI).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|---|---|---|
| I. Anonymous-First Privacy (NON-NEGOTIABLE) | ❌ **VIOLATED today** | `ip_hash` columns store plaintext IPs (`ReviewController.getClientIp` → `ReviewService`/`GateService`, no hashing anywhere in the codebase). **This plan MUST fix it** — SHA-256 hashing at the point of capture, before any persistence — as a first-class task, not an optional cleanup. Gate re-check after Phase 1: PASS once `research.md`'s hashing decision is applied to `data-model.md`'s field definitions and a task exists to update the two call sites. |
| II. Spec-Driven Development | ✅ On track | This plan follows `specify → clarify → plan`; `tasks.md` will be generated next and `speckit-analyze` will run before implementation, per the constitution. |
| III. Layered Backend Architecture | ✅ Mostly followed, one wiring bug | Controller→Service→Repository separation is real and consistent across the codebase; Flyway-only schema changes are already the practice; the one defect (`AdminAuthFilter` not registered) is a configuration bug, not an architectural violation, and is captured as a task. |
| IV. Consistent Frontend Design System | ✅ Followed | Existing components already use `var(--color-...)` tokens from `index.css` and `.jsx` extensions throughout; new admin-UI components must follow the same convention (captured as a task-level constraint, not a new decision). |
| V. Anti-Abuse & Content Integrity by Default | ❌ **Partially violated today** | Bot verification (FR-018) is enforced on review submission but **not** on vote/report; per-IP rate limiting (FR-017) is entirely unimplemented. Both are explicit MUSTs in the constitution's Principle V, not optional hardening — both are captured as required tasks, not deferred. |
| VI. Zero-Error Build Gate (NON-NEGOTIABLE) | ✅ Currently passing | Verified this session: `mvnw compile` succeeds cleanly and `npm run build` succeeds cleanly on the current `main` branch state. This plan's tasks must preserve that — `speckit-converge` will re-verify before the feature is considered done. |

**No unjustified violations require Complexity Tracking.** The two ❌ items above are pre-existing defects being fixed by this plan, not new complexity being introduced — there is nothing to trade off or justify; they are simply required work.

**Post-Phase 1 re-check**: PASS. `research.md` Decision 1 (SHA-256 IP hashing at the point of
capture) and Decision 2 (rate limiting reusing the now-hashed `ip_hash` column) resolve the
Principle I violation; Decision 2 (rate limiting) and Decision 7 (report identity + captcha on
vote/report) resolve the Principle V gaps. `data-model.md`'s `V2` migration and
`contracts/api-delta.md`'s new/changed endpoints operationalize both. No remaining Constitution
gate failures going into `/speckit-tasks`.

## Project Structure

### Documentation (this feature)

```text
specs/001-teacher-review-platform/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output — gap-remediation decisions
├── data-model.md         # Phase 1 output — existing entities + additive schema change
├── quickstart.md        # Phase 1 output — manual end-to-end validation guide
├── contracts/           # Phase 1 output — REST contract, existing + delta
└── tasks.md             # Phase 2 output (/speckit-tasks command — not created here)
```

### Source Code (repository root)

This is the **existing, real structure** already in use — not a new choice between template
options. Both apps already exist at the repo root per `CLAUDE.md`.

```text
unireview-backend/                        # Spring Boot 3 + Java 17 + Maven
├── src/main/java/com/unireview/
│   ├── config/          # CorsConfig, RestTemplateConfig, SecurityConfig, SwaggerConfig
│   ├── controller/       # TeacherController, ReviewController, GateController, AdminController
│   ├── dto/{request,response}/
│   ├── entity/           # Teacher, Review, Reviewer, UnlockedTeacher, AdminUser,
│   │                     # ReviewVote, ReviewReport, Subject, TeacherSubject
│   ├── enums/             # Difficulty, Attendance, MaterialsAllowed, Recommendation,
│   │                     # ReportStatus, ReviewStatus, VoteType, Workload
│   ├── exception/        # GlobalExceptionHandler + 6 custom exceptions
│   ├── repository/       # Spring Data JPA repositories, one per entity
│   ├── security/         # AdminAuthFilter, JwtTokenProvider
│   └── service/          # AdminService, CaptchaService, CsvImportService, GateService,
│                         # ModerationService, ReviewService, TeacherService
├── src/main/resources/
│   ├── application.yml
│   ├── banned_words.txt
│   └── db/migration/     # V1__initial_schema.sql (this plan adds V2__...)
└── src/test/              # currently empty — this plan adds JUnit/Mockito tests here

unireview/                                 # React 19 + Vite
├── src/api/               # axiosConfig.js, teacherApi.js, reviewApi.js, gateApi.js
├── src/components/{common,layout,review,teacher}/
│   └── [new] admin/       # this plan adds admin-only components here
├── src/hooks/             # useTeachers.js, useReviews.js, useGate.jsx (this plan rewires
│                         # these to call src/api/*.js instead of mock data)
├── src/pages/             # HomePage, TeacherListPage, TeacherDetailPage, WriteReviewPage
│   └── [new] admin/       # this plan adds AdminLoginPage, AdminQueuePage, AdminImportPage
├── src/utils/             # cookie.js, recaptcha.js
└── src/*.test.jsx          # this plan adds Vitest + RTL as a new dev dependency
```

**Structure Decision**: Reuse the existing `unireview/` + `unireview-backend/` layout exactly
as-is (matches the constitution's Technology Stack & Environment section). No new top-level
directories, no restructuring — new backend code lands in the existing package-per-layer
folders, new frontend code lands in the existing `pages/`/`components/`/`hooks/` folders, with
one new `admin/` subfolder on each side for the net-new admin UI (mirroring the existing
`review/`/`teacher/` component groupings, not a new pattern).

## Complexity Tracking

*No entries — no unjustified constitution violations are being introduced. The two
Constitution Check gaps above are existing defects this plan fixes, not new complexity.*
