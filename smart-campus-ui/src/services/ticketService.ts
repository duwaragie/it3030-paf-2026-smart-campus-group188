import { api } from '@/lib/axios';

export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'REJECTED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type TicketCategory =
  | 'ELECTRICAL'
  | 'PLUMBING'
  | 'IT_EQUIPMENT'
  | 'FURNITURE'
  | 'HVAC'
  | 'SAFETY'
  | 'CLEANING'
  | 'OTHER';

export interface TicketCommentDTO {
  id: number;
  content: string;
  authorId: number;
  authorName: string;
  createdAt: string;
  updatedAt: string;
}

export interface TicketDTO {
  id: number;
  title: string;
  location: string;
  category: TicketCategory;
  description: string;
  priority: TicketPriority;
  status: TicketStatus;
  preferredContactEmail?: string;
  preferredContactPhone?: string;
  rejectionReason?: string;
  resolutionNotes?: string;
  createdById: number;
  createdByName: string;
  assignedToId?: number;
  assignedToName?: string;
  imageIds: number[];
  comments: TicketCommentDTO[];
  createdAt: string;
  updatedAt: string;
  assignedAt?: string;
}

export interface CreateTicketRequest {
  title: string;
  location: string;
  category: TicketCategory;
  description: string;
  priority: TicketPriority;
  preferredContactEmail?: string;
  preferredContactPhone?: string;
}

export const ticketService = {
  getAll: () =>
    api.get<TicketDTO[]>('/tickets'),

  getById: (id: number) =>
    api.get<TicketDTO>(`/tickets/${id}`),

  create: (data: CreateTicketRequest) =>
    api.post<TicketDTO>('/tickets', data),

  updateStatus: (id: number, payload: { status: TicketStatus; rejectionReason?: string; resolutionNotes?: string }) =>
    api.patch<TicketDTO>(`/tickets/${id}/status`, payload),

  assign: (id: number, assignedToId: number) =>
    api.patch<TicketDTO>(`/tickets/${id}/assign`, { assignedToId }),

  delete: (id: number) =>
    api.delete(`/tickets/${id}`),

  uploadImage: (ticketId: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post<TicketDTO>(`/tickets/${ticketId}/images`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  getImageBlob: (imageId: number) =>
    api.get<Blob>(`/tickets/images/${imageId}`, { responseType: 'blob' }),

  deleteImage: (ticketId: number, imageId: number) =>
    api.delete(`/tickets/${ticketId}/images/${imageId}`),

  addComment: (ticketId: number, content: string) =>
    api.post<TicketCommentDTO>(`/tickets/${ticketId}/comments`, { content }),

  editComment: (ticketId: number, commentId: number, content: string) =>
    api.put<TicketCommentDTO>(`/tickets/${ticketId}/comments/${commentId}`, { content }),

  deleteComment: (ticketId: number, commentId: number) =>
    api.delete(`/tickets/${ticketId}/comments/${commentId}`),
};
