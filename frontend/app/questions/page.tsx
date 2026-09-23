'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { api } from '@/lib/api';
import type {
  CompanyDto,
  Difficulty,
  PageMetaDto,
  QuestionDto,
  TopicDto,
  TopicKind,
} from '@/lib/types';
import {
  ArrowUpRight,
  Building2,
  ChevronDown,
  Filter,
  HelpCircle,
  RefreshCw,
  Search,
  Sparkles,
  Tag,
  X,
} from 'lucide-react';

const DIFFICULTIES: Difficulty[] = ['EASY', 'MEDIUM', 'HARD'];

export default function QuestionsPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  const [selectedCompany, setSelectedCompany] = useState<CompanyDto | null>(null);
  const [companySearch, setCompanySearch] = useState('');
  const [companySuggestions, setCompanySuggestions] = useState<CompanyDto[]>([]);
  const [showCompanyDropdown, setShowCompanyDropdown] = useState(false);

  const [selectedTopics, setSelectedTopics] = useState<TopicDto[]>([]);
  const [availableTopics, setAvailableTopics] = useState<TopicDto[]>([]);
  const [showTopicModal, setShowTopicModal] = useState(false);

  const [difficulty, setDifficulty] = useState<Difficulty | ''>('');

  const [questions, setQuestions] = useState<QuestionDto[]>([]);
  const [pageMeta, setPageMeta] = useState<PageMetaDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const companyInputRef = useRef<HTMLDivElement>(null);

  // Debounce search
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchTerm);
    }, 300);
    return () => clearTimeout(handler);
  }, [searchTerm]);

  // Load available topics
  useEffect(() => {
    api
      .listTopics()
      .then((t) => setAvailableTopics(t))
      .catch((err) => console.error('Failed to load topics:', err));
  }, []);

  // Company autocomplete
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

  // Click outside to dismiss company dropdown
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

  const fetchQuestions = useCallback(
    async (pageToLoad: number, append = false) => {
      if (append) {
        setLoadingMore(true);
      } else {
        setLoading(true);
      }
      setError(null);

      try {
        const res = await api.listQuestions({
          page: pageToLoad,
          size: 20,
          q: debouncedSearch.trim() || undefined,
          company: selectedCompany?.slug || undefined,
          topic: selectedTopics.length > 0 ? selectedTopics.map((t) => t.slug) : undefined,
          difficulty: (difficulty as Difficulty) || undefined,
        });

        if (append) {
          setQuestions((prev) => [...prev, ...res.content]);
        } else {
          setQuestions(res.content);
        }
        setPageMeta(res.page);
      } catch (err: unknown) {
        const errorMsg = err instanceof Error ? err.message : 'Failed to load questions';
        setError(errorMsg);
      } finally {
        setLoading(false);
        setLoadingMore(false);
      }
    },
    [debouncedSearch, selectedCompany, selectedTopics, difficulty]
  );

  useEffect(() => {
    fetchQuestions(0, false);
  }, [fetchQuestions]);

  const handleLoadMore = () => {
    if (!pageMeta || loadingMore || pageMeta.page >= pageMeta.totalPages - 1) return;
    fetchQuestions(pageMeta.page + 1, true);
  };

  const handleClearFilters = () => {
    setSearchTerm('');
    setDebouncedSearch('');
    setSelectedCompany(null);
    setCompanySearch('');
    setSelectedTopics([]);
    setDifficulty('');
  };

  const hasActiveFilters =
    debouncedSearch || selectedCompany || selectedTopics.length > 0 || difficulty;

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

  const getDifficultyBadge = (diff?: Difficulty | null) => {
    if (!diff) return null;
    switch (diff) {
      case 'EASY':
        return (
          <span className="rounded-md bg-emerald-100 px-2 py-0.5 text-xs font-semibold text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300">
            Easy
          </span>
        );
      case 'MEDIUM':
        return (
          <span className="rounded-md bg-amber-100 px-2 py-0.5 text-xs font-semibold text-amber-800 dark:bg-amber-950/60 dark:text-amber-300">
            Medium
          </span>
        );
      case 'HARD':
        return (
          <span className="rounded-md bg-rose-100 px-2 py-0.5 text-xs font-semibold text-rose-800 dark:bg-rose-950/60 dark:text-rose-300">
            Hard
          </span>
        );
    }
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 sm:text-4xl dark:text-white">
          Interview Questions Archive
        </h1>
        <p className="mt-2 text-base text-slate-600 dark:text-slate-400">
          Browse real technical, algorithmic, and system design questions asked during interviews.
        </p>
      </div>

      {/* Filter Bar */}
      <div className="mb-8 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="relative">
          <Search className="pointer-events-none absolute left-3.5 top-3.5 h-5 w-5 text-slate-400" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search questions (e.g. 'Binary Search', 'Rate Limiter', 'Tell me about a time')..."
            className="w-full rounded-xl border border-slate-300 bg-slate-50 py-3 pl-11 pr-10 text-sm focus:border-indigo-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700 dark:bg-slate-800 dark:text-white"
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
        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
          {/* Company filter */}
          <div className="relative" ref={companyInputRef}>
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
              className="flex w-full items-center justify-between rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200"
            >
              <div className="flex items-center gap-1.5 truncate">
                <Filter className="h-3.5 w-3.5 text-slate-400" />
                <span>
                  {selectedTopics.length === 0
                    ? 'All Topics'
                    : `${selectedTopics.length} topics selected`}
                </span>
              </div>
              <ChevronDown className="h-3.5 w-3.5 text-slate-400 shrink-0" />
            </button>
          </div>

          {/* Difficulty filter */}
          <div>
            <select
              value={difficulty}
              onChange={(e) => setDifficulty(e.target.value as Difficulty | '')}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs text-slate-700 focus:border-indigo-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200"
            >
              <option value="">All Difficulties</option>
              {DIFFICULTIES.map((d) => (
                <option key={d} value={d}>
                  {d}
                </option>
              ))}
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

            {difficulty && (
              <span className="inline-flex items-center gap-1 rounded-md bg-indigo-50 px-2 py-1 text-xs font-medium text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300">
                Difficulty: {difficulty}
                <button onClick={() => setDifficulty('')}>
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
                Filter Questions by Topics
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

      {/* Questions list */}
      <div className="space-y-4">
        {loading ? (
          Array.from({ length: 5 }).map((_, i) => (
            <div
              key={i}
              className="animate-pulse rounded-2xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900"
            >
              <div className="h-5 w-1/4 rounded bg-slate-200 dark:bg-slate-800" />
              <div className="mt-3 h-10 w-full rounded bg-slate-100 dark:bg-slate-850" />
            </div>
          ))
        ) : questions.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-slate-300 p-12 text-center dark:border-slate-800">
            <HelpCircle className="mx-auto h-12 w-12 text-slate-400" />
            <h3 className="mt-4 text-base font-semibold text-slate-900 dark:text-white">
              No questions found
            </h3>
            <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
              Try adjusting your search criteria or clearing filters.
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
          questions.map((q) => (
            <div
              key={q.id}
              className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900"
            >
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex flex-wrap items-center gap-2">
                  {q.company && (
                    <span className="flex items-center gap-1 text-xs font-bold text-slate-900 dark:text-white">
                      <Building2 className="h-3.5 w-3.5 text-indigo-500" />
                      {q.company.name}
                    </span>
                  )}
                  <span className="rounded-md bg-indigo-50 px-2 py-0.5 text-xs font-semibold text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300">
                    {q.questionType.replace(/_/g, ' ')}
                  </span>
                  {getDifficultyBadge(q.difficulty)}
                </div>

                <Link
                  href={`/experiences/${q.experienceId}`}
                  className="inline-flex items-center gap-1 text-xs font-semibold text-indigo-600 hover:text-indigo-500 dark:text-indigo-400"
                >
                  <span>View in full interview</span>
                  <ArrowUpRight className="h-3.5 w-3.5" />
                </Link>
              </div>

              {/* Question Text */}
              <p className="mt-3 text-sm font-medium leading-relaxed text-slate-900 dark:text-slate-100 whitespace-pre-wrap">
                {q.text}
              </p>

              {/* Topics Footer */}
              {q.topics && q.topics.length > 0 && (
                <div className="mt-4 flex flex-wrap items-center gap-1.5 pt-3 border-t border-slate-100 dark:border-slate-800">
                  <Tag className="h-3 w-3 text-slate-400" />
                  {q.topics.map((t) => (
                    <span
                      key={t.slug}
                      className="rounded-md bg-slate-100 px-2 py-0.5 text-[11px] font-medium text-slate-600 dark:bg-slate-800 dark:text-slate-400"
                    >
                      {t.name}
                    </span>
                  ))}
                </div>
              )}
            </div>
          ))
        )}

        {/* Load More Button */}
        {pageMeta && pageMeta.page < pageMeta.totalPages - 1 && (
          <div className="pt-6 text-center">
            <button
              onClick={handleLoadMore}
              disabled={loadingMore}
              className="inline-flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-6 py-2.5 text-sm font-semibold text-slate-700 shadow-sm hover:bg-slate-50 disabled:opacity-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 transition-colors"
            >
              {loadingMore ? (
                <>
                  <div className="h-4 w-4 animate-spin rounded-full border-2 border-indigo-600 border-t-transparent" />
                  <span>Loading more...</span>
                </>
              ) : (
                <>
                  <span>Load More Questions</span>
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

