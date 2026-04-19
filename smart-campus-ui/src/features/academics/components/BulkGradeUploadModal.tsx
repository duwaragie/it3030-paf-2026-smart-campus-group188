import { useRef, useState } from 'react';
import {
  enrollmentService,
  type BulkGradeResult,
  type BulkGradeRowStatus,
} from '@/services/enrollmentService';

interface Props {
  sectionId: number;
  sectionLabel: string;
  courseCode: string;
  onClose: () => void;
  onApplied: () => void;
}

const statusStyle: Record<BulkGradeRowStatus, string> = {
  VALID: 'bg-emerald-50 text-emerald-700 border-emerald-100',
  SKIPPED: 'bg-gray-50 text-gray-500 border-gray-100',
  INVALID: 'bg-red-50 text-red-700 border-red-100',
};

const statusLabel: Record<BulkGradeRowStatus, string> = {
  VALID: 'Valid',
  SKIPPED: 'Skipped',
  INVALID: 'Error',
};

export default function BulkGradeUploadModal({
  sectionId,
  sectionLabel,
  courseCode,
  onClose,
  onApplied,
}: Props) {
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<BulkGradeResult | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const handleDownloadTemplate = async () => {
    try {
      setError(null);
      const res = await enrollmentService.downloadGradeTemplate(sectionId);
      const blob = new Blob([res.data as BlobPart], { type: 'text/csv' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `grades-${courseCode}-${sectionLabel}.csv`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch {
      setError('Failed to download template.');
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setPreview(null);
    setSuccess(null);
    setError(null);
    const f = e.target.files?.[0] || null;
    setFile(f);
  };

  const handleValidate = async () => {
    if (!file) return;
    try {
      setBusy(true);
      setError(null);
      setSuccess(null);
      const res = await enrollmentService.uploadGradesCsv(sectionId, file, true);
      setPreview(res.data);
    } catch (err) {
      const e = err as { response?: { data?: { message?: string } } };
      setError(e.response?.data?.message || 'Failed to validate file.');
    } finally {
      setBusy(false);
    }
  };

  const handleCommit = async () => {
    if (!file || !preview || preview.valid === 0) return;
    try {
      setBusy(true);
      setError(null);
      const res = await enrollmentService.uploadGradesCsv(sectionId, file, false);
      if (res.data.committed) {
        const applied = res.data.appliedCount;
        const skippedForErrors = res.data.invalid;
        const parts = [`Applied ${applied} grade${applied === 1 ? '' : 's'}`];
        if (skippedForErrors > 0) parts.push(`skipped ${skippedForErrors} with errors`);
        setSuccess(parts.join(', ') + '.');
        setPreview(res.data);
        onApplied();
      } else {
        setError('Commit did not complete.');
        setPreview(res.data);
      }
    } catch (err) {
      const e = err as { response?: { data?: { message?: string } } };
      setError(e.response?.data?.message || 'Failed to apply grades.');
    } finally {
      setBusy(false);
    }
  };

  const canCommit = !!preview && preview.valid > 0 && !preview.committed;

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl w-full max-w-3xl max-h-[90vh] flex flex-col">
        <div className="flex items-start justify-between p-5 border-b border-gray-100">
          <div>
            <h2 className="text-lg font-bold text-campus-900">Bulk upload grades</h2>
            <p className="text-xs text-gray-500 mt-1">
              {courseCode} / {sectionLabel} — upload a CSV with <strong>SRN</strong> and <strong>Grade</strong> columns.
            </p>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-lg text-gray-400 hover:bg-gray-50"
            aria-label="Close"
          >
            ✕
          </button>
        </div>

        <div className="p-5 space-y-4 overflow-y-auto">
          {error && (
            <div className="p-3 rounded-lg bg-red-50 border border-red-100 text-red-600 text-sm font-medium">
              {error}
            </div>
          )}
          {success && (
            <div className="p-3 rounded-lg bg-emerald-50 border border-emerald-100 text-emerald-700 text-sm font-medium">
              {success}
            </div>
          )}

          <div className="flex items-center gap-3 flex-wrap">
            <button
              onClick={handleDownloadTemplate}
              className="h-9 px-4 rounded-lg border border-gray-200 text-sm font-semibold text-campus-800 hover:bg-gray-50"
            >
              ⤓ Download template
            </button>
            <span className="text-xs text-gray-400">Pre-filled with your roster. Edit grades in Excel, save as CSV, upload below.</span>
          </div>

          <div className="border-2 border-dashed border-gray-200 rounded-xl p-5 space-y-3">
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv,text/csv"
              className="hidden"
              onChange={handleFileChange}
            />
            <div className="flex items-center gap-3 flex-wrap">
              <button
                onClick={() => fileInputRef.current?.click()}
                className="h-9 px-4 rounded-lg bg-campus-800 text-white text-sm font-semibold hover:bg-campus-700"
              >
                Choose CSV file
              </button>
              <span className="text-sm text-campus-800">
                {file ? file.name : <span className="text-gray-400">No file selected</span>}
              </span>
            </div>
            {file && !preview && !success && (
              <button
                onClick={handleValidate}
                disabled={busy}
                className="h-9 px-4 rounded-lg border border-campus-200 text-sm font-semibold text-campus-800 hover:bg-campus-50 disabled:opacity-60"
              >
                {busy ? 'Validating…' : 'Validate'}
              </button>
            )}
          </div>

          {preview && (
            <div className="space-y-3">
              <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                <StatCard label="Total rows" value={preview.total} tone="neutral" />
                <StatCard label="Valid" value={preview.valid} tone="ok" />
                <StatCard label="Skipped (blank)" value={preview.skipped} tone="neutral" />
                <StatCard label="Errors" value={preview.invalid} tone={preview.invalid > 0 ? 'bad' : 'neutral'} />
              </div>

              {preview.invalid > 0 && !preview.committed && (
                <div className="p-3 rounded-lg bg-amber-50 border border-amber-100 text-amber-700 text-xs">
                  {preview.valid > 0
                    ? `${preview.invalid} row${preview.invalid === 1 ? ' has' : 's have'} errors and will be skipped. ${preview.valid} valid row${preview.valid === 1 ? '' : 's'} will still be applied.`
                    : `All rows have errors — nothing can be applied. Fix them and re-upload.`}
                </div>
              )}

              <div className="rounded-xl border border-gray-100 overflow-hidden">
                <table className="w-full text-sm">
                  <thead className="bg-gray-50 text-gray-500 text-[11px] font-semibold uppercase tracking-wider">
                    <tr>
                      <th className="text-left px-3 py-2 w-10">#</th>
                      <th className="text-left px-3 py-2">SRN</th>
                      <th className="text-left px-3 py-2">Student</th>
                      <th className="text-left px-3 py-2">Current</th>
                      <th className="text-left px-3 py-2">New</th>
                      <th className="text-left px-3 py-2">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {preview.rows.map((r) => (
                      <tr key={r.rowNumber} className="border-t border-gray-50">
                        <td className="px-3 py-2 text-gray-400">{r.rowNumber}</td>
                        <td className="px-3 py-2 font-mono text-xs">{r.srn || '—'}</td>
                        <td className="px-3 py-2 text-campus-800">{r.studentName || '—'}</td>
                        <td className="px-3 py-2 text-gray-500">{r.currentGrade || '—'}</td>
                        <td className="px-3 py-2 font-semibold text-campus-900">
                          {r.parsedGrade || r.inputGrade || '—'}
                        </td>
                        <td className="px-3 py-2">
                          <span className={`inline-block text-[11px] font-semibold rounded border px-2 py-0.5 ${statusStyle[r.status]}`}>
                            {statusLabel[r.status]}
                          </span>
                          {r.error && (
                            <div className="text-[11px] text-red-600 mt-0.5">{r.error}</div>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>

        <div className="flex items-center justify-end gap-2 p-5 border-t border-gray-100">
          <button
            onClick={onClose}
            className="h-9 px-4 rounded-lg border border-gray-200 text-sm font-semibold text-campus-800 hover:bg-gray-50"
          >
            Close
          </button>
          <button
            onClick={handleCommit}
            disabled={!canCommit || busy}
            className="h-9 px-4 rounded-lg bg-campus-800 text-white text-sm font-semibold hover:bg-campus-700 disabled:opacity-60"
          >
            {busy ? 'Applying…' : preview && preview.committed ? 'Applied' : `Apply ${preview?.valid || 0} grades`}
          </button>
        </div>
      </div>
    </div>
  );
}

function StatCard({
  label,
  value,
  tone,
}: {
  label: string;
  value: number;
  tone: 'neutral' | 'ok' | 'bad';
}) {
  const toneCls =
    tone === 'ok' ? 'text-emerald-700' : tone === 'bad' ? 'text-red-600' : 'text-campus-900';
  return (
    <div className="rounded-xl border border-gray-100 p-3">
      <p className="text-[10px] uppercase tracking-wider font-semibold text-gray-400">{label}</p>
      <p className={`text-lg font-bold ${toneCls}`}>{value}</p>
    </div>
  );
}
