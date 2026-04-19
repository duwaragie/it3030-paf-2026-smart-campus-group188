import { useEffect, useState } from 'react';
import {
  enrollmentService,
  type GradeChangeDTO,
} from '@/services/enrollmentService';

interface Props {
  enrollmentId: number;
  studentName: string;
  courseCode: string;
  onClose: () => void;
}

export default function GradeHistoryModal({ enrollmentId, studentName, courseCode, onClose }: Props) {
  const [history, setHistory] = useState<GradeChangeDTO[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    enrollmentService
      .gradeHistory(enrollmentId)
      .then((res) => setHistory(res.data))
      .catch(() => setError('Failed to load history.'));
  }, [enrollmentId]);

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl w-full max-w-xl max-h-[80vh] flex flex-col">
        <div className="flex items-start justify-between p-5 border-b border-gray-100">
          <div>
            <h2 className="text-lg font-bold text-campus-900">Grade history</h2>
            <p className="text-xs text-gray-500 mt-1">
              {studentName} — {courseCode}
            </p>
          </div>
          <button onClick={onClose} className="w-8 h-8 rounded-lg text-gray-400 hover:bg-gray-50" aria-label="Close">
            ✕
          </button>
        </div>

        <div className="p-5 overflow-y-auto">
          {error && (
            <div className="p-3 rounded-lg bg-red-50 border border-red-100 text-red-600 text-sm font-medium">
              {error}
            </div>
          )}
          {!history && !error && (
            <div className="flex justify-center py-8">
              <div className="w-6 h-6 border-2 border-gray-200 border-t-campus-600 rounded-full animate-spin" />
            </div>
          )}
          {history && history.length === 0 && (
            <p className="text-sm text-gray-400 text-center py-6">
              No grade changes recorded yet.
            </p>
          )}
          {history && history.length > 0 && (
            <ol className="relative border-l border-gray-200 ml-2 space-y-5">
              {history.map((h) => (
                <li key={h.id} className="pl-5 relative">
                  <span className="absolute -left-[7px] top-1 w-3 h-3 rounded-full bg-campus-600 border-2 border-white" />
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-sm font-semibold text-campus-900">
                      {h.previousGradeLabel || 'Not set'} → {h.newGradeLabel || 'Cleared'}
                    </span>
                    {h.wasReleased && (
                      <span className="text-[10px] font-bold uppercase tracking-wider rounded bg-amber-100 text-amber-700 px-1.5 py-0.5">
                        Post-release edit
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-gray-500 mt-1">
                    {new Date(h.changedAt).toLocaleString()} by{' '}
                    <span className="font-medium text-campus-700">{h.changedByName || 'System'}</span>
                  </p>
                  {h.reason && (
                    <p className="text-xs text-gray-600 mt-1 italic">“{h.reason}”</p>
                  )}
                </li>
              ))}
            </ol>
          )}
        </div>

        <div className="flex justify-end p-4 border-t border-gray-100">
          <button
            onClick={onClose}
            className="h-9 px-4 rounded-lg border border-gray-200 text-sm font-semibold text-campus-800 hover:bg-gray-50"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
