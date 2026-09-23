# Phase 5 — Frontend brief

## Setup

- Next.js (App Router), TypeScript, Tailwind
- `@supabase/supabase-js` and `@supabase/ssr` for auth
- Env vars: `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`, `NEXT_PUBLIC_API_BASE_URL` (points at Cloud Run once deployed; `http://localhost:8080/api/v1` for now)
- create .env.local_template in frontend/ and add the env variables which are required for this project. The user will copy the template content into .env.local and add the scrects and keys into them

## Pages

| Route | Auth | Purpose |
|---|---|---|
| `/` | Public | Browse/search feed — list of published experiences |
| `/experiences/[id]` | Public | One experience, its rounds and questions |
| `/questions` | Public | Browse/search questions directly, filtered by company/topic |
| `/login` | Public | Supabase Auth (magic link or Google OAuth via Supabase — your call, both are free) |
| `/submit` | Login required | Paste raw text |
| `/submit/[jobId]` | Login required | Preview/edit extraction result, publish |
| `/me` | Login required | List of the user's own experiences (edit/delete) |

## Auth wiring

Use Supabase's SSR helpers so the session cookie is available on both server and client components. Every authenticated API call attaches the Supabase session's `access_token` as `Authorization: Bearer <token>` — that's the JWT your Spring Boot `SecurityConfig` validates. No custom token handling needed; Supabase's client SDK manages refresh automatically.

## The core flow: `/submit` → `/submit/[jobId]`

1. `/submit`: a single textarea (min ~100 chars, matching `CreateExtractionRequest`), submit button calls `POST /extractions`, redirect to `/submit/{jobId}` with the returned job id
2. `/submit/[jobId]`: since extraction is synchronous server-side (Phase 3), `GET /extractions/{jobId}` on page load should already return `SUCCEEDED`/`FAILED` — no polling loop needed for v1, though structure the fetch so adding one later (if extraction ever becomes truly async) is a small change, not a rewrite
3. On `SUCCEEDED`: render the extracted `ExperienceInput` as an **editable form**, not read-only text — every field from company name down to individual question text should be editable before publish, since this editable-preview *is* your quality control mechanism (no moderator queue, remember)
4. "Confirm & Publish" button calls `POST /extractions/{jobId}/publish` with the (possibly edited) form values, redirect to the newly created `/experiences/{id}`
5. On `FAILED` or `isInterviewContent: false`: show a clear message ("This doesn't look like an interview experience — try pasting the actual Q&A text") with a way to go back and try again, not a dead end

## Browse/search (`/` and `/questions`)

- Search box wired to the `q` query param, debounced (~300ms) rather than firing on every keystroke
- Filter controls for company (autocomplete via `GET /companies?q=`) and topic (multi-select from `GET /topics`, grouped by `kind`)
- Paginate using the `PageMeta` response — infinite scroll is nicer UX than page numbers for a feed like this, but plain "Load more" is far less frontend complexity for v1

## What "done" looks like for this phase

- Full loop works in the browser: log in → paste real text → see the extraction populate an editable form → publish → see it appear on the home feed
- Logged out, you can still browse and search everything, but hitting `/submit` redirects you to `/login`
- Editing a field in the preview before publishing actually changes what gets saved (test this specifically — it's the one step most likely to get wired read-only by mistake)
