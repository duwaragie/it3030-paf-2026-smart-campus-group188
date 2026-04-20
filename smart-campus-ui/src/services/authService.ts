import { api } from '@/lib/axios';
import type { User } from '@/store/authStore';

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  type: string;
  user: User;
}

export const authService = {
  login: (email: string, password: string) =>
    api.post<LoginResponse>('/auth/login', { email, password }),

  register: (name: string, email: string, password: string) =>
    api.post<{ message: string }>('/auth/register', { name, email, password }),

  verifyEmail: (email: string, otp: string) =>
    api.post<{ message: string }>('/auth/verify-email', { email, otp }),

  resendOtp: (email: string) =>
    api.post<{ message: string }>('/auth/resend-otp', { email }),

  forgotPassword: (email: string) =>
    api.post<{ message: string }>('/auth/forgot-password', { email }),

  resetPassword: (email: string, token: string, newPassword: string) =>
    api.post<{ message: string }>('/auth/reset-password', { email, token, newPassword }),

  getMe: () =>
    api.get<User>('/auth/me'),

  validateAccountSetup: (token: string) =>
    api.get<{ email: string; name: string; role: string }>('/account-setup/validate', { params: { token } }),

  completeAccountSetup: (token: string, password: string) =>
    api.post<{ message: string }>('/account-setup/complete', { token, password }),
};
