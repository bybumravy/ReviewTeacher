# Quickstart: Manual End-to-End Validation

Prerequisites: Docker (for PostgreSQL), Java 17, Node.js. Automated tests (added per this plan,
see `research.md` §10) cover the core logic; this guide validates the full stack end-to-end
manually, the way a real student/admin would use it.

## 1. Start the stack

```bash
cd unireview-backend
docker-compose up -d                 # PostgreSQL on :5432
$env:JAVA_HOME = "<your JDK 17 path>"  # if not already set globally
.\mvnw.cmd spring-boot:run             # backend on :8080, Flyway auto-migrates
```

```bash
cd unireview
npm run dev                           # frontend on :5173
```

Swagger UI for manually exercising the API directly: `http://localhost:8080/swagger-ui.html`.

## 2. US1 — Contribute a review and earn access

1. Open `http://localhost:5173`, navigate to a teacher with no prior review from you, and
   submit a complete review via the UI (all fields, ≥50 chars content).
2. **Expected**: review appears published; a credit indicator increases by 1; you can
   immediately open that same teacher's detailed reviews.
3. Try submitting a second review for the same teacher → **expected**: rejected as duplicate.
4. Try content containing a banned word / phone number / email / link → **expected**: rejected
   immediately with a clear reason, nothing published.

## 3. US2 — Unlock a teacher's reviews with credit

1. With at least 1 credit (from step 2), open a *different* teacher's detailed reviews for the
   first time.
2. **Expected**: credit balance decreases by 1, that teacher becomes permanently unlocked,
   detailed reviews are shown.
3. Revisit the same teacher → **expected**: no further credit spent.
4. With 0 credit and an un-unlocked teacher → **expected**: access denied, prompted to write a
   review elsewhere.

## 4. US3 — Browse and search

1. As a fresh/incognito visitor (no prior identity), search/filter/sort the teacher directory.
2. **Expected**: results and every teacher's summary rating/stats are visible with no identity
   and no credit required.

## 5. US4 — Vote and report

1. On a published review, click upvote/downvote once → count updates. Click again → no
   double-count.
2. Submit a report with a reason → **expected**: succeeds (once identity is required per this
   plan's change) and appears in the admin flagged/report queue.

## 6. US5 — Admin moderation

1. Log in at the new admin login page with `admin` / `admin123`.
2. Open the flagged-review queue; approve one item, reject another.
3. **Expected**: approved item becomes public and its author's credit increases by 1; rejected
   item stays hidden, no credit granted.
4. Hide a previously-published review (new capability from this plan) → **expected**: it
   disappears from public view and, if the author's balance was positive, their credit
   decreases by 1; the teacher's average rating recomputes.

## 7. US6 — Roster import

1. On the new admin CSV-import page, upload a batch containing: one brand-new teacher, one
   teacher matching an existing one exactly on (full name, faculty), and one malformed row
   (missing full name).
2. **Expected**: new teacher created, existing teacher's details updated in place with its
   ratings/reviews intact, malformed row reported back by row number and reason — nothing
   silently dropped.

## 8. Cross-cutting checks

- **Rate limiting**: submit 4 reviews from the same browser/network within a day (4th requires
  a 4th distinct un-reviewed teacher) → 4th is rejected with a rate-limit error.
- **Privacy**: inspect the `reviewers`/`reviews` rows in the DB directly
  (`docker exec -it <container> psql -U postgres -d unireview_db -c "select ip_hash from reviews limit 5;"`)
  → values must be 64-character hex hashes, never a plain IP.
- **Admin auth**: confirm `/api/admin/reviews/flagged` returns `401`/`403` with no token, and
  `200` with a valid token from step 6.1 — validates the filter-registration fix.
- **Build gate**: `cd unireview-backend && .\mvnw.cmd clean package` and
  `cd unireview && npm run build` both succeed with zero errors before calling any task done
  (Constitution Principle VI).
