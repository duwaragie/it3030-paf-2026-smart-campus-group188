import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { AuthLayout } from '../components/AuthLayout';
import { authService } from '@/services/authService';

const passwordPolicy = z
  .string()
  .min(8, 'Must be at least 8 characters')
  .regex(/[A-Z]/, 'Must include an uppercase letter')
  .regex(/[a-z]/, 'Must include a lowercase letter')
  .regex(/\d/, 'Must include a digit')
  .regex(/[^A-Za-z0-9\s]/, 'Must include a special character');

const schema = z
  .object({
    password: passwordPolicy,
    confirmPassword: z.string(),
  })
  .refine((d) => d.password === d.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

type FormValues = z.infer<typeof schema>;

interface InviteInfo {
  email: string;
  name: string;
  role: string;
}

export default function AccountSetupPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token') ?? '';

  const [invite, setInvite] = useState<InviteInfo | null>(null);
  const [validating, setValidating] = useState(true);
  const [validationError, setValidationError] = useState<string | null>(null);

  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (!token) {
      setValidating(false);
      setValidationError('This setup link is missing a token.');
      return;
    }
    authService
      .validateAccountSetup(token)
      .then((res) => setInvite(res.data))
      .catch((err) => {
        const e = err as { response?: { data?: { message?: string; error?: string } } };
        setValidationError(
          e.response?.data?.message || e.response?.data?.error || 'This setup link is invalid or has expired.'
        );
      })
      .finally(() => setValidating(false));
  }, [token]);

  const onSubmit = async (data: FormValues) => {
    try {
      setSubmitting(true);
      setSubmitError(null);
      await authService.completeAccountSetup(token, data.password);
      setSuccess(true);
      setTimeout(() => navigate('/login'), 2500);
    } catch (err) {
      const e = err as { response?: { data?: { message?: string; error?: string } } };
      setSubmitError(
        e.response?.data?.message || e.response?.data?.error || 'Failed to complete account setup.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  const layoutProps = {
    title: '',
    description: '',
    heading: 'Securing your academic journey.',
    subheading:
      'Set a strong password to activate your staff account. Your email is verified when you finish.',
  };

  if (validating) {
    return (
      <AuthLayout {...layoutProps}>
        <div className="space-y-6">
          <p className="text-[15px] text-gray-500">Checking your invitation...</p>
        </div>
      </AuthLayout>
    );
  }

  if (validationError || !invite) {
    return (
      <AuthLayout {...layoutProps}>
        <div className="space-y-6">
          <h2 className="text-[1.75rem] font-bold tracking-tight text-campus-900">Invalid link</h2>
          <p className="text-[15px] text-gray-500">{validationError}</p>
          <Link to="/login" className="inline-flex items-center gap-1.5 text-sm font-semibold text-campus-800 hover:text-campus-600 transition-colors">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path d="M19 12H5M12 19l-7-7 7-7" />
            </svg>
            Back to login
          </Link>
        </div>
      </AuthLayout>
    );
  }

  const EyeToggle = () => (
    <button
      type="button"
      onClick={() => setShowPassword(!showPassword)}
      className="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
    >
      <svg className="w-[18px] h-[18px]" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
        {showPassword ? (
          <>
            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
            <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
            <line x1="1" y1="1" x2="23" y2="23" />
          </>
        ) : (
          <>
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
            <circle cx="12" cy="12" r="3" />
          </>
        )}
      </svg>
    </button>
  );

  return (
    <AuthLayout {...layoutProps}>
      <div className="space-y-6">
        <div className="space-y-2">
          <h2 className="text-[1.75rem] font-bold tracking-tight text-campus-900">Set up your account</h2>
          <p className="text-[15px] text-gray-500">
            Welcome, <span className="font-semibold text-campus-700">{invite.name}</span>. You are
            setting up the <span className="font-semibold text-campus-700">{invite.role}</span> account
            for <span className="font-semibold text-campus-700">{invite.email}</span>.
          </p>
        </div>

        {success ? (
          <div className="flex items-center gap-2.5 bg-emerald-50 text-emerald-700 p-3.5 rounded-xl text-sm border border-emerald-100">
            <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
            Account activated. Redirecting to sign in...
          </div>
        ) : (
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            {submitError && (
              <div className="flex items-center gap-2.5 bg-red-50 text-red-600 p-3.5 rounded-xl text-sm border border-red-100">
                <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <circle cx="12" cy="12" r="10" />
                  <line x1="15" y1="9" x2="9" y2="15" />
                  <line x1="9" y1="9" x2="15" y2="15" />
                </svg>
                {submitError}
              </div>
            )}

            <div className="space-y-2">
              <label htmlFor="password" className="block text-[11px] font-bold text-campus-600 tracking-[0.1em] uppercase">
                New Password
              </label>
              <div className="relative">
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="8+ chars, upper, lower, digit, symbol"
                  autoComplete="new-password"
                  className={`w-full h-[48px] px-4 pr-12 rounded-xl border bg-white text-[15px] text-campus-900 placeholder:text-gray-400 outline-none transition-all focus:border-campus-600 focus:ring-2 focus:ring-campus-600/10 ${errors.password ? 'border-red-400' : 'border-gray-200'}`}
                  {...register('password')}
                />
                <EyeToggle />
              </div>
              {errors.password && <p className="text-red-500 text-xs">{errors.password.message}</p>}
            </div>

            <div className="space-y-2">
              <label htmlFor="confirmPassword" className="block text-[11px] font-bold text-campus-600 tracking-[0.1em] uppercase">
                Confirm Password
              </label>
              <div className="relative">
                <input
                  id="confirmPassword"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="Re-enter your password"
                  autoComplete="new-password"
                  className={`w-full h-[48px] px-4 pr-12 rounded-xl border bg-white text-[15px] text-campus-900 placeholder:text-gray-400 outline-none transition-all focus:border-campus-600 focus:ring-2 focus:ring-campus-600/10 ${errors.confirmPassword ? 'border-red-400' : 'border-gray-200'}`}
                  {...register('confirmPassword')}
                />
                <EyeToggle />
              </div>
              {errors.confirmPassword && <p className="text-red-500 text-xs">{errors.confirmPassword.message}</p>}
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full h-[52px] bg-campus-800 hover:bg-campus-700 text-white text-[15px] font-semibold rounded-xl transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {submitting ? 'Activating...' : 'Activate account'}
            </button>
          </form>
        )}
      </div>
    </AuthLayout>
  );
}
