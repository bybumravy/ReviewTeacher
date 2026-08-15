# Phase 1 Data Model: Anonymous Teacher Review Platform

This documents the **existing** schema (`V1__initial_schema.sql`, already applied) plus the one
additive migration this plan requires. No existing table, column, or constraint is removed or
redefined — see `research.md` for why each change is additive-only.

## Existing Entities (unchanged)

### `teachers`
Faculty member profile. Fields: `id`, `full_name`, `title`, `faculty`, `department`,
`avatar_url`, `avg_rating` (recomputed on publish/approve, and — after this plan — on
hide/remove too), `total_reviews`, `created_at`, `updated_at`. Indexed on `faculty` and
`avg_rating DESC`.

### `subjects` / `teacher_subjects`
Course catalog and teacher-to-subject/semester mapping. Unchanged, out of scope for this plan's
gaps (not touched by any confirmed defect).

### `reviewers`
Anonymous identity. Fields: `id`, `token` (UUIDv4, unique), `review_count`, `credit_balance`
(CHECK ≥ 0), `ip_hash`, `created_at`, `last_active_at`.
**Behavior change (no schema change)**: `ip_hash` must be populated with a SHA-256 digest going
forward (Decision 1 in `research.md`) instead of the raw IP — the column already exists and is
already sized for a hex digest (`VARCHAR(64)` = exactly a SHA-256 hex string length).

### `reviews`
One submission per (reviewer, teacher) pair, enforced by `uq_reviewer_teacher`. Carries
structured ratings, qualitative fields, `status` (`PENDING`/`APPROVED`/`FLAGGED`/`REJECTED`/
`HIDDEN` — `HIDDEN` exists in the enum today but no code path sets it yet), `toxicity_score`,
`ip_hash` (same hashing behavior change as above), vote counts.
**Behavior change (no schema change)**: `HIDDEN` becomes reachable via the new admin
hide/remove flow (FR-022); rate limiting (FR-017) queries this table by `(ip_hash, created_at)`
— no new column, an index already exists on `status` but not on `ip_hash`; add one (see
Migration below) so the daily-count query stays cheap.

### `unlocked_teachers`
Permanent (reviewer, teacher) unlock record, enforced by `uq_reviewer_unlock`. Unchanged.

### `review_votes`
One (voter, review) vote, enforced by `uq_voter_review`. Unchanged schema; behavior fix only
(Decision 8 in `research.md` — validate `voter_token` before insert instead of letting a null
hit the `NOT NULL` constraint uncaught).

### `admin_users`
Admin credentials. Unchanged schema; the seeded row's `password_hash` value is corrected via
data migration (Decision 4), not a structural change. Note: `role` column already exists with
values `ADMIN`/`MODERATOR`, but per clarification the application enforces a single uniform
permission level in v1 — the column is left in place (harmless, avoids a destructive schema
change for a v2-later possibility) but no code path differentiates behavior by its value.

## Schema Change Required: `review_reports`

### Current shape
```
review_reports(id, review_id, reason, description, status, created_at)
```
No column identifies who filed the report — FR-016 requires a reviewer identity to be
attributable to each report (Decision 7 in `research.md`).

### New column
| Column | Type | Constraint | Notes |
|---|---|---|---|
| `reporter_token` | `VARCHAR(50)` | `NOT NULL` | The reviewer identity that filed the report; same convention as `reviews.reviewer_token` / `review_votes.voter_token` (no DB-level FK, matching the existing lazy-reviewer-creation pattern) |

## Migration `V2__fix_reports_admin_and_indexes.sql` (to be created during implementation)

Additive-only, three changes bundled into one migration since they're all pre-launch
correctness fixes discovered together:

1. `ALTER TABLE review_reports ADD COLUMN reporter_token VARCHAR(50) NOT NULL DEFAULT '';`
   followed by dropping the default once existing rows (if any, dev-only) are backfilled or
   truncated — this is pre-launch dev data, so a simple `DEFAULT ''` → drop-default is
   sufficient; no production backfill concern.
2. `CREATE INDEX idx_reviews_iphash_created ON reviews(ip_hash, created_at);` — supports the
   new FR-017 rate-limit query.
3. `UPDATE admin_users SET password_hash = '<newly generated BCrypt hash of admin123>' WHERE username = 'admin';`
   — corrects Decision 4's malformed seed hash. The literal hash value is generated at
   implementation time via `BCryptPasswordEncoder` (deterministic salt is *not* required — any
   valid BCrypt hash of `admin123` works, since login validates via `matches()`, not equality).

No other table is touched. `ddl-auto: validate` stays satisfied since the JPA entity for
`ReviewReport` gains one matching new field.

## Entity Relationship (unchanged shape, for reference)

```
teachers 1───* reviews *───1 reviewers
teachers 1───* teacher_subjects *───1 subjects
teachers 1───* unlocked_teachers *───1 reviewers
reviews  1───* review_votes *───1 reviewers (as voter)
reviews  1───* review_reports *───1 reviewers (as reporter)   [new FK-by-convention]
```
