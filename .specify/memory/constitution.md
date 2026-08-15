<!--
Sync Impact Report
- Version change: (template, unratified) → 1.0.0
- Rationale: Initial ratification. The constitution file previously contained only
  unfilled template placeholders; this is the first concrete adoption, derived from
  CLAUDE.md, project_specification.md, unireview-backend/README.md, and
  specs/001-teacher-review-platform/spec.md.
- Modified principles: n/a (all six are newly defined, none renamed)
- Added sections:
  - Core Principles I–VI
  - Technology Stack & Environment
  - Development Workflow & Quality Gates
  - Governance
- Removed sections: none (template placeholders replaced, no content removed)
- Deferred placeholders: none — every bracketed token has a concrete value
- Templates requiring alignment check (not modified by this command):
  - .specify/templates/plan-template.md — ⚠ pending manual review against Principles I, III, V, VI
  - .specify/templates/spec-template.md — ✅ already technology-agnostic, consistent with Principle II
  - .specify/templates/tasks-template.md — ⚠ pending manual review against Principle VI (build-clean gate)
-->

# UniReview Constitution

## Core Principles

### I. Anonymous-First Privacy (NON-NEGOTIABLE)

Students MUST be able to browse, contribute reviews, vote, and report without creating an
account or supplying any personally identifying information. Any anonymous identifier
(reviewer token) MUST be device/browser-scoped and MUST NOT be linkable to a real identity
by the system. Any network-address data retained for anti-abuse purposes MUST be stored only
as a one-way hash (e.g., SHA-256), never in reversible form. No feature MAY introduce a
requirement for email, phone number, or account login on the student-facing surface without
an explicit constitution amendment.

**Rationale**: Anonymity is the product's core value proposition — it is what makes students
willing to post candid, honest reviews. Any erosion of anonymity directly undermines the
platform's reason to exist.

### II. Spec-Driven Development (Speckit Workflow)

Every feature MUST progress through the Speckit workflow in order — specify → clarify → plan
→ tasks → analyze → implement → converge — before it is considered done. Implementation MUST
NOT begin before a spec exists and ambiguous, scope-defining questions have been resolved via
clarification. `tasks.md` MUST be kept current before code is written for a given unit of
work, and `speckit-analyze` MUST be run before implementation to avoid duplicating existing
structures.

**Rationale**: The project explicitly mandates this workflow (see CLAUDE.md) to keep
implementation aligned with agreed requirements and to prevent ad-hoc, undocumented changes
in a codebase touched by multiple AI assistants and contributors over time.

### III. Layered Backend Architecture

The backend MUST follow a strict three-layer architecture: `Controller` → `Service` →
`Repository`. Controllers MUST NOT contain business logic; business rules MUST live in the
service layer; direct data access MUST be confined to repositories. All schema changes MUST
be expressed as Flyway migrations under `src/main/resources/db/migration` — the schema MUST
NOT be modified by hand against a running database. All error responses MUST be produced
through a `@RestControllerAdvice` and MUST use one unified JSON `ErrorResponse` shape across
every endpoint.

**Rationale**: A consistent layering and a single migration/error path keep a
business-rule-heavy domain (credit gating, moderation, unlocks) auditable and prevent
divergent, ad-hoc error formats or schema drift across a growing API surface.

### IV. Consistent Frontend Design System

The frontend MUST follow the Minimalist Light Theme: clean, high-contrast, flat, free of
heavy gradients or decorative color. Colors MUST be sourced from the CSS variables defined in
`index.css` (`var(--color-...)`) rather than hard-coded values, to keep the UI visually
consistent as it grows. Every file containing JSX MUST use the `.jsx` extension — `.js` is
not permitted for JSX content.

**Rationale**: A shared token-based styling approach and a strict file-extension convention
prevent visual drift and tooling ambiguity as multiple contributors (human and AI) add screens
over time.

### V. Anti-Abuse & Content Integrity by Default

Every write action that creates or mutates public content (submitting a review, voting,
reporting) MUST pass bot/abuse verification before any business logic executes. Review
content MUST pass an instant local filter (banned words, phone numbers, emails, promotional
links) before it is persisted, and MUST then pass an automated toxicity/quality check that
determines whether it auto-publishes or is queued for admin moderation. A duplicate
review from the same identity for the same teacher MUST always be rejected. When the
automated toxicity check is unavailable, the system MUST fail open (publish and credit) to
protect the student experience, while remaining eligible for later moderation via reports —
this fallback MUST NOT be changed to fail closed without an explicit constitution amendment,
since it is a deliberate business tradeoff, not an oversight. Per-network-address submission
rate limits MUST be enforced to deter spam.

**Rationale**: Anonymous, no-login systems are structurally exposed to spam and abuse; this
project's specification treats layered, automatic moderation as a first-class requirement
rather than an afterthought, and explicitly favors availability over strict blocking when the
moderation dependency fails.

### VI. Zero-Error Build Gate (NON-NEGOTIABLE)

A feature MUST NOT be reported or merged as complete while the backend fails to compile, the
frontend fails to build, or any existing test suite is failing. The `speckit-converge` step
MUST be run to completion — build and tests clean at 100% — before implementation work on a
feature is considered finished.

**Rationale**: The project's own workflow (CLAUDE.md, step 7: speckit-converge) makes a clean
build/test run a hard exit criterion, not a suggestion; treating it as optional has previously
allowed broken states to be mistaken for done work.

## Technology Stack & Environment

- **Frontend**: React + Vite, in `unireview/`. Standard commands: `npm run dev` (dev server),
  `npm run build` (production build), `npm run preview` (preview a build).
- **Backend**: Spring Boot 3 + Java 17 + Maven, in `unireview-backend/`. Standard commands:
  `.\mvnw.cmd spring-boot:run` (run), `.\mvnw.cmd compile` (compile),
  `.\mvnw.cmd clean package -DskipTests` (package). Database lifecycle via
  `docker-compose up -d` / `docker-compose down`.
- **Database**: PostgreSQL 18, schema managed exclusively through Flyway migrations.
- **Anti-abuse dependencies**: Google Perspective API (toxicity scoring) and reCAPTCHA v3
  (bot verification) are load-bearing dependencies for Principle V and MUST remain integrated
  wherever content-moderation or write-action verification is implemented, unless replaced by
  an equivalent capability through an explicit amendment.

Introducing a new stack component (a different frontend framework, backend language, database
engine, or migration tool) requires updating both this constitution and `CLAUDE.md` in the
same change — the two documents MUST NOT diverge on stated tech stack.

## Development Workflow & Quality Gates

1. Work on a feature starts with `/speckit-specify` and MUST NOT skip straight to code.
2. Ambiguities affecting scope, privacy/security, or user experience MUST be resolved via
   `/speckit-clarify` rather than silently assumed, except where a documented, reasonable
   default already exists.
3. `task.md` MUST be updated (`/speckit-tasks`) before implementation of the corresponding
   work begins.
4. `/speckit-analyze` MUST be run before writing new code to avoid duplicating existing
   structures or contradicting the current architecture.
5. Implementation (`/speckit-implement`) MUST respect Principles I–V above; a change that
   violates one of them MUST either be redesigned or justified via a constitution amendment
   before it proceeds.
6. `/speckit-converge` MUST be run and MUST reach a 100%-clean build/test state (Principle VI)
   before a feature is reported complete.
7. Any deviation from this workflow (e.g., an urgent hotfix) MUST be called out explicitly to
   the user/reviewer rather than performed silently.

## Governance

This constitution supersedes ad-hoc conventions and prior undocumented practice for this
project. Where CLAUDE.md and this constitution overlap (workflow steps, stack commands), they
MUST be kept consistent; where they conflict, this constitution's Core Principles govern for
questions of privacy, architecture, and quality gates, while CLAUDE.md remains the source of
truth for exact day-to-day command syntax.

**Amendment procedure**: Amendments are made by editing this file directly, updating the Sync
Impact Report at the top of the file, and bumping the version according to the policy below.
A principle MAY only be weakened or removed (MAJOR bump) with an explicit, recorded rationale
in the Sync Impact Report — silent weakening is not permitted.

**Versioning policy** (semantic versioning applied to governance):
- **MAJOR**: Backward-incompatible governance change — a principle is removed or redefined in
  a way that reverses its prior guarantee (e.g., relaxing the anonymity or fail-open rules).
- **MINOR**: A new principle or section is added, or existing guidance is materially expanded.
- **PATCH**: Wording clarifications, typo fixes, or non-semantic refinements.

**Compliance review**: Every feature's `/speckit-plan` and `/speckit-analyze` passes MUST
check the proposed design against these Core Principles before implementation proceeds. A
design that cannot satisfy a NON-NEGOTIABLE principle (I or VI) MUST be revised rather than
implemented as-is.

**Version**: 1.0.0 | **Ratified**: 2026-08-14 | **Last Amended**: 2026-08-14
