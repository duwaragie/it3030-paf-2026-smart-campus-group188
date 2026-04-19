import { useState } from 'react';
import { GRADE_OPTIONS, type EnrollmentDTO, type Grade } from '@/services/enrollmentService';

interface Props {
  enrollment: EnrollmentDTO;
  newGrade: Grade;
  busy: boolean;
  onCancel: () => void;
  onConfirm: (reason: string | undefined) => void;
}

const labelFor = (g?: Grade | null) =>
  GRADE_OPTIONS.find((o) => o.value === g)?.label || (g ?? 'Not set');

export default function ReleasedGradeReasonModal({
  enrollment,
  newGrade,
  busy,
  onCancel,
  onConfirm,
}: Props) {
  const [reason, setReason] = useState('');

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl w-full max-w-md">
        <div className="p-5 border-b border-gray-100">
          <h2 className="text-lg font-bold text-campus-900">Change released grade</h2>
          <p className="text-xs text-gray-500 mt-1">
            This grade is already released. The student will be notified and the change is recorded in the audit log.
          </p>
        </div>

        <div className="p-5 space-y-4">
          <div className="rounded-xl bg-gray-50 p-3 text-sm space-y-1">
            <div className="flex justify-between">
              <span className="text-gray-500">Student</span>
              <span className="font-semibold text-campus-900">{enrollment.studentName}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500">Course</span>
              <span className="font-mono text-campus-800">
                {enrollment.courseCode} / {enrollment.sectionLabel}
              </span>
            </div>
            <div className="flex justify-between items-center pt-1 border-t border-gray-200/70">
              <span className="text-gray-500">Change</span>
              <span className="text-sm">
                <span className="text-gray-500">{labelFor(enrollment.grade)}</span>
                <span className="mx-2 text-gray-400">→</span>
                <span className="font-bold text-campus-900">{labelFor(newGrade)}</span>
              </span>
            </div>
          </div>

          <div>
            <label className="text-xs font-semibold text-gray-700 mb-1 block">
              Reason <span className="text-gray-400 font-normal">(optional)</span>
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="e.g. Typo in original grade; re-mark after review."
              rows={3}
              maxLength={500}
              className="w-full p-3 rounded-lg border border-gray-200 text-sm focus:outline-none focus:ring-2 focus:ring-campus-200 focus:border-campus-400"
            />
            <p className="text-[11px] text-gray-400 mt-1">{reason.length}/500</p>
          </div>
        </div>

        <div className="flex justify-end gap-2 p-4 border-t border-gray-100">
          <button
            onClick={onCancel}
            disabled={busy}
            className="h-9 px-4 rounded-lg border border-gray-200 text-sm font-semibold text-campus-800 hover:bg-gray-50 disabled:opacity-60"
          >
            Cancel
          </button>
          <button
            onClick={() => onConfirm(reason.trim() || undefined)}
            disabled={busy}
            className="h-9 px-4 rounded-lg bg-campus-800 text-white text-sm font-semibold hover:bg-campus-700 disabled:opacity-60"
          >
            {busy ? 'Saving…' : 'Confirm & save'}
          </button>
        </div>
      </div>
    </div>
  );
}
