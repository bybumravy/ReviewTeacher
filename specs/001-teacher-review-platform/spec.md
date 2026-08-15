# Feature Specification: Anonymous Teacher Review Platform (Credit-Gated Access)

**Feature Branch**: `001-teacher-review-platform`

**Created**: 2026-08-14

**Status**: Draft

**Input**: User description: "UniReview — a fully anonymous, no-account platform where university students share candid reviews of their teachers (teaching quality, grading, exams). Access to detailed reviews is gated by a credit system (Glassdoor-style): a student earns 1 credit by contributing an AI-moderated, quality review, and spends 1 credit to unlock another teacher's detailed reviews. Reviews are auto-moderated by a two-layer filter (banned-word/pattern filter, then AI toxicity scoring) so most reviews publish instantly; borderline content is queued for admin review. The platform includes anti-spam protections (bot verification, per-IP rate limits, duplicate-review prevention) and an admin console for moderating flagged content and managing the teacher roster." (see `project_specification.md` and `unireview-backend/README.md`)

## Clarifications

### Session 2026-08-14

- Q: Does the platform need two separate admin permission tiers (e.g., full ADMIN vs a limited MODERATOR), or is one uniform admin role enough for v1? → A: One uniform admin role — any authenticated admin can moderate reviews and manage the teacher roster.
- Q: Can a student edit or delete their own review after they've submitted it? → A: No — reviews are immutable after submission; only admins can hide/remove content.
- Q: When an admin re-imports the teacher roster and a record matches an existing teacher, what should happen to the existing record? → A: Update in place (upsert) — the existing teacher's details are updated, preserving their rating history and reviews.
- Q: Does the student-facing platform need to support English as well as Vietnamese in v1, or is Vietnamese-only sufficient? → A: Vietnamese-only for v1.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Contribute a Review and Earn Access (Priority: P1)

A student who just finished a course wants to share their honest experience with a teacher. They write a structured review (ratings + written feedback) without creating an account. If the review passes automated content checks, it publishes immediately, the student earns 1 credit, and they automatically gain permanent access to that same teacher's detailed reviews at no extra cost.

**Why this priority**: This is the core value exchange that funds the entire platform — without a steady stream of contributed reviews, there is nothing to unlock and no reason to return. It must work standalone before anything else matters.

**Independent Test**: Can be fully tested by submitting a complete, policy-compliant review for a teacher with no prior reviews from this student, and confirming the review appears publicly, the student's credit balance increases by 1, and the reviewed teacher's detailed reviews are immediately viewable by that student without spending a credit.

**Acceptance Scenarios**:

1. **Given** a student has never reviewed Teacher A before, **When** they submit a complete review that contains no policy violations, **Then** the review is published, the student's credit balance increases by 1, and the student can immediately view Teacher A's detailed reviews without spending a credit.
2. **Given** a student submits a review containing banned language, a phone number, an email address, or a promotional link, **When** the review is submitted, **Then** the submission is rejected immediately with a clear reason and nothing is published or credited.
3. **Given** a student submits a review that passes the instant filters but is flagged as borderline by the toxicity check, **When** the review is submitted, **Then** the review is held out of public view pending admin decision and no credit is granted yet.
4. **Given** a student has already reviewed Teacher A, **When** they attempt to submit a second review for Teacher A using the same identity, **Then** the submission is rejected as a duplicate.
5. **Given** the automated toxicity check is temporarily unavailable, **When** a student submits a review that passed the instant filters, **Then** the review is published and credited so the student's experience is not interrupted, and it remains eligible for later admin review via user reports.

---

### User Story 2 - Unlock a Teacher's Detailed Reviews Using Credit (Priority: P1)

A student wants to read the detailed, written reviews for a teacher they have not reviewed themselves. If they have at least 1 credit, viewing that teacher's reviews automatically spends 1 credit and grants permanent access to that teacher going forward. If they have no credit, they're prompted to contribute a review of a different teacher to earn one.

**Why this priority**: This is the other half of the core loop and the primary reason students return to the platform — without it, credits earned in Story 1 have no purpose.

**Independent Test**: Can be fully tested by having a student identity with at least 1 credit open a teacher's detailed reviews for the first time and confirming the credit balance decreases by 1, the teacher becomes permanently unlocked for that identity, and a second visit does not spend another credit.

**Acceptance Scenarios**:

1. **Given** a student has 1 or more credits and has not unlocked Teacher B, **When** they open Teacher B's detailed reviews, **Then** 1 credit is deducted, Teacher B becomes permanently unlocked for that student, and the detailed reviews are shown.
2. **Given** a student has already unlocked Teacher B (by prior credit spend or by having reviewed Teacher B themselves), **When** they open Teacher B's detailed reviews again, **Then** the reviews are shown with no additional credit deducted.
3. **Given** a student has 0 credits and has not unlocked Teacher B, **When** they attempt to open Teacher B's detailed reviews, **Then** access is denied and they are prompted to contribute a review of another teacher to earn a credit.
4. **Given** a visitor has no anonymous identity yet, **When** they attempt to open any teacher's detailed reviews, **Then** access is denied with guidance that contributing a review is required first.

---

### User Story 3 - Browse and Search the Teacher Directory (Priority: P2)

A student wants to find a teacher by name, department, or subject, filter by minimum rating, and sort results, without needing any credit or prior contribution. Summary rating information (overall score, review count, structured statistics) is always visible to everyone.

**Why this priority**: This is the entry point that lets any visitor — even one with zero credits — discover value in the platform and decide whether to invest the effort of writing a review.

**Independent Test**: Can be fully tested by searching/filtering/sorting the teacher directory as a first-time anonymous visitor and confirming results and summary statistics display correctly with no credit or identity required.

**Acceptance Scenarios**:

1. **Given** the teacher directory contains multiple teachers, **When** a visitor searches by name or department keyword, **Then** matching teachers are returned.
2. **Given** a visitor filters by faculty and a minimum rating, **When** the filter is applied, **Then** only teachers meeting both criteria are shown.
3. **Given** a visitor sorts the directory by rating or review count, **When** the sort is applied, **Then** results are ordered accordingly.
4. **Given** a visitor opens any teacher's profile, **When** the page loads, **Then** the teacher's summary rating and aggregate structured statistics are visible without spending a credit or having an identity.

---

### User Story 4 - React to and Flag Reviews (Priority: P3)

A student reading a teacher's reviews wants to upvote or downvote a review as helpful/unhelpful, and can report a review that seems abusive, fake, or otherwise inappropriate so admins can investigate.

**Why this priority**: Improves content quality and trust over time but the platform delivers its core value (Stories 1-3) without it.

**Independent Test**: Can be fully tested by having a student identity vote once on a review and confirming the vote count updates and a second vote attempt from the same identity on the same review does not double-count; and by submitting a report with a reason and confirming it reaches the admin queue.

**Acceptance Scenarios**:

1. **Given** a student identity has not voted on a review, **When** they upvote or downvote it, **Then** the corresponding count increases by 1.
2. **Given** a student identity has already voted on a review, **When** they attempt to vote on it again, **Then** the duplicate vote is not counted.
3. **Given** a student finds a review inappropriate, **When** they submit a report with a reason, **Then** the report is recorded and made visible to admins for review.

---

### User Story 5 - Moderate Flagged Reviews (Priority: P3)

An admin needs to review the queue of reviews held for manual moderation (either flagged by the toxicity check or reported by students) and decide to approve or reject each one, so borderline content doesn't stay in limbo and clearly bad content doesn't slip through.

**Why this priority**: Necessary for platform trust and legal/reputational safety, but the volume is expected to be a small fraction of total submissions since most reviews auto-publish.

**Independent Test**: Can be fully tested by an authenticated admin viewing the flagged queue, approving one item and rejecting another, and confirming the approved review becomes publicly visible and credits its author while the rejected one stays hidden and grants no credit.

**Acceptance Scenarios**:

1. **Given** a review is in the flagged queue, **When** an admin approves it, **Then** the review becomes publicly visible and its author's credit balance increases by 1.
2. **Given** a review is in the flagged queue, **When** an admin rejects it, **Then** the review remains hidden from the public and no credit is granted.
3. **Given** a previously published review is later found to violate policy, **When** an admin hides/removes it, **Then** it is no longer publicly visible and 1 credit is deducted from its author's balance (if their balance is positive).
4. **Given** an unauthenticated user, **When** they attempt to access the moderation queue, **Then** access is denied.

---

### User Story 6 - Maintain the Teacher Roster (Priority: P3)

An admin needs to bulk-import or update the list of teachers (name, title, faculty, department) so the directory stays current each semester without manual one-by-one entry.

**Why this priority**: Operational necessity for keeping data current, but infrequent (semester-scale cadence) and not part of the moment-to-moment student experience.

**Independent Test**: Can be fully tested by an authenticated admin importing a batch of teacher records and confirming they appear correctly in the public directory.

**Acceptance Scenarios**:

1. **Given** an admin has a properly formatted batch of teacher records, **When** they import it, **Then** the teachers appear in the public directory with correct details.
2. **Given** a batch import contains invalid or malformed records, **When** it is submitted, **Then** the admin is shown which records failed and why, without silently dropping data.
3. **Given** an imported record matches a teacher that already exists in the roster, **When** the import runs, **Then** the existing teacher's details are updated in place (upsert) and that teacher's existing ratings and reviews are preserved unchanged.

---

### Edge Cases

- What happens when a student clears their browser storage/cookies? They are treated as a brand-new anonymous identity and lose their prior credit balance and unlock history (see Assumptions).
- What happens when a student exceeds the daily review submission limit for their network address? Further submissions are rejected until the limit resets, to deter spam.
- How does the system handle a review submitted without passing bot/abuse verification? The submission is rejected before any content processing occurs.
- What happens if an admin rejects a review whose author already had it auto-published under fail-open behavior? The review is taken down retroactively and the credit clawback rule applies.
- How does the system prevent a student from unlocking a teacher they already have free access to (via authoring a review) by also spending a credit? Existing free access is checked first, before any credit is spent.
- What happens when a teacher has zero reviews yet? The directory and profile show the teacher with a neutral/empty rating state rather than an error.
- What happens when a student tries to change or take down a review they already submitted? There is no student-facing edit or delete action; reviews are immutable once submitted (see FR-025). A student who wants content removed must report it or the platform relies on admin action.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST let any visitor browse, search (by name/department/subject keyword), filter (by faculty and minimum rating), sort, and paginate the teacher directory without creating an account or spending credit.
- **FR-002**: The system MUST show every teacher's summary rating, review count, and aggregate structured statistics (e.g., difficulty, workload, attendance patterns) publicly, without requiring credit.
- **FR-003**: The system MUST let a visitor contribute a written review for a teacher without registering an account, establishing a persistent anonymous identity for that visitor the first time they contribute.
- **FR-004**: The system MUST require each review to include structured ratings and a minimum amount of written feedback, and MUST reject submissions that omit required fields.
- **FR-005**: The system MUST reject, before publishing or storing, any review content containing banned/profane language, phone numbers, email addresses, or promotional links, and MUST tell the submitter why it was rejected.
- **FR-006**: The system MUST run every review that passes the instant content filter through an automated toxicity/quality check.
- **FR-007**: The system MUST auto-publish a review and award its author 1 credit when the automated check scores it as acceptable.
- **FR-008**: The system MUST hold a review out of public view and withhold credit when the automated check scores it as borderline/toxic, and MUST place it in an admin moderation queue.
- **FR-009**: The system MUST auto-publish (fail open) and credit a review when the automated toxicity check is unavailable or times out, so the contributor's experience is not blocked, while keeping it eligible for later moderation via user reports.
- **FR-010**: The system MUST prevent the same anonymous identity from submitting more than one review for the same teacher.
- **FR-011**: The system MUST automatically grant an identity permanent, free access to the detailed reviews of any teacher that identity has successfully reviewed.
- **FR-012**: The system MUST allow an identity with at least 1 credit to unlock a teacher's detailed reviews, automatically deducting 1 credit and recording permanent access for that identity/teacher pair the first time.
- **FR-013**: The system MUST NOT deduct additional credit when an identity re-visits a teacher it has already unlocked or reviewed.
- **FR-014**: The system MUST deny access to a teacher's detailed reviews, with a clear reason, when the requesting identity has 0 credit and has not previously unlocked or reviewed that teacher.
- **FR-015**: The system MUST let an identity cast one upvote or downvote per review, and MUST prevent the same identity from voting more than once on the same review.
- **FR-016**: The system MUST let an identity report a review with a reason and description, and make that report visible to admins.
- **FR-017**: The system MUST limit the number of reviews a single network address can submit within a day, rejecting submissions beyond that limit.
- **FR-018**: The system MUST verify that write actions (submitting a review, voting, reporting) originate from a legitimate visitor and not automated abuse, rejecting the action if verification fails.
- **FR-019**: The system MUST NOT collect or display any personally identifying information about reviewers; any network-address data retained for anti-abuse purposes MUST be stored in a form that is not reversible to the original address.
- **FR-020**: The system MUST provide a restricted admin area, accessible only to authenticated admins, for managing content and the teacher roster. All admin accounts share the same single permission level in v1 — there is no separate limited/moderator-only tier.
- **FR-021**: The system MUST let an authenticated admin view the moderation queue and approve (publish + credit the author) or reject (keep hidden, no credit) each flagged review.
- **FR-022**: The system MUST let an authenticated admin hide/remove a previously published review, and MUST deduct 1 credit from its author's balance when doing so, provided the author's balance is positive.
- **FR-023**: The system MUST let an authenticated admin bulk-import teacher roster records and MUST report which records, if any, failed to import and why. When an imported record matches an existing teacher, the system MUST update that teacher's details in place (upsert) rather than creating a duplicate, and MUST preserve the existing teacher's ratings and reviews.
- **FR-024**: The system MUST recompute a teacher's summary rating and review count whenever a review affecting that teacher is published, hidden, or removed.
- **FR-025**: The system MUST NOT allow a student to edit or delete their own review after submission; a published or held review can only be changed in status (hidden/approved/rejected) by an admin.
- **FR-026**: The system's student-facing content (UI text and content-moderation word lists) MUST support Vietnamese only in v1; multi-language support is out of scope.

### Key Entities

- **Teacher**: A faculty member profile (name, title, faculty/department, avatar) with aggregate rating and review-count statistics; maintained by admins, browsable/searchable by everyone.
- **Reviewer Identity**: A persistent, anonymous, device-scoped identity created on first contribution; tracks credit balance and review count. Carries no personal information.
- **Review**: A single submission tied to one reviewer identity and one teacher; contains structured ratings, qualitative feedback, contextual details (semester, difficulty, workload, etc.), a moderation status (published, held for review, rejected, hidden), and vote counts.
- **Unlock Record**: Records that a given reviewer identity has permanent access to a given teacher's detailed reviews, whether earned by authorship or purchased with credit.
- **Vote**: One reviewer identity's up/down reaction to a specific review; at most one per identity per review.
- **Report**: A flag raised by a reviewer identity against a review, with a reason and description, awaiting admin action.
- **Admin Account**: A privileged, authenticated user who moderates content and manages the teacher roster. All admin accounts have the same single permission level in v1 (no tiered roles).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A first-time visitor can find and open a specific teacher's profile using search or filters in under 1 minute, with no account creation required.
- **SC-002**: A student who submits a policy-compliant review sees it published and their credit balance updated within a few seconds, with no manual step in between.
- **SC-003**: At least 95% of submitted reviews that contain no policy violations are auto-published without requiring any admin action.
- **SC-004**: A student can go from zero credits to viewing a new teacher's detailed reviews within a single visit, requiring only one qualifying review submission in between.
- **SC-005**: Fewer than 1% of published reviews are duplicate submissions (same identity, same teacher).
- **SC-006**: Admins can find, decide on, and clear any individual item in the flagged-review queue in under 2 minutes per item once in the admin console.
- **SC-007**: No visitor is ever required to create an account, provide an email address, or provide any personal information to browse, contribute, or unlock reviews.

## Assumptions

- An anonymous identity is scoped to the browser/device that created it; if a student clears their browser's storage or switches devices, they are treated as a new identity and forfeit their prior credit balance and unlock history. There is no cross-device recovery mechanism in this scope, since supporting one would require collecting identifying information that conflicts with the anonymity goal.
- Approval and auto-unlock happen as a single atomic outcome of a successful review submission — the student is not asked to take a separate confirming action.
- The teacher roster is created and maintained by admins (manual entry or bulk import); students cannot add new teachers to the platform.
- The specific numeric defaults already defined in the project's technical documentation (1 credit earned per approved review, 1 credit spent per unlock, 3 review submissions per network address per day, borderline-toxicity threshold, minimum review length) are treated as fixed business rules for this feature, not open implementation choices.
- Teachers themselves do not have accounts and cannot respond to or dispute reviews in this scope; that capability is out of scope for v1.
- There is no student-facing appeal process for a rejected or hidden review in this scope; disputes are handled by admins acting on reports.
- All admin accounts operate at a single permission level in v1; role-based admin permission tiers are out of scope until a need is demonstrated.
- The platform targets a single Vietnamese university and ships Vietnamese-only in v1; English/bilingual support is out of scope until explicitly prioritized.
