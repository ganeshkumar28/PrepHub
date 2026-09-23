'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/api';
import {
  AlertCircle,
  ArrowRight,
  CheckCircle2,
  FileText,
  Lightbulb,
  Sparkles,
} from 'lucide-react';

const MIN_WORDS = 80;
const MIN_CHARS = 400;

export default function SubmitPage() {
  const router = useRouter();
  const [rawText, setRawText] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const wordCount = rawText.trim()
    ? rawText.trim().split(/\s+/).filter(Boolean).length
    : 0;
  const charCount = rawText.trim().length;
  const isTooShort = wordCount < MIN_WORDS && charCount < MIN_CHARS;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isTooShort) {
      setError(
        `Please provide a bit more detail (at least ${MIN_WORDS} words or ${MIN_CHARS} characters) so our AI can accurately extract rounds, questions, and topics.`
      );
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const job = await api.createExtraction(rawText);
      // Immediately navigate to the preview/edit screen
      router.push(`/submit/${job.id}`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to process interview extraction';
      setError(msg);
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6 lg:px-8">
      {/* Header */}
      <div className="mb-8">
        <div className="inline-flex items-center gap-1.5 rounded-full bg-indigo-50 px-3 py-1 text-xs font-semibold text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300">
          <Sparkles className="h-3.5 w-3.5" /> AI-Powered Extraction
        </div>
        <h1 className="mt-3 text-3xl font-extrabold tracking-tight text-slate-900 sm:text-4xl dark:text-white">
          Share Your Interview Experience
        </h1>
        <p className="mt-2 text-base text-slate-600 dark:text-slate-400">
          Paste your raw notes, email drafts, or interview breakdown. Our AI will automatically parse rounds, questions, and topics into a structured form that you can edit and verify before publishing.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-3">
        {/* Main Form (2 cols) */}
        <div className="lg:col-span-2">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900">
              <label
                htmlFor="interview-text"
                className="flex items-center justify-between text-sm font-semibold text-slate-900 dark:text-white"
              >
                <span>Raw Interview Text</span>
                <span
                  className={`text-xs font-medium ${
                    isTooShort ? 'text-amber-600 dark:text-amber-400' : 'text-emerald-600 dark:text-emerald-400'
                  }`}
                >
                  {wordCount} words ({charCount} chars) / min ~{MIN_CHARS} chars
                </span>
              </label>

              <textarea
                id="interview-text"
                rows={15}
                value={rawText}
                onChange={(e) => setRawText(e.target.value)}
                placeholder="Example:
I interviewed at Google for an L5 Senior Software Engineer role in Mountain View in May 2024.
Offer received.

Round 1: Screening (45 min)
Asked to design a data structure that supports insert, delete, and getRandom in O(1) time. We discussed hash maps with array swap trick.

Round 2: Coding & Algorithms (45 min)
Given a binary tree, serialize and deserialize it. Follow-up: what if node values are large strings?

Round 3: System Design (45 min)
Design a URL shortener like bit.ly capable of handling 500M new URLs per month..."
                className="mt-3 w-full rounded-xl border border-slate-300 bg-slate-50 p-4 font-mono text-sm leading-relaxed text-slate-900 placeholder:text-slate-400 focus:border-indigo-500 focus:bg-white focus:text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700 dark:bg-slate-850 dark:text-white dark:focus:bg-slate-900 dark:focus:text-white"
              />

              {/* Error box */}
              {error && (
                <div className="mt-4 flex items-start gap-2.5 rounded-xl border border-rose-200 bg-rose-50 p-3.5 text-xs text-rose-800 dark:border-rose-900 dark:bg-rose-950/50 dark:text-rose-300">
                  <AlertCircle className="h-4 w-4 shrink-0 text-rose-600 dark:text-rose-400 mt-0.5" />
                  <p className="leading-relaxed">{error}</p>
                </div>
              )}

              {/* Submit CTA */}
              <div className="mt-5 flex items-center justify-between border-t border-slate-100 pt-4 dark:border-slate-800">
                <span className="text-xs text-slate-500">
                  You will review and edit every single field on the next screen.
                </span>

                <button
                  type="submit"
                  disabled={loading || isTooShort}
                  className="flex items-center gap-2 rounded-xl bg-indigo-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                >
                  {loading ? (
                    <>
                      <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                      <span>Extracting with AI...</span>
                    </>
                  ) : (
                    <>
                      <span>Extract &amp; Review</span>
                      <ArrowRight className="h-4 w-4" />
                    </>
                  )}
                </button>
              </div>
            </div>
          </form>
        </div>

        {/* Tips sidebar (1 col) */}
        <div className="space-y-4">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900">
            <div className="flex items-center gap-2 text-sm font-bold text-slate-900 dark:text-white">
              <Lightbulb className="h-4 w-4 text-amber-500" />
              <span>What to include</span>
            </div>
            <ul className="mt-3 space-y-2.5 text-xs text-slate-600 dark:text-slate-400">
              <li className="flex items-start gap-2">
                <CheckCircle2 className="h-3.5 w-3.5 shrink-0 text-emerald-500 mt-0.5" />
                <span>
                  <strong>Company &amp; Role</strong>: Target company, position title, and seniority level (e.g. Senior Backend Engineer).
                </span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle2 className="h-3.5 w-3.5 shrink-0 text-emerald-500 mt-0.5" />
                <span>
                  <strong>Rounds Breakdown</strong>: Separate your text by rounds (Coding, System Design, Behavioral, HR).
                </span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle2 className="h-3.5 w-3.5 shrink-0 text-emerald-500 mt-0.5" />
                <span>
                  <strong>Actual Questions</strong>: Specific problems or prompts asked by the interviewers.
                </span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle2 className="h-3.5 w-3.5 shrink-0 text-emerald-500 mt-0.5" />
                <span>
                  <strong>Outcome &amp; Location</strong>: Offer, rejected, or withdrawn, plus date and whether it was virtual or onsite.
                </span>
              </li>
            </ul>
          </div>

          <div className="rounded-2xl border border-indigo-100 bg-indigo-50/60 p-5 dark:border-indigo-950 dark:bg-indigo-950/30">
            <div className="flex items-center gap-2 text-xs font-bold text-indigo-900 dark:text-indigo-300">
              <Sparkles className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
              <span>Quality Control Guarantee</span>
            </div>
            <p className="mt-2 text-xs text-indigo-800 dark:text-indigo-300 leading-relaxed">
              Nothing is published immediately. After extraction, you get a fully interactive form where you can edit, correct, add, or delete any company detail, round, question, and topic.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
