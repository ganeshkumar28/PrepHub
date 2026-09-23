'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { api } from '@/lib/api';
import type {
  CompanyDto,
  ExperienceDto,
  Level,
  Outcome,
  PageMetaDto,
  TopicDto,
  TopicKind,
} from '@/lib/types';
import {
  Briefcase,
  Building2,
  Calendar,
  CheckCircle2,
  ChevronDown,
  Clock,
  Filter,
  Layers,
  MapPin,
  RefreshCw,
  Search,
  Sparkles,
  User,
  X,
  XCircle,
} from 'lucide-react';

const LEVELS: Level[] = ['INTERN', 'JUNIOR', 'MID', 'SENIOR', 'LEAD', 'PRINCIPAL'];
const OUTCOMES: { value: Outcome; label: string }[] = [
  { value: 'SELECTED', label: 'Offer / Selected' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'UNKNOWN', label: 'Unknown / Other' },
];

export default function HomePage() {
  // Query & filter states
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  const [selectedCompany, setSelectedCompany] = useState<CompanyDto | null>(null);
  const [companySearch, setCompanySearch] = useState('');
  const [companySuggestions, setCompanySuggestions] = useState<CompanyDto[]>([]);
  const [showCompanyDropdown, setShowCompanyDropdown] = useState(false);

  const [selectedTopics, setSelectedTopics] = useState<TopicDto[]>([]);
  const [availableTopics, setAvailableTopics] = useState<TopicDto[]>([]);
  const [showTopicModal, setShowTopicModal] = useState(false);

  const [level, setLevel] = useState<Level | ''>('');
  const [outcome, setOutcome] = useState<Outcome | ''>('');
  const [year, setYear] = useState<string>('');
  const [sort, setSort] = useState<'newest' | 'oldest'>('newest');

  // Feed data state
  const [experiences, setExperiences] = useState<ExperienceDto[]>([]);
  const [pageMeta, setPageMeta] = useState<PageMetaDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const companyInputRef = useRef<HTMLDivElement>(null);

  // Debounce search term (~300ms)
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchTerm);
    }, 300);
    return () => clearTimeout(handler);
  }, [searchTerm]);

  // Load available topics on mount
  useEffect(() => {
    api
      .listTopics()
      .then((topics) => setAvailableTopics(topics))
      .catch((err) => console.error('Failed to load topics:', err));
  }, []);

  // Company autocomplete debounce
  useEffect(() => {
    if (!companySearch || companySearch.trim().length < 1) {
      setCompanySuggestions([]);
      return;
    }
    const handler = setTimeout(() => {
      api
        .searchCompanies(companySearch.trim())
        .then((res) => setCompanySuggestions(res))
        .catch(() => setCompanySuggestions([]));
    }, 200);
    return () => clearTimeout(handler);
  }, [companySearch]);

  // Close company dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (
        companyInputRef.current &&
        !companyInputRef.current.contains(e.target as Node)
      ) {
        setShowCompanyDropdown(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Fetch experiences
  const fetchExperiences = useCallback(
    async (pageToLoad: number, append = false) => {
      if (append) {
        setLoadingMore(true);
      } else {
        setLoading(true);
      }
      setError(null);

      try {
        const res = await api.listExperiences({
          page: pageToLoad,
          size: 15,
          q: debouncedSearch.trim() || undefined,
          company: selectedCompany?.slug || undefined,
          topic: selectedTopics.length > 0 ? selectedTopics.map((t) => t.slug) : undefined,
          level: (level as Level) || undefined,
          outcome: (outcome as Outcome) || undefined,
          year: year ? parseInt(year, 10) : undefined,
          sort,
        });

        if (append) {
          setExperiences((prev) => [...prev, ...res.content]);
        } else {
          setExperiences(res.content);
        }
        setPageMeta(res.page);
      } catch (err: unknown) {
        const errorMsg = err instanceof Error ? err.message : 'Failed to load experiences';
        setError(errorMsg);
      } finally {
        setLoading(false);
        setLoadingMore(false);
      }
    },
    [debouncedSearch, selectedCompany, selectedTopics, level, outcome, year, sort]
  );

  // Trigger search whenever filters change
  useEffect(() => {
    fetchExperiences(0, false);
  }, [fetchExperiences]);

  const handleLoadMore = () => {
    if (!pageMeta || loadingMore || pageMeta.page >= pageMeta.totalPages - 1) return;
    fetchExperiences(pageMeta.page + 1, true);
  };

  const handleClearFilters = () => {
    setSearchTerm('');
    setDebouncedSearch('');
    setSelectedCompany(null);
    setCompanySearch('');
    setSelectedTopics([]);
    setLevel('');
    setOutcome('');
    setYear('');
    setSort('newest');
  };

  const hasActiveFilters =
    debouncedSearch ||
    selectedCompany ||
    selectedTopics.length > 0 ||
    level ||
    outcome ||
    year ||
    sort !== 'newest';

  // Group topics by kind for the filter modal
  const topicsByKind = availableTopics.reduce((acc, topic) => {
    acc[topic.kind] = acc[topic.kind] || [];
    acc[topic.kind].push(topic);
    return acc;
  }, {} as Record<TopicKind, TopicDto[]>);

  const toggleTopic = (topic: TopicDto) => {
    if (selectedTopics.some((t) => t.slug === topic.slug)) {
      setSelectedTopics(selectedTopics.filter((t) => t.slug !== topic.slug));
    } else {
      setSelectedTopics([...selectedTopics, topic]);
    }
  };

  const getOutcomeBadge = (outcome: Outcome) => {
    switch (outcome) {
      case 'SELECTED':
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-semibold text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300">
            <CheckCircle2 className="h-3 w-3" /> Offer
          </span>
        );
      case 'REJECTED':
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-rose-100 px-2.5 py-0.5 text-xs font-semibold text-rose-800 dark:bg-rose-950/60 dark:text-rose-300">
            <XCircle className="h-3 w-3" /> Rejected
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-semibold text-amber-800 dark:bg-amber-950/60 dark:text-amber-300">
            {outcome}
          </span>
        );
    }
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      {/* Hero Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 sm:text-4xl dark:text-white">
          Real Tech Interview Experiences
        </h1>
        <p className="mt-2 text-base text-slate-600 dark:text-slate-400">
          Explore firsthand rounds, questions, and insights shared by verified software engineers.
        </p>
      </div>

      {/* Search and Filters Section */}
      <div className="mb-8 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        {/* Main Search Input */}
        <div className="relative">
          <Search className="pointer-events-none absolute left-3.5 top-3.5 h-5 w-5 text-slate-400" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search by keywords, questions, companies, topics (e.g. 'LRU Cache', 'Meta', 'System Design')..."
            className="w-full rounded-xl border border-slate-300 bg-slate-50 py-3 pl-11 pr-10 text-sm focus:border-indigo-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700 dark:bg-slate-800 dark:text-white dark:focus:bg-slate-900"
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm('')}
              className="absolute right-3.5 top-3.5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
            >
              <X className="h-5 w-5" />
            </button>
          )}
        </div>

        {/* Filter controls row */}
        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-6">
          {/* Company autocomplete */}
          <div className="relative lg:col-span-2" ref={companyInputRef}>
            <div className="relative">
              <Building2 className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
              <input
                type="text"
                value={selectedCompany ? selectedCompany.name : companySearch}
                onChange={(e) => {
                  setSelectedCompany(null);
                  setCompanySearch(e.target.value);
                  setShowCompanyDropdown(true);
                }}
                onFocus={() => setShowCompanyDropdown(true)}
                placeholder="Filter by company..."
                className="w-full rounded-lg border border-slate-300 bg-white py-2 pl-9 pr-8 text-xs focus:border-indigo-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-white"
              />
              {selectedCompany ? (
                <button
                  type="button"
                  onClick={() => {
                    setSelectedCompany(null);
                    setCompanySearch('');
                  }}
                  className="absolute right-2.5 top-2.5 text-slate-400 hover:text-slate-600"
                >
                  <X className="h-3.5 w-3.5" />
                </button>
              ) : (
                <ChevronDown className="pointer-events-none absolute right-2.5 top-2.5 h-4 w-4 text-slate-400" />
              )}
            </div>

            {/* Suggestions dropdown */}
            {showCompanyDropdown && companySuggestions.length > 0 && !selectedCompany && (
              <div className="absolute z-20 mt-1 max-h-48 w-full overflow-y-auto rounded-lg border border-slate-200 bg-white py-1 shadow-lg dark:border-slate-800 dark:bg-slate-900">
                {companySuggestions.map((c) => (
                  <button
                    key={c.slug}
                    type="button"
                    onClick={() => {
                      setSelectedCompany(c);
                      setCompanySearch(c.name);
                      setShowCompanyDropdown(false);
                    }}
                    className="flex w-full items-center px-3 py-2 text-left text-xs hover:bg-indigo-50 dark:hover:bg-indigo-950/50"
                  >
                    <Building2 className="mr-2 h-3.5 w-3.5 text-slate-400" />
                    <span className="font-medium text-slate-800 dark:text-slate-200">
                      {c.name}
                    </span>
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Topics trigger button */}
          <div>
            <button
              type="button"
              onClick={() => setShowTopicModal(true)}
              className="flex w-full items-center justify-between rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-750"
            >
              <div className="flex items-center gap-1.5 truncate">
                <Filter className="h-3.5 w-3.5 text-slate-400" />
                <span>
                  {selectedTopics.length === 0
                    ? 'Topics'
                    : `${selectedTopics.length} selected`}
                </span>
              </div>
              <ChevronDown className="h-3.5 w-3.5 text-slate-400 shrink-0" />
            </button>
          </div>

          {/* Level dropdown */}
          <div>
            <select
              value={level}
              onChange={(e) => setLevel(e.target.value as Level | '')}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs text-slate-700 focus:border-indigo-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200"
            >
              <option value="">All Levels</option>
              {LEVELS.map((lvl) => (
                <option key={lvl} value={lvl}>
                  {lvl}
                </option>
              ))}
            </select>
          </div>

          {/* Outcome dropdown */}
          <div>
            <select
              value={outcome}
              onChange={(e) => setOutcome(e.target.value as Outcome | '')}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs text-slate-700 focus:border-indigo-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200"
            >
              <option value="">All Outcomes</option>
              {OUTCOMES.map((oc) => (
                <option key={oc.value} value={oc.value}>
                  {oc.label}
                </option>
              ))}
            </select>
          </div>

          {/* Sort dropdown */}
          <div>
            <select
              value={sort}
              onChange={(e) => setSort(e.target.value as 'newest' | 'oldest')}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs text-slate-700 focus:border-indigo-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200"
            >
              <option value="newest">Newest First</option>
              <option value="oldest">Oldest First</option>
            </select>
          </div>
        </div>

        {/* Selected filters pills */}
        {hasActiveFilters && (
          <div className="mt-4 flex flex-wrap items-center gap-2 pt-3 border-t border-slate-100 dark:border-slate-800">
            <span className="text-xs text-slate-500">Active filters:</span>

            {selectedCompany && (
              <span className="inline-flex items-center gap-1 rounded-md bg-indigo-50 px-2 py-1 text-xs font-medium text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300">
                Company: {selectedCompany.name}
                <button
                  onClick={() => {
                    setSelectedCompany(null);
                    setCompanySearch('');
                  }}
                >
                  <X className="h-3 w-3" />
                </button>
              </span>
            )}

            {selectedTopics.map((topic) => (
              <span
                key={topic.slug}
                className="inline-flex items-center gap-1 rounded-md bg-indigo-50 px-2 py-1 text-xs font-medium text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300"
              >
                {topic.name}
                <button onClick={() => toggleTopic(topic)}>
                  <X className="h-3 w-3" />
                </button>
              </span>
            ))}

            {level && (
              <span className="inline-flex items-center gap-1 rounded-md bg-indigo-50 px-2 py-1 text-xs font-medium text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300">
                Level: {level}
                <button onClick={() => setLevel('')}>
                  <X className="h-3 w-3" />
                </button>
              </span>
            )}

            {outcome && (
              <span className="inline-flex items-center gap-1 rounded-md bg-indigo-50 px-2 py-1 text-xs font-medium text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300">
                Outcome: {outcome}
                <button onClick={() => setOutcome('')}>
                  <X className="h-3 w-3" />
                </button>
              </span>
            )}

            <button
              onClick={handleClearFilters}
              className="text-xs font-medium text-rose-600 hover:text-rose-500 ml-auto"
            >
              Clear all
            </button>
          </div>
        )}
      </div>

      {/* Topic selection modal */}
      {showTopicModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="max-h-[85vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white p-6 shadow-xl dark:bg-slate-900">
            <div className="flex items-center justify-between border-b border-slate-200 pb-4 dark:border-slate-800">
              <h3 className="text-lg font-bold text-slate-900 dark:text-white">
                Filter by Topics
              </h3>
              <button
                onClick={() => setShowTopicModal(false)}
                className="rounded-lg p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="mt-4 space-y-6">
              {Object.entries(topicsByKind).map(([kind, topics]) => (
                <div key={kind}>
                  <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                    {kind.replace(/_/g, ' ')}
                  </h4>
                  <div className="mt-2 flex flex-wrap gap-1.5">
                    {topics.map((t) => {
                      const isSelected = selectedTopics.some(
                        (st) => st.slug === t.slug
                      );
                      return (
                        <button
                          key={t.slug}
                          type="button"
                          onClick={() => toggleTopic(t)}
                          className={`rounded-lg px-2.5 py-1 text-xs font-medium transition-colors ${
                            isSelected
                              ? 'bg-indigo-600 text-white'
                              : 'bg-slate-100 text-slate-700 hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700'
                          }`}
                        >
                          {t.name}
                        </button>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>

            <div className="mt-6 flex justify-end border-t border-slate-200 pt-4 dark:border-slate-800">
              <button
                onClick={() => setShowTopicModal(false)}
                className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-500"
              >
                Done ({selectedTopics.length} selected)
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Error state */}
      {error && (
        <div className="mb-6 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800 dark:border-rose-800 dark:bg-rose-950/50 dark:text-rose-300">
          <p>{error}</p>
        </div>
      )}

      {/* Feed list */}
      <div className="space-y-4">
        {loading ? (
          // Skeletons
          Array.from({ length: 4 }).map((_, i) => (
            <div
              key={i}
              className="animate-pulse rounded-2xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900"
            >
              <div className="h-6 w-1/3 rounded bg-slate-200 dark:bg-slate-800" />
              <div className="mt-3 h-4 w-1/4 rounded bg-slate-100 dark:bg-slate-850" />
              <div className="mt-4 h-16 w-full rounded bg-slate-100 dark:bg-slate-850" />
            </div>
          ))
        ) : experiences.length === 0 ? (
          // Empty state
          <div className="rounded-2xl border border-dashed border-slate-300 p-12 text-center dark:border-slate-800">
            <Briefcase className="mx-auto h-12 w-12 text-slate-400" />
            <h3 className="mt-4 text-base font-semibold text-slate-900 dark:text-white">
              No interview experiences found
            </h3>
            <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
              Try adjusting your search criteria or clear active filters.
            </p>
            {hasActiveFilters && (
              <button
                onClick={handleClearFilters}
                className="mt-4 inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-3.5 py-2 text-xs font-semibold text-white shadow-sm hover:bg-indigo-500"
              >
                <RefreshCw className="h-3.5 w-3.5" /> Clear Filters
              </button>
            )}
          </div>
        ) : (
          experiences.map((exp) => {
            const totalQuestions = (exp.rounds || []).reduce(
              (acc, r) => acc + (r.questions?.length || 0),
              0
            );

            // Collect unique topics from questions
            const topicSet = new Set<string>();
            exp.rounds?.forEach((r) =>
              r.questions?.forEach((q) =>
                q.topics?.forEach((t) => topicSet.add(t.name))
              )
            );
            const topicNames = Array.from(topicSet).slice(0, 5);

            return (
              <Link
                key={exp.id}
                href={`/experiences/${exp.id}`}
                className="group block rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition-all hover:border-indigo-300 hover:shadow-md dark:border-slate-800 dark:bg-slate-900 dark:hover:border-indigo-800"
              >
                {/* Header row */}
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-lg font-bold text-slate-900 group-hover:text-indigo-600 dark:text-white dark:group-hover:text-indigo-400 transition-colors">
                        {exp.company?.name || 'Company'}
                      </span>
                      <span className="text-slate-300 dark:text-slate-700">&middot;</span>
                      <span className="text-sm font-medium text-slate-700 dark:text-slate-300">
                        {exp.roleTitle}
                      </span>
                    </div>

                    {/* Metadata tags */}
                    <div className="mt-1.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500 dark:text-slate-400">
                      {exp.level && (
                        <span className="font-semibold text-slate-700 dark:text-slate-300">
                          {exp.level}
                        </span>
                      )}
                      {exp.interviewYear && (
                        <span className="flex items-center gap-1">
                          <Calendar className="h-3 w-3" />
                          {exp.interviewMonth
                            ? `${exp.interviewMonth}/${exp.interviewYear}`
                            : exp.interviewYear}
                        </span>
                      )}
                      {exp.location && (
                        <span className="flex items-center gap-1">
                          <MapPin className="h-3 w-3" />
                          {exp.location}
                        </span>
                      )}
                      {exp.yearsOfExperience !== null &&
                        exp.yearsOfExperience !== undefined && (
                          <span>{exp.yearsOfExperience} yrs exp</span>
                        )}
                      <span className="flex items-center gap-1">
                        <User className="h-3 w-3" />
                        {exp.isAnonymous || !exp.author
                          ? 'Anonymous'
                          : exp.author.displayName}
                      </span>
                    </div>
                  </div>

                  {/* Outcome badge */}
                  <div>{getOutcomeBadge(exp.outcome)}</div>
                </div>

                {/* Summary snippet */}
                {exp.summary && (
                  <p className="mt-3 text-sm text-slate-600 line-clamp-2 dark:text-slate-300">
                    {exp.summary}
                  </p>
                )}

                {/* Footer details */}
                <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-3 text-xs text-slate-500 dark:border-slate-800 dark:text-slate-400">
                  <div className="flex items-center gap-3">
                    <span className="flex items-center gap-1 font-medium text-slate-700 dark:text-slate-300">
                      <Layers className="h-3.5 w-3.5 text-indigo-500" />
                      {exp.rounds?.length || 0} {(exp.rounds?.length || 0) === 1 ? 'Round' : 'Rounds'}
                    </span>
                    <span className="flex items-center gap-1 font-medium text-slate-700 dark:text-slate-300">
                      <Sparkles className="h-3.5 w-3.5 text-indigo-500" />
                      {totalQuestions} {totalQuestions === 1 ? 'Question' : 'Questions'}
                    </span>
                  </div>

                  {/* Topic pills */}
                  {topicNames.length > 0 && (
                    <div className="flex flex-wrap items-center gap-1">
                      {topicNames.map((name) => (
                        <span
                          key={name}
                          className="rounded-md bg-slate-100 px-2 py-0.5 text-[11px] font-medium text-slate-600 dark:bg-slate-800 dark:text-slate-400"
                        >
                          {name}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              </Link>
            );
          })
        )}

        {/* Load More Button */}
        {pageMeta && pageMeta.page < pageMeta.totalPages - 1 && (
          <div className="pt-6 text-center">
            <button
              onClick={handleLoadMore}
              disabled={loadingMore}
              className="inline-flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-6 py-2.5 text-sm font-semibold text-slate-700 shadow-sm hover:bg-slate-50 disabled:opacity-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-750 transition-colors"
            >
              {loadingMore ? (
                <>
                  <div className="h-4 w-4 animate-spin rounded-full border-2 border-indigo-600 border-t-transparent" />
                  <span>Loading more...</span>
                </>
              ) : (
                <>
                  <span>Load More Experiences</span>
                  <ChevronDown className="h-4 w-4" />
                </>
              )}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
