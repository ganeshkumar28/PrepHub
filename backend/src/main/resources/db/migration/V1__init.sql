-- PrepHub v1 schema (Supabase Postgres)
-- Scope: paste -> AI extract -> user preview/edit -> publish -> public browse & search.
-- Deliberately deferred to a later migration: moderation/reports, bookmarks, vector search & question dedup.
-- Auth is handled entirely by Supabase Auth (auth.users) -- no custom users/JWT table here.

create extension if not exists pg_trgm;

-- ---------------------------------------------------------------------------
-- Profiles: extends Supabase's built-in auth.users, never duplicates auth data
-- ---------------------------------------------------------------------------
create table profiles (
    id           uuid primary key references auth.users(id) on delete cascade,
    display_name text not null,
    created_at   timestamptz not null default now()
);

-- ---------------------------------------------------------------------------
-- Reference data
-- ---------------------------------------------------------------------------
create table companies (
    id         uuid primary key default gen_random_uuid(),
    name       text not null,
    slug       text not null unique,
    aliases    text[] not null default '{}',
    created_at timestamptz not null default now()
);
create index ix_companies_name_trgm on companies using gin (name gin_trgm_ops);
create index ix_companies_aliases on companies using gin (aliases);

create table topics (
    id         uuid primary key default gen_random_uuid(),
    name       text not null,
    slug       text not null unique,
    kind       text not null
               check (kind in ('LANGUAGE', 'FRAMEWORK', 'CONCEPT', 'DATABASE',
                                'SYSTEM_DESIGN', 'DSA', 'DEVOPS', 'BEHAVIORAL', 'OTHER')),
    created_at timestamptz not null default now()
);
create index ix_topics_kind on topics (kind);

-- ---------------------------------------------------------------------------
-- Extraction jobs: async AI work + basic usage telemetry
-- (token counts are cheap to store and let you watch Gemini free-tier burn rate)
-- ---------------------------------------------------------------------------
create table extraction_jobs (
    id            uuid primary key default gen_random_uuid(),
    user_id       uuid        not null references profiles(id) on delete cascade,
    raw_text      text        not null,
    status        text        not null default 'QUEUED'
                  check (status in ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    result        jsonb,
    error_message text,
    input_tokens  integer,
    output_tokens integer,
    created_at    timestamptz not null default now(),
    completed_at  timestamptz
);
create index ix_extraction_jobs_user on extraction_jobs (user_id, created_at desc);

-- ---------------------------------------------------------------------------
-- Experiences, rounds, questions
-- ---------------------------------------------------------------------------
create table experiences (
    id                  uuid primary key default gen_random_uuid(),
    author_id           uuid        not null references profiles(id) on delete cascade,
    is_anonymous        boolean     not null default false,
    extraction_job_id   uuid        references extraction_jobs(id) on delete set null,
    company_id          uuid        references companies(id),
    company_name_raw    text,
    role_title          text,
    level               text        check (level in ('INTERN', 'JUNIOR', 'MID', 'SENIOR', 'LEAD', 'PRINCIPAL')),
    years_of_experience numeric(3, 1) check (years_of_experience >= 0),
    location            text,
    interview_year      smallint    check (interview_year between 2000 and 2100),
    interview_month     smallint    check (interview_month between 1 and 12),
    interview_mode      text        check (interview_mode in ('ONSITE', 'REMOTE', 'HYBRID')),
    outcome             text        not null default 'UNKNOWN'
                        check (outcome in ('SELECTED', 'REJECTED', 'PENDING', 'UNKNOWN')),
    summary             text,
    status              text        not null default 'DRAFT'
                        check (status in ('DRAFT', 'PUBLISHED')),
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now(),
    search_tsv          tsvector generated always as (
        to_tsvector('english', coalesce(role_title, '') || ' ' || coalesce(summary, ''))
    ) stored
);
create index ix_experiences_feed on experiences (status, created_at desc);
create index ix_experiences_company on experiences (company_id);
create index ix_experiences_author on experiences (author_id);
create index ix_experiences_year on experiences (interview_year);
create index ix_experiences_search on experiences using gin (search_tsv);

create table interview_rounds (
    id            uuid primary key default gen_random_uuid(),
    experience_id uuid     not null references experiences(id) on delete cascade,
    round_number  smallint not null check (round_number > 0),
    round_type    text     not null default 'OTHER'
                  check (round_type in ('ONLINE_ASSESSMENT', 'TECHNICAL', 'MACHINE_CODING',
                                        'SYSTEM_DESIGN', 'MANAGERIAL', 'HR', 'BEHAVIORAL', 'OTHER')),
    duration_minutes smallint check (duration_minutes > 0),
    notes         text,
    unique (experience_id, round_number)
);

create table questions (
    id            uuid primary key default gen_random_uuid(),
    experience_id uuid        not null references experiences(id) on delete cascade,
    round_id      uuid        references interview_rounds(id) on delete set null,
    text          text        not null,
    question_type text        not null default 'OTHER'
                  check (question_type in ('THEORY', 'CODING', 'SYSTEM_DESIGN',
                                           'BEHAVIORAL', 'SCENARIO', 'OTHER')),
    difficulty    text        check (difficulty in ('EASY', 'MEDIUM', 'HARD')),
    search_tsv    tsvector generated always as (to_tsvector('english', text)) stored,
    created_at    timestamptz not null default now()
);
create index ix_questions_experience on questions (experience_id);
create index ix_questions_search on questions using gin (search_tsv);

create table question_topics (
    question_id uuid    not null references questions(id) on delete cascade,
    topic_id    uuid    not null references topics(id),
    is_primary  boolean not null default false,
    primary key (question_id, topic_id)
);
create index ix_question_topics_topic on question_topics (topic_id);

-- ---------------------------------------------------------------------------
-- Deferred to a later migration (do not build in v1):
--   question_clusters + pgvector embeddings + semantic dedup   -> v1.1
--   bookmarks                                                   -> v2
--   reports + admin moderation queue + role-based access        -> v2
-- ---------------------------------------------------------------------------
