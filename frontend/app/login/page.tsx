'use client';

import { Suspense, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { createClient } from '@/lib/supabase/client';
import {
  AlertCircle,
  ArrowRight,
  CheckCircle2,
  Code2,
  Lock,
  Mail,
  UserPlus,
} from 'lucide-react';

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const next = searchParams.get('next') || '/';

  // Mode: 'signin' or 'signup'
  const [activeTab, setActiveTab] = useState<'signin' | 'signup'>('signin');

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  // Validation errors
  const [emailError, setEmailError] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [confirmPasswordError, setConfirmPasswordError] = useState<string | null>(null);

  const [loading, setLoading] = useState(false);
  const [generalMessage, setGeneralMessage] = useState<{
    type: 'success' | 'error' | 'account-not-found';
    text: string;
  } | null>(null);

  // Switch tab cleanly
  const switchTab = (tab: 'signin' | 'signup') => {
    setActiveTab(tab);
    setPassword('');
    setConfirmPassword('');
    setEmailError(null);
    setPasswordError(null);
    setConfirmPasswordError(null);
    setGeneralMessage(null);
  };

  // Client validation
  const validateForm = (): boolean => {
    let isValid = true;
    setEmailError(null);
    setPasswordError(null);
    setConfirmPasswordError(null);

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email.trim()) {
      setEmailError('Email address is required.');
      isValid = false;
    } else if (!emailRegex.test(email.trim())) {
      setEmailError('Please enter a valid email address (e.g., name@company.com).');
      isValid = false;
    }

    if (!password) {
      setPasswordError('Password is required.');
      isValid = false;
    } else if (activeTab === 'signup') {
      if (password.length < 8) {
        setPasswordError('Password must be at least 8 characters long.');
        isValid = false;
      } else if (!/[A-Z]/.test(password)) {
        setPasswordError('Password must contain at least one uppercase letter (A-Z).');
        isValid = false;
      } else if (!/[a-z]/.test(password)) {
        setPasswordError('Password must contain at least one lowercase letter (a-z).');
        isValid = false;
      } else if (!/[0-9]/.test(password)) {
        setPasswordError('Password must contain at least one number (0-9).');
        isValid = false;
      } else if (!/[^A-Za-z0-9]/.test(password)) {
        setPasswordError('Password must contain at least one special character (e.g., !@#$%^&*).');
        isValid = false;
      }
    } else if (password.length < 6) {
      setPasswordError('Password must be at least 6 characters long.');
      isValid = false;
    }

    if (activeTab === 'signup') {
      if (!confirmPassword) {
        setConfirmPasswordError('Please confirm your password.');
        isValid = false;
      } else if (password !== confirmPassword) {
        setConfirmPasswordError('Passwords do not match.');
        isValid = false;
      }
    }

    return isValid;
  };

  const handleSignIn = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;

    setLoading(true);
    setGeneralMessage(null);

    try {
      const supabase = createClient();
      const { error } = await supabase.auth.signInWithPassword({
        email: email.trim(),
        password,
      });

      if (error) {
        if (
          error.message.toLowerCase().includes('invalid login credentials') ||
          error.message.toLowerCase().includes('invalid grant')
        ) {
          setGeneralMessage({
            type: 'account-not-found',
            text:
              "Invalid email or password. If you haven't created an account yet, please click 'Create an Account' below before signing in.",
          });
        } else {
          setGeneralMessage({ type: 'error', text: error.message });
        }
      } else {
        router.push(next);
        router.refresh();
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Sign in failed';
      setGeneralMessage({ type: 'error', text: msg });
    } finally {
      setLoading(false);
    }
  };

  const handleSignUp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;

    setLoading(true);
    setGeneralMessage(null);

    try {
      const supabase = createClient();
      const { data, error } = await supabase.auth.signUp({
        email: email.trim(),
        password,
      });

      if (error) {
        setGeneralMessage({ type: 'error', text: error.message });
      } else {
        // Check if user is automatically logged in or needs email confirmation
        if (data.session) {
          router.push(next);
          router.refresh();
        } else {
          // Switch to sign in tab cleanly with email preserved
          setActiveTab('signin');
          setPassword('');
          setConfirmPassword('');
          setGeneralMessage({
            type: 'success',
            text:
              'Account successfully created! Please enter your password to sign in (or verify your email if confirmation was sent).',
          });
        }
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Sign up failed';
      setGeneralMessage({ type: 'error', text: msg });
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSignIn = async () => {
    setLoading(true);
    setGeneralMessage(null);

    try {
      const supabase = createClient();
      const redirectUrl = `${window.location.origin}/auth/callback?next=${encodeURIComponent(next)}`;
      const { error } = await supabase.auth.signInWithOAuth({
        provider: 'google',
        options: {
          redirectTo: redirectUrl,
        },
      });

      if (error) {
        setGeneralMessage({
          type: 'error',
          text:
            'Google Sign-In is not enabled yet in your Supabase project. Please use Email & Password below or enable Google in Supabase Dashboard → Authentication → Providers.',
        });
        setLoading(false);
      }
    } catch (err: unknown) {
      const errorMsg =
        err instanceof Error
          ? err.message
          : 'Google sign in requires enabling Google OAuth in Supabase.';
      setGeneralMessage({
        type: 'error',
        text:
          'Google Provider is not enabled in Supabase yet. Please sign in with Email & Password below.',
      });
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto max-w-md px-4 py-16 sm:px-6">
      <div className="rounded-2xl border border-slate-200 bg-white p-8 shadow-sm dark:border-slate-800 dark:bg-slate-900">
        {/* Brand Icon & Heading */}
        <div className="text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-indigo-600 text-white shadow-md">
            <Code2 className="h-6 w-6" />
          </div>
          <h1 className="mt-4 text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            {activeTab === 'signin' ? 'Sign In to PrepHub' : 'Create an Account'}
          </h1>
          <p className="mt-1.5 text-xs text-slate-600 dark:text-slate-400">
            {activeTab === 'signin'
              ? 'Enter your email and password to access your account'
              : 'Sign up to share interview experiences and track your prep'}
          </p>
        </div>

        {/* Prominent Tab Switcher */}
        <div className="mt-6 flex rounded-xl bg-slate-100 p-1 dark:bg-slate-800">
          <button
            type="button"
            onClick={() => switchTab('signin')}
            className={`flex-1 rounded-lg py-2 text-xs font-bold transition-all ${
              activeTab === 'signin'
                ? 'bg-white text-indigo-700 shadow-sm dark:bg-slate-750 dark:text-white'
                : 'text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'
            }`}
          >
            Sign In
          </button>
          <button
            type="button"
            onClick={() => switchTab('signup')}
            className={`flex-1 rounded-lg py-2 text-xs font-bold transition-all ${
              activeTab === 'signup'
                ? 'bg-white text-indigo-700 shadow-sm dark:bg-slate-750 dark:text-white'
                : 'text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-white'
            }`}
          >
            Create Account
          </button>
        </div>

        {/* Notification messages */}
        {generalMessage && (
          <div
            className={`mt-5 rounded-xl p-3.5 text-xs ${
              generalMessage.type === 'success'
                ? 'bg-emerald-50 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800'
                : generalMessage.type === 'account-not-found'
                ? 'bg-amber-50 text-amber-900 dark:bg-amber-950/60 dark:text-amber-300 border border-amber-200 dark:border-amber-800'
                : 'bg-rose-50 text-rose-800 dark:bg-rose-950/60 dark:text-rose-300 border border-rose-200 dark:border-rose-800'
            }`}
          >
            <div className="flex items-start gap-2.5">
              {generalMessage.type === 'success' ? (
                <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600 dark:text-emerald-400 mt-0.5" />
              ) : (
                <AlertCircle className="h-4 w-4 shrink-0 text-rose-600 dark:text-rose-400 mt-0.5" />
              )}
              <div className="flex-1">
                <p className="leading-relaxed">{generalMessage.text}</p>
                {generalMessage.type === 'account-not-found' && (
                  <button
                    type="button"
                    onClick={() => switchTab('signup')}
                    className="mt-2 inline-flex items-center gap-1 font-bold text-indigo-700 hover:underline dark:text-indigo-300"
                  >
                    <span>Click here to create an account now</span>
                    <ArrowRight className="h-3 w-3" />
                  </button>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Google OAuth Button */}
        <div className="mt-5">
          <button
            type="button"
            onClick={handleGoogleSignIn}
            disabled={loading}
            className="flex w-full items-center justify-center gap-3 rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-xs font-semibold text-slate-700 shadow-2xs hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:opacity-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-750 transition-colors"
          >
            <svg className="h-4 w-4" viewBox="0 0 24 24">
              <path
                fill="#4285F4"
                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
              />
              <path
                fill="#34A853"
                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
              />
              <path
                fill="#FBBC05"
                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
              />
              <path
                fill="#EA4335"
                d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
              />
            </svg>
            <span>Continue with Google</span>
          </button>
        </div>

        {/* Divider */}
        <div className="relative my-5">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-slate-200 dark:border-slate-800" />
          </div>
          <div className="relative flex justify-center text-xs uppercase">
            <span className="bg-white px-3 text-[11px] font-semibold text-slate-400 dark:bg-slate-900">
              Or with email &amp; password
            </span>
          </div>
        </div>

        {/* Form */}
        <form
          onSubmit={activeTab === 'signin' ? handleSignIn : handleSignUp}
          className="space-y-4"
          noValidate
        >
          {/* Email field */}
          <div>
            <label
              htmlFor="email"
              className="block text-xs font-semibold text-slate-700 dark:text-slate-300"
            >
              Email address <span className="text-rose-500">*</span>
            </label>
            <div className="relative mt-1">
              <Mail className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  if (emailError) setEmailError(null);
                }}
                placeholder="you@company.com"
                className={`block w-full rounded-xl border bg-white py-2 pl-9 pr-3 text-xs placeholder:text-slate-400 focus:outline-none focus:ring-2 dark:bg-slate-800 dark:text-white ${
                  emailError
                    ? 'border-rose-300 focus:border-rose-500 focus:ring-rose-500/20'
                    : 'border-slate-300 focus:border-indigo-500 focus:ring-indigo-500/20 dark:border-slate-700'
                }`}
              />
            </div>
            {emailError && (
              <p className="mt-1 text-[11px] font-medium text-rose-600 dark:text-rose-400">
                {emailError}
              </p>
            )}
          </div>

          {/* Password field */}
          <div>
            <label
              htmlFor="password"
              className="block text-xs font-semibold text-slate-700 dark:text-slate-300"
            >
              Password <span className="text-rose-500">*</span>
            </label>
            <div className="relative mt-1">
              <Lock className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  if (passwordError) setPasswordError(null);
                }}
                placeholder={activeTab === 'signup' ? 'Create a strong password' : 'Enter your password'}
                className={`block w-full rounded-xl border bg-white py-2 pl-9 pr-3 text-xs placeholder:text-slate-400 focus:outline-none focus:ring-2 dark:bg-slate-800 dark:text-white ${
                  passwordError
                    ? 'border-rose-300 focus:border-rose-500 focus:ring-rose-500/20'
                    : 'border-slate-300 focus:border-indigo-500 focus:ring-indigo-500/20 dark:border-slate-700'
                }`}
              />
            </div>
            {passwordError && (
              <p className="mt-1 text-[11px] font-medium text-rose-600 dark:text-rose-400">
                {passwordError}
              </p>
            )}

            {/* Real-time Password Rules Checklist for Sign Up */}
            {activeTab === 'signup' && (
              <div className="mt-2.5 rounded-lg border border-slate-200/80 bg-slate-50/70 p-2.5 text-[11px] dark:border-slate-800 dark:bg-slate-850">
                <p className="font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                  Password requirements:
                </p>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5 text-slate-500 dark:text-slate-400">
                  <div className={`flex items-center gap-1.5 ${password.length >= 8 ? 'text-emerald-600 dark:text-emerald-400 font-medium' : ''}`}>
                    <CheckCircle2 className={`h-3 w-3 shrink-0 ${password.length >= 8 ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-300 dark:text-slate-600'}`} />
                    <span>8+ characters</span>
                  </div>
                  <div className={`flex items-center gap-1.5 ${/[A-Z]/.test(password) ? 'text-emerald-600 dark:text-emerald-400 font-medium' : ''}`}>
                    <CheckCircle2 className={`h-3 w-3 shrink-0 ${/[A-Z]/.test(password) ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-300 dark:text-slate-600'}`} />
                    <span>1 uppercase letter (A-Z)</span>
                  </div>
                  <div className={`flex items-center gap-1.5 ${/[a-z]/.test(password) ? 'text-emerald-600 dark:text-emerald-400 font-medium' : ''}`}>
                    <CheckCircle2 className={`h-3 w-3 shrink-0 ${/[a-z]/.test(password) ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-300 dark:text-slate-600'}`} />
                    <span>1 lowercase letter (a-z)</span>
                  </div>
                  <div className={`flex items-center gap-1.5 ${/[0-9]/.test(password) ? 'text-emerald-600 dark:text-emerald-400 font-medium' : ''}`}>
                    <CheckCircle2 className={`h-3 w-3 shrink-0 ${/[0-9]/.test(password) ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-300 dark:text-slate-600'}`} />
                    <span>1 number (0-9)</span>
                  </div>
                  <div className={`flex items-center gap-1.5 sm:col-span-2 ${/[^A-Za-z0-9]/.test(password) ? 'text-emerald-600 dark:text-emerald-400 font-medium' : ''}`}>
                    <CheckCircle2 className={`h-3 w-3 shrink-0 ${/[^A-Za-z0-9]/.test(password) ? 'text-emerald-600 dark:text-emerald-400' : 'text-slate-300 dark:text-slate-600'}`} />
                    <span>1 special character (e.g. !@#$%^&*)</span>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Confirm Password field on Sign Up */}
          {activeTab === 'signup' && (
            <div>
              <label
                htmlFor="confirm-password"
                className="block text-xs font-semibold text-slate-700 dark:text-slate-300"
              >
                Confirm Password <span className="text-rose-500">*</span>
              </label>
              <div className="relative mt-1">
                <Lock className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                <input
                  id="confirm-password"
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => {
                    setConfirmPassword(e.target.value);
                    if (confirmPasswordError) setConfirmPasswordError(null);
                  }}
                  placeholder="Re-enter your password"
                  className={`block w-full rounded-xl border bg-white py-2 pl-9 pr-3 text-xs placeholder:text-slate-400 focus:outline-none focus:ring-2 dark:bg-slate-800 dark:text-white ${
                    confirmPasswordError
                      ? 'border-rose-300 focus:border-rose-500 focus:ring-rose-500/20'
                      : 'border-slate-300 focus:border-indigo-500 focus:ring-indigo-500/20 dark:border-slate-700'
                  }`}
                />
              </div>
              {confirmPasswordError && (
                <p className="mt-1 text-[11px] font-medium text-rose-600 dark:text-rose-400">
                  {confirmPasswordError}
                </p>
              )}
            </div>
          )}

          {/* Submit Button */}
          <button
            type="submit"
            disabled={loading}
            className="flex w-full items-center justify-center gap-2 rounded-xl bg-indigo-600 px-4 py-2.5 text-xs font-bold text-white shadow-sm hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:opacity-50 transition-all mt-2"
          >
            {loading ? (
              <div className="flex items-center gap-2">
                <div className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-white border-t-transparent" />
                <span>{activeTab === 'signin' ? 'Signing in...' : 'Creating account...'}</span>
              </div>
            ) : (
              <>
                <span>{activeTab === 'signin' ? 'Sign In' : 'Create Account'}</span>
                <ArrowRight className="h-4 w-4" />
              </>
            )}
          </button>

          {/* Quick Switch Text Link */}
          <div className="text-center pt-2">
            {activeTab === 'signin' ? (
              <p className="text-xs text-slate-500">
                Don&apos;t have an account yet?{' '}
                <button
                  type="button"
                  onClick={() => switchTab('signup')}
                  className="font-bold text-indigo-600 hover:text-indigo-500 dark:text-indigo-400"
                >
                  Create an account
                </button>
              </p>
            ) : (
              <p className="text-xs text-slate-500">
                Already have an account?{' '}
                <button
                  type="button"
                  onClick={() => switchTab('signin')}
                  className="font-bold text-indigo-600 hover:text-indigo-500 dark:text-indigo-400"
                >
                  Sign in instead
                </button>
              </p>
            )}
          </div>
        </form>
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense
      fallback={
        <div className="flex h-96 items-center justify-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-indigo-600 border-t-transparent" />
        </div>
      }
    >
      <LoginForm />
    </Suspense>
  );
}
