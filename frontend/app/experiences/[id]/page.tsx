'use client';

import { use, useEffect, useState } from 'react';
import Link from 'next/link';
import { api } from '@/lib/api';
import type { Difficulty, ExperienceDto, Outcome, QuestionType, RoundType } from '@/lib/types';
import {
  ArrowLeft,
  Building2,
  Calendar,
  CheckCircle2,
  Clock,
  HelpCircle,
  Layers,
  MapPin,
  Tag,
  User,
  XCircle,
} from 'lucide-react';

export default function ExperienceDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const resolvedParams = use(params);
  const experienceId = resolvedParams.id;

  const [experience, setExperience] = useState<ExperienceDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    api
      .getExperience(experienceId)
      .then((data) => setExperience(data))
      .catch((err) => {
        const msg = err instanceof Error ? err.message : 'Failed to load experience';
        setError(msg);
      })
      .finally(() => setLoading(false));
  }, [experienceId]);

  const getOutcomeBadge = (outcome: Outcome) => {
    switch (outcome) {
      case 'SELECTED':
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300">
            <CheckCircle2 className="h-4 w-4" /> Offer Received
          </span>
        );
      case 'REJECTED':
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-rose-100 px-3 py-1 text-xs font-semibold text-rose-800 dark:bg-rose-950/60 dark:text-rose-300">
            <XCircle className="h-4 w-4" /> Rejected
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-100 px-3 py-1 text-xs font-semibold text-amber-800 dark:bg-amber-950/60 dark:text-amber-300">
            {outcome}
          </span>
        );
    }
  };

  const getDifficultyBadge = (difficulty?: Difficulty | null) => {
    if (!difficulty) return null;
    switch (difficulty) {
      case 'EASY':
        return (
          <span className="rounded-md bg-emerald-50 px-2 py-0.5 text-xs font-semibold text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-400">
            Easy
          </span>
        );
      case 'MEDIUM':
        return (
          <span className="rounded-md bg-amber-50 px-2 py-0.5 text-xs font-semibold text-amber-700 dark:bg-amber-950/40 dark:text-amber-400">
            Medium
          </span>
        );
      case 'HARD':
        return (
          <span className="rounded-md bg-rose-50 px-2 py-0.5 text-xs font-semibold text-rose-700 dark:bg-rose-950/40 dark:text-rose-400">
            Hard
          </span>
        );
    }
  };

  const formatRoundType = (type: RoundType) => {
    return type.replace(/_/g, ' ');
  };

  const formatQuestionType = (type: QuestionType) => {
    return type.replace(/_/g, ' ');
  };

  if (loading) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-12 sm:px-6">
        <div className="animate-pulse space-y-6">
          <div className="h-6 w-24 rounded bg-slate-200 dark:bg-slate-800" />
          <div className="h-10 w-2/3 rounded bg-slate-200 dark:bg-slate-800" />
          <div className="h-32 rounded-2xl bg-white p-6 dark:bg-slate-900" />
          <div className="h-64 rounded-2xl bg-white p-6 dark:bg-slate-900" />
        </div>
      </div>
    );
  }

  if (error || !experience) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-16 text-center">
        <h2 className="text-xl font-bold text-slate-900 dark:text-white">
          Experience Not Found
        </h2>
        <p className="mt-2 text-sm text-slate-500">
          {error || "The interview experience you are looking for doesn't exist."}
        </p>
        <Link
          href="/"
          className="mt-6 inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500"
        >
          <ArrowLeft className="h-4 w-4" /> Back to Experiences
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      {/* Navigation */}
      <div className="mb-6">
        <Link
          href="/"
          className="inline-flex items-center gap-2 text-sm font-medium text-slate-500 hover:text-indigo-600 dark:text-slate-400 dark:hover:text-indigo-400 transition-colors"
        >
          <ArrowLeft className="h-4 w-4" /> Back to all experiences
        </Link>
      </div>

      {/* Main Experience Header Card */}
      <div className="rounded-2xl border border-slate-200 bg-white p-6 sm:p-8 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <div className="flex items-center gap-2.5">
              <span className="text-2xl font-extrabold tracking-tight text-slate-900 sm:text-3xl dark:text-white">
                {experience.company?.name || 'Company'}
              </span>
              <span className="text-slate-300 dark:text-slate-700">&middot;</span>
              <span className="text-lg font-medium text-slate-700 dark:text-slate-300">
                {experience.roleTitle}
              </span>
            </div>

            {/* Subtitle details */}
            <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2 text-xs text-slate-600 dark:text-slate-400">
              {experience.level && (
                <span className="rounded-md bg-slate-100 px-2.5 py-1 font-semibold text-slate-800 dark:bg-slate-800 dark:text-slate-200">
                  {experience.level}
                </span>
              )}
              {experience.interviewYear && (
                <span className="flex items-center gap-1.5">
                  <Calendar className="h-3.5 w-3.5 text-slate-400" />
                  Interviewed in{' '}
                  {experience.interviewMonth
                    ? `${experience.interviewMonth}/${experience.interviewYear}`
                    : experience.interviewYear}
                </span>
              )}
              {experience.interviewMode && (
                <span className="capitalize">{experience.interviewMode.toLowerCase().replace('_', ' ')}</span>
              )}
              {experience.location && (
                <span className="flex items-center gap-1.5">
                  <MapPin className="h-3.5 w-3.5 text-slate-400" />
                  {experience.location}
                </span>
              )}
              {experience.yearsOfExperience !== null &&
                experience.yearsOfExperience !== undefined && (
                  <span>{experience.yearsOfExperience} years experience</span>
                )}
            </div>
          </div>

          <div>{getOutcomeBadge(experience.outcome)}</div>
        </div>

        {/* Summary note */}
        {experience.summary && (
          <div className="mt-6 rounded-xl bg-slate-50 p-4 text-sm leading-relaxed text-slate-700 dark:bg-slate-800/60 dark:text-slate-200">
            <h3 className="mb-1 text-xs font-bold uppercase tracking-wider text-slate-400">
              Overview & Key Takeaways
            </h3>
            <p className="whitespace-pre-wrap">{experience.summary}</p>
          </div>
        )}

        {/* Author / Date footer */}
        <div className="mt-6 flex items-center justify-between border-t border-slate-100 pt-4 text-xs text-slate-400 dark:border-slate-800">
          <div className="flex items-center gap-2">
            <User className="h-3.5 w-3.5" />
            <span>
              Shared by{' '}
              <strong className="text-slate-700 dark:text-slate-300">
                {experience.isAnonymous || !experience.author
                  ? 'Anonymous Candidate'
                  : experience.author.displayName}
              </strong>
            </span>
          </div>
          <span>
            Posted on {new Date(experience.createdAt).toLocaleDateString()}
          </span>
        </div>
      </div>

      {/* Rounds & Questions Section */}
      <div className="mt-10 space-y-8">
        <div className="flex items-center gap-2">
          <Layers className="h-5 w-5 text-indigo-600" />
          <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white">
            Interview Rounds & Questions ({experience.rounds?.length || 0})
          </h2>
        </div>

        {experience.rounds && experience.rounds.length > 0 ? (
          experience.rounds
            .sort((a, b) => a.roundNumber - b.roundNumber)
            .map((round) => (
              <div
                key={round.roundNumber}
                className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900"
              >
                {/* Round Header */}
                <div className="border-b border-slate-100 bg-slate-50/75 px-6 py-4 dark:border-slate-800 dark:bg-slate-850">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="flex items-center gap-2.5">
                      <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-600 text-xs font-bold text-white">
                        {round.roundNumber}
                      </span>
                      <h3 className="text-base font-bold text-slate-900 dark:text-white">
                        Round {round.roundNumber}: {formatRoundType(round.roundType)}
                      </h3>
                    </div>

                    {round.durationMinutes && (
                      <span className="flex items-center gap-1 text-xs text-slate-500">
                        <Clock className="h-3.5 w-3.5" />
                        {round.durationMinutes} minutes
                      </span>
                    )}
                  </div>

                  {round.notes && (
                    <p className="mt-2 text-xs text-slate-600 dark:text-slate-400">
                      {round.notes}
                    </p>
                  )}
                </div>

                {/* Questions in this round */}
                <div className="p-6 space-y-4">
                  {round.questions && round.questions.length > 0 ? (
                    round.questions.map((question, qIdx) => (
                      <div
                        key={question.id || qIdx}
                        className="rounded-xl border border-slate-100 bg-slate-50/50 p-4 dark:border-slate-800/80 dark:bg-slate-800/40"
                      >
                        {/* Question Badges */}
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="rounded-md bg-indigo-50 px-2 py-0.5 text-xs font-semibold text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300">
                            {formatQuestionType(question.questionType)}
                          </span>
                          {getDifficultyBadge(question.difficulty)}
                        </div>

                        {/* Question Text */}
                        <p className="mt-2.5 text-sm font-medium leading-relaxed text-slate-900 dark:text-slate-100 whitespace-pre-wrap">
                          {question.text}
                        </p>

                        {/* Topic Tags */}
                        {question.topics && question.topics.length > 0 && (
                          <div className="mt-3 flex flex-wrap items-center gap-1.5 pt-2 border-t border-slate-100 dark:border-slate-800">
                            <Tag className="h-3 w-3 text-slate-400" />
                            {question.topics.map((topic) => (
                              <span
                                key={topic.slug}
                                className="rounded-md bg-white px-2 py-0.5 text-[11px] font-medium text-slate-600 shadow-2xs border border-slate-200 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
                              >
                                {topic.name}
                              </span>
                            ))}
                          </div>
                        )}
                      </div>
                    ))
                  ) : (
                    <p className="text-xs italic text-slate-400">
                      No specific questions recorded for this round.
                    </p>
                  )}
                </div>
              </div>
            ))
        ) : (
          <p className="text-sm text-slate-500">No rounds listed.</p>
        )}
      </div>
    </div>
  );
}

