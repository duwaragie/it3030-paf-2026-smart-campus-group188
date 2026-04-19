import { api } from '@/lib/axios';

export type EnrollmentStatus = 'ENROLLED' | 'WAITLISTED' | 'WITHDRAWN' | 'COMPLETED';

export type Grade =
  | 'A_PLUS' | 'A' | 'A_MINUS'
  | 'B_PLUS' | 'B' | 'B_MINUS'
  | 'C_PLUS' | 'C' | 'C_MINUS'
  | 'D_PLUS' | 'D'
  | 'F' | 'I' | 'W';

export const GRADE_OPTIONS: { value: Grade; label: string; points: number | null }[] = [
  { value: 'A_PLUS',  label: 'A+', points: 4.0 },
  { value: 'A',       label: 'A',  points: 4.0 },
  { value: 'A_MINUS', label: 'A-', points: 3.7 },
  { value: 'B_PLUS',  label: 'B+', points: 3.3 },
  { value: 'B',       label: 'B',  points: 3.0 },
  { value: 'B_MINUS', label: 'B-', points: 2.7 },
  { value: 'C_PLUS',  label: 'C+', points: 2.3 },
  { value: 'C',       label: 'C',  points: 2.0 },
  { value: 'C_MINUS', label: 'C-', points: 1.7 },
  { value: 'D_PLUS',  label: 'D+', points: 1.3 },
  { value: 'D',       label: 'D',  points: 1.0 },
  { value: 'F',       label: 'F',  points: 0.0 },
  { value: 'I',       label: 'I',  points: null },
  { value: 'W',       label: 'W',  points: null },
];

export interface EnrollmentDTO {
  id: number;

  studentId: number;
  studentName: string;
  studentEmail: string;
  studentRegistrationNumber?: string | null;

  sectionId: number;
  sectionLabel: string;
  sectionCapacity: number;
  lecturerId?: number | null;
  lecturerName?: string | null;

  offeringId: number;
  courseCode: string;
  courseTitle: string;
  semester: string;
  credits: number;

  status: EnrollmentStatus;
  grade?: Grade | null;
  gradeLabel?: string | null;
  gradePoints?: number | null;
  gradeReleased: boolean;
  gradeReleasedAt?: string | null;

  enrolledAt: string;
  withdrawnAt?: string | null;
}

export interface TranscriptDTO {
  studentId: number;
  studentName: string;
  studentRegistrationNumber?: string | null;
  gpa: number;
  creditsEarned: number;
  coursesCompleted: number;
  entries: EnrollmentDTO[];
}

export interface GradeChangeDTO {
  id: number;
  previousGrade?: Grade | null;
  previousGradeLabel?: string | null;
  newGrade?: Grade | null;
  newGradeLabel?: string | null;
  wasReleased: boolean;
  changedByName?: string | null;
  reason?: string | null;
  changedAt: string;
}

export type BulkGradeRowStatus = 'VALID' | 'SKIPPED' | 'INVALID';

export interface BulkGradeRowResult {
  rowNumber: number;
  srn: string;
  inputGrade?: string | null;
  studentName?: string | null;
  currentGrade?: Grade | null;
  parsedGrade?: Grade | null;
  status: BulkGradeRowStatus;
  error?: string | null;
}

export interface BulkGradeResult {
  total: number;
  valid: number;
  skipped: number;
  invalid: number;
  committed: boolean;
  appliedCount: number;
  rows: BulkGradeRowResult[];
}

export const enrollmentService = {
  enroll: (sectionId: number) =>
    api.post<EnrollmentDTO>('/enrollments', { sectionId }),

  withdraw: (id: number) => api.delete<EnrollmentDTO>(`/enrollments/${id}`),

  listMine: () => api.get<EnrollmentDTO[]>('/enrollments/me'),

  roster: (sectionId: number) =>
    api.get<EnrollmentDTO[]>(`/enrollments/section/${sectionId}`),

  setGrade: (enrollmentId: number, grade: Grade, reason?: string) =>
    api.put<EnrollmentDTO>(`/enrollments/${enrollmentId}/grade`,
      reason ? { grade, reason } : { grade }),

  gradeHistory: (enrollmentId: number) =>
    api.get<GradeChangeDTO[]>(`/enrollments/${enrollmentId}/history`),

  transcript: () => api.get<TranscriptDTO>('/transcripts/me'),

  downloadGradeTemplate: (sectionId: number) =>
    api.get(`/course-sections/${sectionId}/grades/template`, {
      responseType: 'blob',
    }),

  uploadGradesCsv: (sectionId: number, file: File, dryRun: boolean) => {
    const form = new FormData();
    form.append('file', file);
    return api.post<BulkGradeResult>(
      `/course-sections/${sectionId}/grades/csv`,
      form,
      {
        params: { dryRun },
        headers: { 'Content-Type': 'multipart/form-data' },
      }
    );
  },
};
