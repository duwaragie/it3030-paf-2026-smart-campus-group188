import { api } from '@/lib/axios';

export type NotificationType =
  | 'ENROLLMENT_CONFIRMED'
  | 'ENROLLMENT_WAITLISTED'
  | 'WAITLIST_PROMOTED'
  | 'ENROLLMENT_WITHDRAWN'
  | 'GRADE_RELEASED'
  | 'GRADE_UPDATED'
  | 'COURSE_STATUS_CHANGED'
  | 'BOOKING_CREATED'
  | 'BOOKING_APPROVED'
  | 'BOOKING_REJECTED'
  | 'BOOKING_CANCELLED'
  | 'TICKET_CREATED'
  | 'TICKET_ASSIGNED'
  | 'TICKET_UPDATED'
  | 'TICKET_RESOLVED'
  | 'SYSTEM_ANNOUNCEMENT'
  | 'ANNOUNCEMENT'
  | 'GENERAL';

export type NotificationPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface NotificationDTO {
  id: number;
  type: NotificationType;
  priority: NotificationPriority;
  title: string;
  message: string;
  link?: string | null;
  read: boolean;
  readAt?: string | null;
  createdAt: string;
}

// In-app is always on and not part of the DTO.
// Quiet hours suppress push only; times are "HH:mm" strings.
export interface NotificationPreferenceDTO {
  email: boolean;
  push: boolean;
  quietHoursEnabled: boolean;
  quietHoursStart: string;
  quietHoursEnd: string;
}

export const notificationService = {
  list: (limit = 50) => api.get<NotificationDTO[]>('/notifications/me', { params: { limit } }),
  unreadCount: () => api.get<{ count: number }>('/notifications/me/unread-count'),
  markRead: (id: number) => api.patch<NotificationDTO>(`/notifications/${id}/read`),
  markAllRead: () => api.patch<{ updated: number }>('/notifications/me/read-all'),
  delete: (id: number) => api.delete(`/notifications/${id}`),
  getPreferences: () => api.get<NotificationPreferenceDTO>('/notifications/preferences'),
  updatePreferences: (pref: Partial<NotificationPreferenceDTO>) =>
    api.put<NotificationPreferenceDTO>('/notifications/preferences', pref),
};
