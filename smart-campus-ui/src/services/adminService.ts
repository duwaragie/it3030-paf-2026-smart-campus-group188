import { api } from '@/lib/axios';
import type { User } from '@/store/authStore';

export interface UserDTO {
  id: number;
  email: string;
  name: string;
  picture: string;
  role: string;
}

export const adminService = {
  getAllUsers: () =>
    api.get<UserDTO[]>('/admin/users'),

  getUserById: (id: number) =>
    api.get<UserDTO>(`/admin/users/${id}`),

  updateUserRole: (id: number, role: string) =>
    api.put<UserDTO>(`/admin/users/${id}/role`, null, { params: { role } }),

  deleteUser: (id: number) =>
    api.delete(`/admin/users/${id}`),
};
