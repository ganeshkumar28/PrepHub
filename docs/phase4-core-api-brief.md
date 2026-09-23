# Phase 4 — Core API brief

Everything here is already fully specified in `docs/openapi.yaml`; this brief is the implementation notes Antigravity needs beyond the contract itself.

## `POST /extractions/{jobId}/publish`

This is the "confirm" step in the preview/edit/publish flow. Takes the (possibly user-edited) `ExperienceInput` body, not just re-reads the job's stored result — the user may have corrected something in the preview screen.

- Verify the job belongs to the calling user (`extraction_jobs.user_id` matches the JWT's `sub`) and is `SUCCEEDED` — else `409`
- Verify the job hasn't already been published (an `experiences` row with this `extraction_job_id` doesn't already exist) — else `409`
- Create the `experiences` row with `status: 'PUBLISHED'` directly (no `DRAFT` intermediate state needed in v1, since the draft *is* the unpublished extraction job)
- Create `interview_rounds` and `questions` rows from the input, resolving `topicSlugs` to `question_topics` rows
- Company resolution here uses the same trigram-match-or-create logic from Phase 3

## `GET /experiences` and `GET /questions` (browse/search)

- `q` param → `plainto_tsquery('english', :q)` against the existing `search_tsv` generated columns — no need to build search logic, Postgres does it
- `company`/`topic` filters → straightforward joins, filter by slug
- Always filter `where status = 'PUBLISHED'` for the public list endpoints — unpublished/draft experiences should never appear here regardless of who's asking (there's no draft state to leak in v1, but keep the filter anyway as a safety net for when drafts are added later)
- Pagination: standard Spring Data `Pageable`, map to the `PageMeta` schema (`page`, `size`, `totalElements`, `totalPages`)

## `PUT /experiences/{id}` and `DELETE /experiences/{id}`

- Author-only: compare `experiences.author_id` to the JWT's `sub`, return `403` on mismatch — don't just filter these out of query results, actively reject
- `PUT` replaces rounds/questions wholesale (delete existing rounds for the experience, re-create from the request body) rather than trying to diff and patch individual rounds — simpler, and matches how the frontend's edit flow will naturally work

## `GET /topics` and `GET /companies`

- `/topics` — simple list, optionally filtered by `kind`, no auth required
- `/companies?q=` — trigram search (`similarity(name, :q) > 0.2 order by similarity(name, :q) desc limit 10`) for autocomplete as the user types a company name; also no auth required

## Error handling

Implement a single `@ControllerAdvice` that maps exceptions to `Problem` (RFC 9457) responses — validation failures to `400`, missing entities to `404`, ownership mismatches to `403`, the job-state conflicts above to `409`. One handler, not scattered try/catches per controller.

## What "done" looks like for this phase

- Publish a real extraction end-to-end and immediately see it via `GET /experiences`
- Search (`?q=`) returns relevant results for a word that actually appears in a published experience's summary or a question's text
- Attempting to `PUT`/`DELETE` someone else's experience (test with two different Supabase users) returns `403`, not `404` or a silent no-op
- Every error case returns a proper `application/problem+json` body, not a raw stack trace or Spring's default error page
