'use client';

import { use, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/api';
import type {
  Difficulty,
  ExperienceInputDto,
  ExtractionJobDto,
  InterviewMode,
  Level,
  Outcome,
  QuestionInputDto,
  QuestionType,
  RoundInputDto,
  RoundType,
  TopicDto,
} from '@/lib/types';
import {
  AlertCircle,
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  ChevronDown,
  Edit3,
  HelpCircle,
  Layers,
  Plus,
  Send,
  Sparkles,
  Tag,
  Trash2,
  X,
} from 'lucide-react';

const LEVELS: Level[] = ['INTERN', 'JUNIOR', 'MID', 'SENIOR', 'LEAD', 'PRINCIPAL'];
const OUTCOMES: Outcome[] = ['SELECTED', 'REJECTED', 'PENDING', 'UNKNOWN'];
const INTERVIEW_MODES: InterviewMode[] = ['ONSITE', 'REMOTE', 'HYBRID'];
const ROUND_TYPES: RoundType[] = [
  'ONLINE_ASSESSMENT',
  'TECHNICAL',
  'MACHINE_CODING',
  'SYSTEM_DESIGN',
  'MANAGERIAL',
  'HR',
  'BEHAVIORAL',
  'OTHER',
];
const QUESTION_TYPES: QuestionType[] = [
  'THEORY',
  'CODING',
  'SYSTEM_DESIGN',
  'BEHAVIORAL',
  'SCENARIO',
  'OTHER',
];
const DIFFICULTIES: Difficulty[] = ['EASY', 'MEDIUM', 'HARD'];

function normalizeLevel(val: unknown): Level {
  const str = String(val || '').toUpperCase().trim();
  if (str === 'INTERN') return 'INTERN';
  if (str === 'JUNIOR' || str === 'ENTRY' || str === 'ENTRY_LEVEL' || str === 'FRESHER') return 'JUNIOR';
  if (str === 'MID' || str === 'MID_LEVEL' || str === 'INTERMEDIATE') return 'MID';
  if (str === 'SENIOR' || str === 'SR') return 'SENIOR';
  if (str === 'LEAD' || str === 'STAFF') return 'LEAD';
  if (str === 'PRINCIPAL' || str === 'DIRECTOR') return 'PRINCIPAL';
  return 'MID';
}

function normalizeDifficulty(val: unknown): Difficulty {
  const str = String(val || '').toUpperCase().trim();
  if (str === 'EASY') return 'EASY';
  if (str === 'HARD') return 'HARD';
  return 'MEDIUM';
}

function normalizeOutcome(val: unknown): Outcome {
  const str = String(val || '').toUpperCase().trim();
  if (str === 'OFFER' || str === 'SELECTED' || str === 'HIRED') return 'SELECTED';
  if (str === 'REJECTED' || str === 'NO_OFFER') return 'REJECTED';
  if (str === 'PENDING') return 'PENDING';
  return 'UNKNOWN';
}

function normalizeInterviewMode(val: unknown): InterviewMode {
  const str = String(val || '').toUpperCase().trim();
  if (str === 'ONSITE' || str === 'IN_PERSON' || str === 'ON_SITE') return 'ONSITE';
  if (str === 'REMOTE' || str === 'VIRTUAL' || str === 'ONLINE') return 'REMOTE';
  return 'HYBRID';
}

function normalizeRoundType(val: unknown): RoundType {
  const str = String(val || '').toUpperCase().trim();
  if (str === 'ONLINE_ASSESSMENT' || str === 'OA') return 'ONLINE_ASSESSMENT';
  if (str === 'MACHINE_CODING') return 'MACHINE_CODING';
  if (str === 'SYSTEM_DESIGN') return 'SYSTEM_DESIGN';
  if (str === 'MANAGERIAL' || str === 'HIRING_MANAGER') return 'MANAGERIAL';
  if (str === 'HR') return 'HR';
  if (str === 'BEHAVIORAL') return 'BEHAVIORAL';
  if (str === 'TECHNICAL' || str === 'CODING') return 'TECHNICAL';
  return 'OTHER';
}

function normalizeQuestionType(val: unknown): QuestionType {
  const str = String(val || '').toUpperCase().trim();
  if (str === 'CODING' || str === 'DSA') return 'CODING';
  if (str === 'SYSTEM_DESIGN') return 'SYSTEM_DESIGN';
  if (str === 'BEHAVIORAL') return 'BEHAVIORAL';
  if (str === 'THEORY' || str === 'CS_FUNDAMENTALS' || str === 'TRIVIA') return 'THEORY';
  if (str === 'SCENARIO') return 'SCENARIO';
  return 'OTHER';
}

export default function SubmitPreviewPage({
  params,
}: {
  params: Promise<{ jobId: string }>;
}) {
  const router = useRouter();
  const resolvedParams = use(params);
  const jobId = resolvedParams.jobId;

  const [job, setJob] = useState<ExtractionJobDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [publishing, setPublishing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [publishError, setPublishError] = useState<string | null>(null);

  // Available topics for tag suggestions
  const [allTopics, setAllTopics] = useState<TopicDto[]>([]);

  // The fully editable form state initialized from extraction result
  const [formData, setFormData] = useState<ExperienceInputDto | null>(null);

  // Fetch extraction job status on mount (with short polling support if still pending)
  useEffect(() => {
    let timer: NodeJS.Timeout;
    let attempts = 0;

    const fetchJob = async () => {
      try {
        const data = await api.getExtraction(jobId);
        setJob(data);

        if (data.status === 'SUCCEEDED' && data.result?.experience) {
          // Initialize editable form data with deep copy from extraction result
          const exp = data.result.experience;
          setFormData({
            companyName: exp.companyName || '',
            roleTitle: exp.roleTitle || '',
            level: normalizeLevel(exp.level),
            yearsOfExperience: exp.yearsOfExperience ?? null,
            location: exp.location || '',
            interviewYear: exp.interviewYear || new Date().getFullYear(),
            interviewMonth: exp.interviewMonth || new Date().getMonth() + 1,
            interviewMode: normalizeInterviewMode(exp.interviewMode),
            outcome: normalizeOutcome(exp.outcome),
            summary: exp.summary || '',
            isAnonymous: exp.isAnonymous ?? false,
            rounds: (exp.rounds || []).map((r, rIdx) => ({
              roundNumber: r.roundNumber || rIdx + 1,
              roundType: normalizeRoundType(r.roundType),
              durationMinutes: r.durationMinutes ?? 45,
              notes: r.notes || '',
              questions: (r.questions || []).map((q) => ({
                text: q.text || '',
                questionType: normalizeQuestionType(q.questionType),
                difficulty: normalizeDifficulty(q.difficulty),
                topicSlugs: [...(q.topicSlugs || [])].slice(0, 5),
              })),
            })),
          });
          setLoading(false);
        } else if (data.status === 'FAILED') {
          setLoading(false);
        } else {
          // PENDING or PROCESSING: retry after 1.5s
          attempts++;
          if (attempts < 12) {
            timer = setTimeout(fetchJob, 1500);
          } else {
            setLoading(false);
            setError('Extraction timed out. Please try again.');
          }
        }
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to fetch extraction job';
        setError(msg);
        setLoading(false);
      }
    };

    fetchJob();
    api.listTopics().then((t) => setAllTopics(t)).catch(() => {});

    return () => {
      if (timer) clearTimeout(timer);
    };
  }, [jobId]);

  // Form manipulation helpers
  const handleUpdateExperience = <K extends keyof ExperienceInputDto>(
    field: K,
    value: ExperienceInputDto[K]
  ) => {
    if (!formData) return;
    setFormData({
      ...formData,
      [field]: value,
    });
  };

  const handleUpdateRound = <K extends keyof RoundInputDto>(
    roundIdx: number,
    field: K,
    value: RoundInputDto[K]
  ) => {
    if (!formData) return;
    const newRounds = [...formData.rounds];
    newRounds[roundIdx] = {
      ...newRounds[roundIdx],
      [field]: value,
    };
    setFormData({ ...formData, rounds: newRounds });
  };

  const handleAddRound = () => {
    if (!formData) return;
    const newRoundNum = formData.rounds.length + 1;
    const newRound: RoundInputDto = {
      roundNumber: newRoundNum,
      roundType: 'TECHNICAL',
      durationMinutes: 45,
      notes: '',
      questions: [
        {
          text: '',
          questionType: 'CODING',
          difficulty: 'MEDIUM',
          topicSlugs: [],
        },
      ],
    };
    setFormData({
      ...formData,
      rounds: [...formData.rounds, newRound],
    });
  };

  const handleDeleteRound = (roundIdx: number) => {
    if (!formData) return;
    if (formData.rounds.length <= 1) {
      alert('An experience must have at least one round.');
      return;
    }
    const filtered = formData.rounds.filter((_, idx) => idx !== roundIdx);
    // Renumber rounds
    const renumbered = filtered.map((r, i) => ({
      ...r,
      roundNumber: i + 1,
    }));
    setFormData({ ...formData, rounds: renumbered });
  };

  const handleUpdateQuestion = <K extends keyof QuestionInputDto>(
    roundIdx: number,
    questionIdx: number,
    field: K,
    value: QuestionInputDto[K]
  ) => {
    if (!formData) return;
    const newRounds = [...formData.rounds];
    const newQuestions = [...newRounds[roundIdx].questions];
    newQuestions[questionIdx] = {
      ...newQuestions[questionIdx],
      [field]: value,
    };
    newRounds[roundIdx] = {
      ...newRounds[roundIdx],
      questions: newQuestions,
    };
    setFormData({ ...formData, rounds: newRounds });
  };

  const handleAddQuestion = (roundIdx: number) => {
    if (!formData) return;
    const newRounds = [...formData.rounds];
    newRounds[roundIdx].questions.push({
      text: '',
      questionType: 'CODING',
      difficulty: 'MEDIUM',
      topicSlugs: [],
    });
    setFormData({ ...formData, rounds: newRounds });
  };

  const handleDeleteQuestion = (roundIdx: number, questionIdx: number) => {
    if (!formData) return;
    const newRounds = [...formData.rounds];
    newRounds[roundIdx].questions = newRounds[roundIdx].questions.filter(
      (_, idx) => idx !== questionIdx
    );
    setFormData({ ...formData, rounds: newRounds });
  };

  const handleAddTopicToQuestion = (
    roundIdx: number,
    questionIdx: number,
    topicSlug: string
  ) => {
    if (!formData || !topicSlug.trim()) return;
    const slug = topicSlug.trim().toLowerCase().replace(/\s+/g, '-');
    const existing = formData.rounds[roundIdx].questions[questionIdx].topicSlugs;
    if (!existing.includes(slug)) {
      handleUpdateQuestion(roundIdx, questionIdx, 'topicSlugs', [...existing, slug]);
    }
  };

  const handleRemoveTopicFromQuestion = (
    roundIdx: number,
    questionIdx: number,
    topicSlug: string
  ) => {
    if (!formData) return;
    const existing = formData.rounds[roundIdx].questions[questionIdx].topicSlugs;
    handleUpdateQuestion(
      roundIdx,
      questionIdx,
      'topicSlugs',
      existing.filter((s) => s !== topicSlug)
    );
  };

  // Submit & Publish handler
  const handlePublish = async () => {
    if (!formData) return;

    if (!formData.companyName.trim()) {
      setPublishError('Please specify a company name.');
      return;
    }

    if (!formData.rounds || formData.rounds.length === 0) {
      setPublishError('Please include at least one interview round.');
      return;
    }

    // Validate that questions have non-empty text
    for (const round of formData.rounds) {
      if (!round.questions || round.questions.length === 0) {
        setPublishError(`Round ${round.roundNumber} must contain at least one question.`);
        return;
      }
      for (const q of round.questions) {
        if (!q.text.trim() || q.text.trim().length < 5) {
          setPublishError(
            `Question in Round ${round.roundNumber} is too short (min 5 characters required).`
          );
          return;
        }
      }
    }

    setPublishing(true);
    setPublishError(null);

    try {
      const rawYoe = formData.yearsOfExperience;
      const numYoe =
        rawYoe !== null && rawYoe !== undefined && (rawYoe as unknown) !== ''
          ? Number(rawYoe)
          : null;
      const validYoe =
        numYoe !== null && !isNaN(numYoe)
          ? Math.max(0, Math.min(60, Math.round(numYoe * 10) / 10))
          : null;

      const numYear = formData.interviewYear ? Number(formData.interviewYear) : null;
      const validYear =
        numYear && !isNaN(numYear) && numYear >= 2000 && numYear <= 2100
          ? numYear
          : new Date().getFullYear();

      const numMonth = formData.interviewMonth ? Number(formData.interviewMonth) : null;
      const validMonth =
        numMonth && !isNaN(numMonth) && numMonth >= 1 && numMonth <= 12
          ? numMonth
          : new Date().getMonth() + 1;

      const sanitizedPayload: ExperienceInputDto = {
        companyName: formData.companyName.trim(),
        roleTitle: formData.roleTitle?.trim() || null,
        level: normalizeLevel(formData.level),
        yearsOfExperience: validYoe,
        location: formData.location?.trim() || null,
        interviewYear: validYear,
        interviewMonth: validMonth,
        interviewMode: normalizeInterviewMode(formData.interviewMode),
        outcome: normalizeOutcome(formData.outcome),
        summary: formData.summary?.trim() || null,
        isAnonymous: Boolean(formData.isAnonymous),
        rounds: formData.rounds.map((r, i) => ({
          roundNumber: i + 1,
          roundType: normalizeRoundType(r.roundType),
          durationMinutes:
            r.durationMinutes && Number(r.durationMinutes) > 0
              ? Number(r.durationMinutes)
              : null,
          notes: r.notes?.trim() || null,
          questions: r.questions.map((q) => ({
            text: q.text.trim(),
            questionType: normalizeQuestionType(q.questionType),
            difficulty: normalizeDifficulty(q.difficulty),
            topicSlugs: (q.topicSlugs || []).slice(0, 5),
          })),
        })),
      };

      const published = await api.publishExtraction(jobId, sanitizedPayload);
      router.push(`/experiences/${published.id}`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to publish experience';
      setPublishError(msg);
      setPublishing(false);
    }
  };

  // Loading view
  if (loading) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-20 text-center">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600 dark:bg-indigo-950/60 dark:text-indigo-400">
          <Sparkles className="h-8 w-8 animate-pulse" />
        </div>
        <h2 className="mt-4 text-xl font-bold text-slate-900 dark:text-white">
          Extracting Interview Details...
        </h2>
        <p className="mt-2 text-sm text-slate-500">
          Analyzing interview rounds, questions, difficulty levels, and topics.
        </p>
      </div>
    );
  }

  // Failure or Not Interview Content
  const isFailed =
    job?.status === 'FAILED' ||
    (job?.result && job.result.isInterviewContent === false);

  if (isFailed || error || !formData) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-16 text-center">
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-100 text-amber-600 dark:bg-amber-950/60 dark:text-amber-400">
          <AlertTriangle className="h-8 w-8" />
        </div>
        <h2 className="mt-4 text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
          Extraction Notice
        </h2>
        <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
          {job?.result?.isInterviewContent === false
            ? "This doesn't look like an interview experience — try pasting the actual Q&A text."
            : error || job?.errorCode || 'Could not extract valid interview details.'}
        </p>

        {job?.result?.warnings && job.result.warnings.length > 0 && (
          <div className="mt-6 rounded-xl bg-slate-100 p-4 text-left text-xs text-slate-600 dark:bg-slate-800 dark:text-slate-300">
            <span className="font-semibold block mb-1">Details:</span>
            <ul className="list-disc pl-4 space-y-1">
              {job.result.warnings.map((w, idx) => (
                <li key={idx}>{w}</li>
              ))}
            </ul>
          </div>
        )}

        <div className="mt-8 flex justify-center gap-4">
          <Link
            href="/submit"
            className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500"
          >
            <ArrowLeft className="h-4 w-4" /> Go Back &amp; Try Again
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      {/* Navigation & Header */}
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <Link
            href="/submit"
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-800 dark:hover:text-slate-200"
          >
            <ArrowLeft className="h-3.5 w-3.5" /> Back to paste text
          </Link>
          <h1 className="mt-1 text-2xl font-extrabold tracking-tight text-slate-900 sm:text-3xl dark:text-white">
            Review &amp; Edit Before Publishing
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Every field below was extracted by AI and is <strong>fully editable</strong>. Correct any errors or add missing questions before publishing.
          </p>
        </div>

        <button
          onClick={handlePublish}
          disabled={publishing}
          className="flex items-center gap-2 rounded-xl bg-indigo-600 px-6 py-2.5 text-sm font-bold text-white shadow-md hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:opacity-50 transition-all"
        >
          {publishing ? (
            <>
              <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
              <span>Publishing...</span>
            </>
          ) : (
            <>
              <Send className="h-4 w-4" />
              <span>Confirm &amp; Publish</span>
            </>
          )}
        </button>
      </div>

      {/* Warnings & AI feedback banner */}
      {job?.result?.warnings && job.result.warnings.length > 0 && (
        <div className="mb-6 flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-xs text-amber-900 dark:border-amber-900/60 dark:bg-amber-950/40 dark:text-amber-300">
          <AlertCircle className="h-5 w-5 shrink-0 text-amber-600" />
          <div>
            <span className="font-bold">AI Extraction Note:</span>
            <ul className="mt-1 list-disc pl-4 space-y-0.5">
              {job.result.warnings.map((w, idx) => (
                <li key={idx}>{w}</li>
              ))}
            </ul>
          </div>
        </div>
      )}

      {/* Error notification */}
      {publishError && (
        <div className="mb-6 flex items-start gap-3 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-xs text-rose-800 dark:border-rose-900 dark:bg-rose-950/50 dark:text-rose-300">
          <AlertCircle className="h-5 w-5 shrink-0 text-rose-600" />
          <p>{publishError}</p>
        </div>
      )}

      {/* SECTION 1: Experience Overview Form */}
      <div className="mb-8 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <h2 className="text-base font-bold text-slate-900 dark:text-white mb-4 flex items-center gap-2">
          <Edit3 className="h-4 w-4 text-indigo-600" />
          <span>General Interview Details</span>
        </h2>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {/* Company Name */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
              Company Name <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              required
              value={formData.companyName}
              onChange={(e) => handleUpdateExperience('companyName', e.target.value)}
              placeholder="e.g. Google, Meta, Stripe"
              className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs focus:border-indigo-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-white"
            />
          </div>

          {/* Role Title */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
              Role Title
            </label>
            <input
              type="text"
              value={formData.roleTitle || ''}
              onChange={(e) => handleUpdateExperience('roleTitle', e.target.value)}
              placeholder="e.g. Senior Software Engineer"
              className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs focus:border-indigo-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-white"
            />
          </div>

          {/* Level */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
              Level / Seniority
            </label>
            <select
              value={formData.level || 'MID'}
              onChange={(e) => handleUpdateExperience('level', e.target.value as Level)}
              className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs focus:border-indigo-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-white"
            >
              {LEVELS.map((lvl) => (
                <option key={lvl} value={lvl}>
                  {lvl}
                </option>
              ))}
            </select>
          </div>

          {/* Outcome */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
              Interview Outcome <span className="text-rose-500">*</span>
            </label>
            <select
              value={formData.outcome}
              onChange={(e) => handleUpdateExperience('outcome', e.target.value as Outcome)}
              className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-bold text-slate-800 focus:border-indigo-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-white"
            >
              <option value="SELECTED">Offer / Selected</option>
              <option value="REJECTED">Rejected</option>
              <option value="PENDING">Pending</option>
              <option value="UNKNOWN">Unknown / Other</option>
            </select>
          </div>

          {/* Years of Experience */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
              Years of Experience
            </label>
            <input
              type="number"
              step="0.5"
              min="0"
              max="50"
              value={formData.yearsOfExperience ?? ''}
              onChange={(e) =>
                handleUpdateExperience(
                  'yearsOfExperience',
                  e.target.value ? parseFloat(e.target.value) : null
                )
              }
              placeholder="e.g. 4.5"
              className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs focus:border-indigo-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-white"
            />
          </div>

          {/* Mode */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
              Interview Mode
            </label>
            <select
              value={formData.interviewMode || 'REMOTE'}
              onChange={(e) =>
                handleUpdateExperience('interviewMode', e.target.value as InterviewMode)
              }
              className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs focus:border-indigo-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-white"
            >
              <option value="REMOTE">Remote / Virtual</option>
              <option value="ONSITE">On-site / In Person</option>
              <option value="HYBRID">Hybrid</option>
            </select>
          </div>

          {/* Year & Month */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
              Interview Month / Year
            </label>
            <div className="mt-1 flex gap-2">
              <select
                value={formData.interviewMonth || 1}
                onChange={(e) =>
                  handleUpdateExperience('interviewMonth', parseInt(e.target.value, 10))
                }
                className="w-1/2 rounded-lg border border-slate-300 bg-white px-2 py-2 text-xs focus:border-indigo-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-white"
              >
                {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
                  <option key={m} value={m}>
                    Month {m}
                  </option>
                ))}
              </select>
              <input
                type="number"
                min="2000"
                max="2100"
                value={formData.interviewYear || new Date().getFullYear()}
                onChange={(e) =>
                  handleUpdateExperience(
                    'interviewYear',
                    e.target.value ? parseInt(e.target.value, 10) : undefined
                  )
                }
                className="w-1/2 rounded-lg border border-slate-300 bg-white px-2 py-2 text-xs focus:border-indigo-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-white"
              />
            </div>
          </div>

          {/* Location */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
              Location
            </label>
            <input
              type="text"
              value={formData.location || ''}
              onChange={(e) => handleUpdateExperience('location', e.target.value)}
              placeholder="e.g. Seattle, WA or Remote"
              className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs focus:border-indigo-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-white"
            />
          </div>

          {/* Anonymous check */}
          <div className="flex items-center gap-2 pt-6">
            <input
              type="checkbox"
              id="is-anon"
              checked={formData.isAnonymous || false}
              onChange={(e) => handleUpdateExperience('isAnonymous', e.target.checked)}
              className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
            />
            <label htmlFor="is-anon" className="text-xs font-medium text-slate-700 dark:text-slate-300">
              Post anonymously (hide your author name)
            </label>
          </div>
        </div>

        {/* Summary textarea */}
        <div className="mt-4">
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
            Overall Summary / Notes
          </label>
          <textarea
            rows={3}
            value={formData.summary || ''}
            onChange={(e) => handleUpdateExperience('summary', e.target.value)}
            placeholder="Share overall impressions, preparation tips, compensation details, or hiring team responsiveness..."
            className="mt-1 block w-full rounded-lg border border-slate-300 bg-white p-3 text-xs leading-relaxed focus:border-indigo-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-white"
          />
        </div>
      </div>

      {/* SECTION 2: Rounds & Questions Editor */}
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
            <Layers className="h-5 w-5 text-indigo-600" />
            <span>Interview Rounds &amp; Questions ({formData.rounds.length})</span>
          </h2>

          <button
            type="button"
            onClick={handleAddRound}
            className="inline-flex items-center gap-1.5 rounded-lg border border-indigo-600 bg-indigo-50 px-3.5 py-1.5 text-xs font-semibold text-indigo-700 hover:bg-indigo-100 dark:border-indigo-500 dark:bg-indigo-950/60 dark:text-indigo-300 transition-colors"
          >
            <Plus className="h-3.5 w-3.5" /> Add Round
          </button>
        </div>

        {formData.rounds.map((round, rIdx) => (
          <div
            key={rIdx}
            className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden dark:border-slate-800 dark:bg-slate-900"
          >
            {/* Round Top Bar */}
            <div className="border-b border-slate-200 bg-slate-50/80 px-6 py-4 dark:border-slate-800 dark:bg-slate-850">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex flex-wrap items-center gap-3">
                  <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-600 text-xs font-bold text-white">
                    {round.roundNumber}
                  </span>

                  {/* Round Type Selector */}
                  <select
                    value={round.roundType}
                    onChange={(e) =>
                      handleUpdateRound(rIdx, 'roundType', e.target.value as RoundType)
                    }
                    className="rounded-lg border border-slate-300 bg-white px-2.5 py-1 text-xs font-bold text-slate-800 focus:border-indigo-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                  >
                    <option value="TECHNICAL">Technical / Coding</option>
                    <option value="ONLINE_ASSESSMENT">Online Assessment (OA)</option>
                    <option value="MACHINE_CODING">Machine Coding</option>
                    <option value="SYSTEM_DESIGN">System Design</option>
                    <option value="MANAGERIAL">Managerial / Hiring Manager</option>
                    <option value="BEHAVIORAL">Behavioral</option>
                    <option value="HR">HR</option>
                    <option value="OTHER">Other</option>
                  </select>

                  {/* Duration input */}
                  <div className="flex items-center gap-1.5 text-xs text-slate-500">
                    <span>Duration:</span>
                    <input
                      type="number"
                      min="5"
                      max="480"
                      value={round.durationMinutes ?? ''}
                      onChange={(e) =>
                        handleUpdateRound(
                          rIdx,
                          'durationMinutes',
                          e.target.value ? parseInt(e.target.value, 10) : null
                        )
                      }
                      className="w-16 rounded-md border border-slate-300 bg-white px-2 py-0.5 text-xs dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                    />
                    <span>mins</span>
                  </div>
                </div>

                {/* Delete round button */}
                <button
                  type="button"
                  onClick={() => handleDeleteRound(rIdx)}
                  className="inline-flex items-center gap-1 text-xs font-semibold text-rose-600 hover:text-rose-700 dark:text-rose-400"
                >
                  <Trash2 className="h-3.5 w-3.5" /> Delete Round
                </button>
              </div>

              {/* Round Notes */}
              <div className="mt-3">
                <input
                  type="text"
                  value={round.notes || ''}
                  onChange={(e) => handleUpdateRound(rIdx, 'notes', e.target.value)}
                  placeholder="Round notes (e.g. 2 interviewers, live CoderPad session, friendly tone)..."
                  className="w-full rounded-md border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-700 placeholder:text-slate-400 focus:border-indigo-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200"
                />
              </div>
            </div>

            {/* Questions list inside round */}
            <div className="p-6 space-y-5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Questions in this round ({round.questions.length})
                </span>
                <button
                  type="button"
                  onClick={() => handleAddQuestion(rIdx)}
                  className="inline-flex items-center gap-1 text-xs font-semibold text-indigo-600 hover:text-indigo-500 dark:text-indigo-400"
                >
                  <Plus className="h-3 w-3" /> Add Question
                </button>
              </div>

              {round.questions.map((question, qIdx) => (
                <div
                  key={qIdx}
                  className="rounded-xl border border-slate-200 bg-slate-50/50 p-4 dark:border-slate-800 dark:bg-slate-850/50"
                >
                  <div className="flex flex-wrap items-center justify-between gap-2 pb-2">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-xs font-bold text-slate-500">
                        Q{qIdx + 1}:
                      </span>

                      {/* Question Type */}
                      <select
                        value={question.questionType}
                        onChange={(e) =>
                          handleUpdateQuestion(
                            rIdx,
                            qIdx,
                            'questionType',
                            e.target.value as QuestionType
                          )
                        }
                        className="rounded-md border border-slate-300 bg-white px-2 py-0.5 text-xs font-semibold text-indigo-700 dark:border-slate-700 dark:bg-slate-800 dark:text-indigo-300"
                      >
                        <option value="CODING">Coding</option>
                        <option value="SYSTEM_DESIGN">System Design</option>
                        <option value="BEHAVIORAL">Behavioral</option>
                        <option value="THEORY">Theory / CS Fundamentals</option>
                        <option value="SCENARIO">Scenario / Case Study</option>
                        <option value="OTHER">Other</option>
                      </select>

                      {/* Difficulty */}
                      <select
                        value={question.difficulty || 'MEDIUM'}
                        onChange={(e) =>
                          handleUpdateQuestion(
                            rIdx,
                            qIdx,
                            'difficulty',
                            e.target.value as Difficulty
                          )
                        }
                        className="rounded-md border border-slate-300 bg-white px-2 py-0.5 text-xs font-semibold text-slate-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
                      >
                        {DIFFICULTIES.map((d) => (
                          <option key={d} value={d}>
                            {d}
                          </option>
                        ))}
                      </select>
                    </div>

                    <button
                      type="button"
                      onClick={() => handleDeleteQuestion(rIdx, qIdx)}
                      className="text-xs font-medium text-rose-500 hover:text-rose-700"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </div>

                  {/* Question Text Textarea */}
                  <textarea
                    rows={2}
                    value={question.text}
                    onChange={(e) =>
                      handleUpdateQuestion(rIdx, qIdx, 'text', e.target.value)
                    }
                    placeholder="Enter question prompt or technical problem details..."
                    className="mt-1 w-full rounded-lg border border-slate-300 bg-white p-2.5 text-xs font-medium leading-relaxed text-slate-900 focus:border-indigo-500 focus:outline-none dark:border-slate-700 dark:bg-slate-800 dark:text-white"
                  />

                  {/* Topics tag editor */}
                  <div className="mt-2.5 flex flex-wrap items-center gap-1.5">
                    <span className="text-[11px] font-semibold text-slate-400">
                      Topics:
                    </span>

                    {/* Active topic pills */}
                    {question.topicSlugs.map((slug) => (
                      <span
                        key={slug}
                        className="inline-flex items-center gap-1 rounded-md bg-white px-2 py-0.5 text-[11px] font-medium text-slate-700 border border-slate-200 shadow-2xs dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
                      >
                        {slug}
                        <button
                          type="button"
                          onClick={() => handleRemoveTopicFromQuestion(rIdx, qIdx, slug)}
                          className="text-slate-400 hover:text-rose-600"
                        >
                          <X className="h-3 w-3" />
                        </button>
                      </span>
                    ))}

                    {/* Quick topic add select */}
                    <select
                      onChange={(e) => {
                        if (e.target.value) {
                          handleAddTopicToQuestion(rIdx, qIdx, e.target.value);
                          e.target.value = '';
                        }
                      }}
                      className="rounded border border-dashed border-slate-300 bg-transparent px-1.5 py-0.5 text-[11px] text-slate-500 hover:border-slate-400 dark:border-slate-700"
                      defaultValue=""
                    >
                      <option value="" disabled>
                        + Add topic tag...
                      </option>
                      {allTopics.map((t) => (
                        <option key={t.slug} value={t.slug}>
                          {t.name}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* Bottom Publish Bar */}
      <div className="mt-10 flex items-center justify-between border-t border-slate-200 pt-6 dark:border-slate-800">
        <Link
          href="/submit"
          className="text-xs font-semibold text-slate-500 hover:text-slate-800"
        >
          Cancel and return
        </Link>

        <button
          onClick={handlePublish}
          disabled={publishing}
          className="flex items-center gap-2 rounded-xl bg-indigo-600 px-8 py-3 text-sm font-bold text-white shadow-md hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:opacity-50 transition-all"
        >
          {publishing ? (
            <>
              <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
              <span>Publishing Experience...</span>
            </>
          ) : (
            <>
              <Send className="h-4 w-4" />
              <span>Confirm &amp; Publish Experience</span>
            </>
          )}
        </button>
      </div>
    </div>
  );
}

