'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { api } from '@/lib/api';
import type {
  Difficulty,
  ExperienceDto,
  ExperienceInputDto,
  InterviewMode,
  Level,
  Outcome,
  PageMetaDto,
  QuestionType,
  RoundType,
  UserDto,
} from '@/lib/types';
import {
  AlertCircle,
  Building2,
  Calendar,
  CheckCircle2,
  Clock,
  Edit3,
  ExternalLink,
  FilePlus2,
  Layers,
  MapPin,
  Plus,
  Sparkles,
  Tag,
  Trash2,
  User,
  X,
  XCircle,
} from 'lucide-react';

const LEVELS: Level[] = ['INTERN', 'JUNIOR', 'MID', 'SENIOR', 'LEAD', 'PRINCIPAL'];
const OUTCOMES: Outcome[] = ['SELECTED', 'REJECTED', 'PENDING', 'UNKNOWN'];
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

export default function MyExperiencesPage() {
  const [profile, setProfile] = useState<UserDto | null>(null);
  const [experiences, setExperiences] = useState<ExperienceDto[]>([]);
  const [pageMeta, setPageMeta] = useState<PageMetaDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Edit modal state
  const [editingExperienceId, setEditingExperienceId] = useState<string | null>(null);
  const [editFormData, setEditFormData] = useState<ExperienceInputDto | null>(null);
  const [savingEdit, setSavingEdit] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

  // Delete modal state
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [userProfile, myExps] = await Promise.all([
        api.getCurrentUser().catch(() => null),
        api.listMyExperiences(),
      ]);
      setProfile(userProfile);
      setExperiences(myExps.content);
      setPageMeta(myExps.page);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load user experiences';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Open Edit Modal
  const handleOpenEdit = (exp: ExperienceDto) => {
    setEditingExperienceId(exp.id);
    setEditError(null);
    setEditFormData({
      companyName: exp.company?.name || '',
      roleTitle: exp.roleTitle || '',
      level: exp.level || 'MID',
      yearsOfExperience: exp.yearsOfExperience ?? null,
      location: exp.location || '',
      interviewYear: exp.interviewYear || new Date().getFullYear(),
      interviewMonth: exp.interviewMonth || new Date().getMonth() + 1,
      interviewMode: (exp.interviewMode as string) === 'VIRTUAL' ? 'REMOTE' : (exp.interviewMode || 'REMOTE'),
      outcome: (exp.outcome as string) === 'OFFER' ? 'SELECTED' : (exp.outcome || 'PENDING'),
      summary: exp.summary || '',
      isAnonymous: exp.isAnonymous ?? false,
      rounds: (exp.rounds || []).map((r) => ({
        roundNumber: r.roundNumber,
        roundType: r.roundType,
        durationMinutes: r.durationMinutes ?? 45,
        notes: r.notes || '',
        questions: (r.questions || []).map((q) => ({
          text: q.text,
          questionType: q.questionType,
          difficulty: q.difficulty || 'MEDIUM',
          topicSlugs: (q.topics || []).map((t) => t.slug),
        })),
      })),
    });
  };

  // Save Edit
  const handleSaveEdit = async () => {
    if (!editingExperienceId || !editFormData) return;
    if (!editFormData.companyName.trim()) {
      setEditError('Company name is required.');
      return;
    }

    setSavingEdit(true);
    setEditError(null);
    try {
      const updated = await api.updateExperience(editingExperienceId, editFormData);
      setExperiences((prev) =>
        prev.map((e) => (e.id === updated.id ? updated : e))
      );
      setEditingExperienceId(null);
      setEditFormData(null);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to save changes';
      setEditError(msg);
    } finally {
      setSavingEdit(false);
    }
  };

  // Delete Experience
  const handleDeleteExperience = async (id: string) => {
    try {
      await api.deleteExperience(id);
      setExperiences((prev) => prev.filter((e) => e.id !== id));
      setDeletingId(null);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to delete experience';
      alert(msg);
    }
  };

  const getOutcomeBadge = (outcome: Outcome) => {
    switch (outcome) {
      case 'SELECTED':
      case ('OFFER' as any):
        return (
          <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-semibold text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300">
            <CheckCircle2 className="h-3 w-3" /> Offer Received
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
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      {/* User Profile Header */}
      <div className="mb-8 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-indigo-600 text-white shadow-md font-bold text-xl">
              {profile?.displayName?.charAt(0).toUpperCase() || 'U'}
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white">
                {profile?.displayName || 'My Profile'}
              </h1>
              <p className="text-xs text-slate-500">
                Author ID: {profile?.id || 'Connected'} &middot; {experiences.length}{' '}
                {experiences.length === 1 ? 'Submission' : 'Submissions'}
              </p>
            </div>
          </div>

          <Link
            href="/submit"
            className="flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 transition-colors"
          >
            <FilePlus2 className="h-4 w-4" />
            <span>Share Another Experience</span>
          </Link>
        </div>
      </div>

      {/* Submissions Section */}
      <div className="mb-6 flex items-center justify-between">
        <h2 className="text-lg font-bold text-slate-900 dark:text-white">
          My Published Experiences
        </h2>
      </div>

      {error && (
        <div className="mb-6 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800 dark:border-rose-900 dark:bg-rose-950/50 dark:text-rose-300">
          <p>{error}</p>
        </div>
      )}

      {loading ? (
        <div className="space-y-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <div
              key={i}
              className="animate-pulse rounded-2xl border border-slate-200 bg-white p-6 dark:border-slate-800 dark:bg-slate-900"
            >
              <div className="h-5 w-1/4 rounded bg-slate-200 dark:bg-slate-800" />
              <div className="mt-3 h-10 w-full rounded bg-slate-100 dark:bg-slate-850" />
            </div>
          ))}
        </div>
      ) : experiences.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-300 p-12 text-center dark:border-slate-800">
          <FilePlus2 className="mx-auto h-12 w-12 text-slate-400" />
          <h3 className="mt-4 text-base font-semibold text-slate-900 dark:text-white">
            You haven&apos;t shared any interview experiences yet
          </h3>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
            Help fellow developers prepare by submitting your interview questions and rounds.
          </p>
          <Link
            href="/submit"
            className="mt-5 inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500"
          >
            <Plus className="h-4 w-4" /> Submit Your First Experience
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {experiences.map((exp) => (
            <div
              key={exp.id}
              className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900"
            >
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-lg font-bold text-slate-900 dark:text-white">
                      {exp.company?.name}
                    </span>
                    <span className="text-slate-300 dark:text-slate-700">&middot;</span>
                    <span className="text-sm font-medium text-slate-700 dark:text-slate-300">
                      {exp.roleTitle}
                    </span>
                  </div>

                  <div className="mt-1.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
                    {exp.level && <span className="font-semibold">{exp.level}</span>}
                    {exp.interviewYear && (
                      <span>
                        Interviewed{' '}
                        {exp.interviewMonth
                          ? `${exp.interviewMonth}/${exp.interviewYear}`
                          : exp.interviewYear}
                      </span>
                    )}
                    <span>{exp.isAnonymous ? 'Anonymous' : 'Public attribution'}</span>
                    <span>Posted {new Date(exp.createdAt).toLocaleDateString()}</span>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  {getOutcomeBadge(exp.outcome)}

                  {/* Actions */}
                  <div className="flex items-center gap-1.5 pl-2 border-l border-slate-200 dark:border-slate-800">
                    <Link
                      href={`/experiences/${exp.id}`}
                      className="rounded-lg p-2 text-slate-500 hover:text-indigo-600 hover:bg-slate-100 dark:hover:bg-slate-800 dark:hover:text-indigo-400 transition-colors"
                      title="View public page"
                    >
                      <ExternalLink className="h-4 w-4" />
                    </Link>

                    <button
                      onClick={() => handleOpenEdit(exp)}
                      className="rounded-lg p-2 text-slate-500 hover:text-indigo-600 hover:bg-slate-100 dark:hover:bg-slate-800 dark:hover:text-indigo-400 transition-colors"
                      title="Edit experience"
                    >
                      <Edit3 className="h-4 w-4" />
                    </button>

                    <button
                      onClick={() => setDeletingId(exp.id)}
                      className="rounded-lg p-2 text-slate-500 hover:text-rose-600 hover:bg-slate-100 dark:hover:bg-slate-800 dark:hover:text-rose-400 transition-colors"
                      title="Delete experience"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>

              {exp.summary && (
                <p className="mt-3 text-xs text-slate-600 line-clamp-2 dark:text-slate-300">
                  {exp.summary}
                </p>
              )}

              <div className="mt-4 flex items-center gap-4 text-xs font-medium text-slate-500 border-t border-slate-100 pt-3 dark:border-slate-800">
                <span className="flex items-center gap-1">
                  <Layers className="h-3.5 w-3.5 text-indigo-500" />
                  {exp.rounds?.length || 0} Rounds
                </span>
                <span className="flex items-center gap-1">
                  <Sparkles className="h-3.5 w-3.5 text-indigo-500" />
                  {(exp.rounds || []).reduce(
                    (acc, r) => acc + (r.questions?.length || 0),
                    0
                  )}{' '}
                  Questions
                </span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deletingId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl dark:bg-slate-900">
            <h3 className="text-lg font-bold text-slate-900 dark:text-white">
              Delete Interview Experience?
            </h3>
            <p className="mt-2 text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
              Are you sure you want to delete this experience? All associated rounds and questions will be permanently removed.
            </p>
            <div className="mt-6 flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setDeletingId(null)}
                className="rounded-lg px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => handleDeleteExperience(deletingId)}
                className="rounded-lg bg-rose-600 px-4 py-2 text-xs font-semibold text-white shadow-sm hover:bg-rose-500"
              >
                Yes, Delete Experience
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Full Edit Modal */}
      {editingExperienceId && editFormData && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 overflow-y-auto">
          <div className="my-8 max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-2xl bg-white p-6 shadow-2xl dark:bg-slate-900">
            <div className="flex items-center justify-between border-b border-slate-200 pb-4 dark:border-slate-800">
              <h3 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <Edit3 className="h-5 w-5 text-indigo-600" />
                <span>Edit Interview Experience</span>
              </h3>
              <button
                onClick={() => setEditingExperienceId(null)}
                className="rounded-lg p-1 text-slate-400 hover:text-slate-600"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {editError && (
              <div className="mt-4 rounded-lg bg-rose-50 p-3 text-xs text-rose-800 border border-rose-200">
                {editError}
              </div>
            )}

            <div className="mt-4 space-y-4">
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
                    Company Name
                  </label>
                  <input
                    type="text"
                    value={editFormData.companyName}
                    onChange={(e) =>
                      setEditFormData({ ...editFormData, companyName: e.target.value })
                    }
                    className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2 text-xs dark:border-slate-700 dark:bg-slate-800"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
                    Role Title
                  </label>
                  <input
                    type="text"
                    value={editFormData.roleTitle || ''}
                    onChange={(e) =>
                      setEditFormData({ ...editFormData, roleTitle: e.target.value })
                    }
                    className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2 text-xs dark:border-slate-700 dark:bg-slate-800"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
                    Seniority Level
                  </label>
                  <select
                    value={editFormData.level || 'MID'}
                    onChange={(e) =>
                      setEditFormData({
                        ...editFormData,
                        level: e.target.value as Level,
                      })
                    }
                    className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2 text-xs dark:border-slate-700 dark:bg-slate-800"
                  >
                    {LEVELS.map((l) => (
                      <option key={l} value={l}>
                        {l}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
                    Outcome
                  </label>
                  <select
                    value={editFormData.outcome}
                    onChange={(e) =>
                      setEditFormData({
                        ...editFormData,
                        outcome: e.target.value as Outcome,
                      })
                    }
                    className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2 text-xs dark:border-slate-700 dark:bg-slate-800"
                  >
                    {OUTCOMES.map((o) => (
                      <option key={o} value={o}>
                        {o}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
                  Summary
                </label>
                <textarea
                  rows={3}
                  value={editFormData.summary || ''}
                  onChange={(e) =>
                    setEditFormData({ ...editFormData, summary: e.target.value })
                  }
                  className="mt-1 block w-full rounded-lg border border-slate-300 p-2.5 text-xs dark:border-slate-700 dark:bg-slate-800"
                />
              </div>

              {/* Rounds count & notice */}
              <div className="rounded-lg bg-slate-50 p-3 text-xs text-slate-600 dark:bg-slate-800">
                <span>Rounds included: {editFormData.rounds.length}.</span>
              </div>
            </div>

            <div className="mt-6 flex justify-end gap-3 border-t border-slate-200 pt-4 dark:border-slate-800">
              <button
                type="button"
                onClick={() => setEditingExperienceId(null)}
                className="rounded-lg px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSaveEdit}
                disabled={savingEdit}
                className="rounded-lg bg-indigo-600 px-5 py-2 text-xs font-semibold text-white shadow-sm hover:bg-indigo-500 disabled:opacity-50"
              >
                {savingEdit ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

