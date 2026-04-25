import { useMemo } from 'react';
import type { BookingDTO, BookingStatus } from '@/services/bookingService';

type BookingGanttChartProps = {
  bookings: BookingDTO[];
  scope: 'admin' | 'user';
};

type TimelineBooking = BookingDTO & {
  startMs: number;
  endMs: number;
  leftPct: number;
  widthPct: number;
  lane: number;
};

type TimelineRow = {
  resourceName: string;
  locationName?: string;
  bookings: TimelineBooking[];
  laneCount: number;
};

const STATUS_STYLES: Record<BookingStatus, { bar: string; text: string; dot: string }> = {
  PENDING: { bar: 'bg-amber-100 text-amber-900 border border-amber-200', text: 'text-amber-800', dot: 'bg-amber-500' },
  APPROVED: { bar: 'bg-emerald-100 text-emerald-900 border border-emerald-200', text: 'text-emerald-800', dot: 'bg-emerald-500' },
  REJECTED: { bar: 'bg-red-100 text-red-900 border border-red-200', text: 'text-red-800', dot: 'bg-red-500' },
  CANCELLED: { bar: 'bg-gray-100 text-gray-900 border border-gray-200', text: 'text-gray-700', dot: 'bg-gray-500' },
  COMPLETED: { bar: 'bg-blue-100 text-blue-900 border border-blue-200', text: 'text-blue-800', dot: 'bg-blue-500' },
};

const AXIS_TICKS = 6;
const LABEL_WIDTH = 240;
const LANE_STEP = 32;
const LANE_BAR_HEIGHT = 22;
const USER_BAR_STYLE = 'bg-campus-700 text-white border border-campus-800/10';

function toMs(value: string): number {
  const parsed = new Date(value).getTime();
  return Number.isNaN(parsed) ? 0 : parsed;
}

function formatAxisLabel(value: Date): string {
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(value);
}

function formatRangeLabel(startMs: number, endMs: number): string {
  const start = new Date(startMs);
  const end = new Date(endMs);
  return `${new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' }).format(start)} · ${new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit' }).format(start)} - ${new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit' }).format(end)}`;
}

function buildTimelineRows(bookings: BookingDTO[]): { rows: TimelineRow[]; rangeStartMs: number; rangeEndMs: number; ticks: number[] } {
  if (bookings.length === 0) {
    return { rows: [], rangeStartMs: 0, rangeEndMs: 0, ticks: [] };
  }

  const sortedBookings = bookings
    .map((booking) => ({
      ...booking,
      startMs: toMs(booking.startTime),
      endMs: toMs(booking.endTime),
    }))
    .filter((booking) => booking.startMs > 0 && booking.endMs > booking.startMs)
    .sort((left, right) => left.startMs - right.startMs || left.endMs - right.endMs);

  if (sortedBookings.length === 0) {
    return { rows: [], rangeStartMs: 0, rangeEndMs: 0, ticks: [] };
  }

  const earliestStart = Math.min(...sortedBookings.map((booking) => booking.startMs));
  const latestEnd = Math.max(...sortedBookings.map((booking) => booking.endMs));
  const padding = Math.max(30 * 60 * 1000, Math.round((latestEnd - earliestStart) * 0.08));
  const rangeStartMs = earliestStart - padding;
  const rangeEndMs = latestEnd + padding;
  const totalSpanMs = Math.max(rangeEndMs - rangeStartMs, 1);

  const grouped = new Map<string, TimelineBooking[]>();

  for (const booking of sortedBookings) {
    const resourceKey = booking.resourceName;
    const current = grouped.get(resourceKey) ?? [];

    let lane = 0;
    while (current.some((existing) => existing.lane === lane && booking.startMs < existing.endMs && booking.endMs > existing.startMs)) {
      lane += 1;
    }

    current.push({
      ...booking,
      leftPct: ((booking.startMs - rangeStartMs) / totalSpanMs) * 100,
      widthPct: Math.max(((booking.endMs - booking.startMs) / totalSpanMs) * 100, 5),
      lane,
    });

    grouped.set(resourceKey, current);
  }

  const rows = Array.from(grouped.entries())
    .map(([resourceName, rowBookings]) => {
      const laneCount = Math.max(1, Math.max(...rowBookings.map((booking) => booking.lane)) + 1);
      return {
        resourceName,
        locationName: rowBookings[0]?.locationName,
        bookings: rowBookings.sort((left, right) => left.startMs - right.startMs || left.endMs - right.endMs),
        laneCount,
      } satisfies TimelineRow;
    })
    .sort((left, right) => left.resourceName.localeCompare(right.resourceName));

  const ticks = Array.from({ length: AXIS_TICKS }, (_, index) => rangeStartMs + (totalSpanMs * index) / (AXIS_TICKS - 1));

  return { rows, rangeStartMs, rangeEndMs, ticks };
}

export function BookingGanttChart({ bookings, scope }: BookingGanttChartProps) {
  const timeline = useMemo(() => buildTimelineRows(bookings), [bookings]);

  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
      <div className="px-5 py-4 border-b border-gray-100">
        <div className="flex items-center justify-between gap-4 flex-wrap">
          <div>
            <h2 className="text-base font-bold text-campus-900">Booking Gantt Chart</h2>
            <p className="text-xs text-gray-500 mt-1">
              {scope === 'admin'
                ? 'Shows booking owner, purpose, and status across each resource timeline.'
                : 'Shows booked slots only. Empty gaps are available time.'}
            </p>
          </div>
          <div className="text-xs text-gray-500">
            {timeline.rows.length > 0 ? formatRangeLabel(timeline.rangeStartMs, timeline.rangeEndMs) : 'No bookings to plot yet'}
          </div>
        </div>
      </div>

      {timeline.rows.length === 0 ? (
        <div className="px-5 py-10 text-center">
          <p className="text-sm font-semibold text-gray-900">No bookings found</p>
          <p className="text-xs text-gray-500 mt-1">
            Create a booking request to see its status on the timeline.
          </p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <div className="min-w-[900px]">
          <div className="grid border-b border-gray-100 bg-gray-50/60" style={{ gridTemplateColumns: `${LABEL_WIDTH}px 1fr` }}>
            <div className="px-5 py-3 text-[11px] font-bold uppercase tracking-wider text-gray-400">Resource</div>
            <div className="relative px-4 py-3">
              <div className="grid h-full" style={{ gridTemplateColumns: `repeat(${AXIS_TICKS}, minmax(0, 1fr))` }}>
                {timeline.ticks.map((tick, index) => (
                  <div key={`${tick}-${index}`} className="relative text-[10px] font-semibold uppercase tracking-wide text-gray-400">
                    <span className="absolute left-0 top-0 -translate-x-1/2">{formatAxisLabel(new Date(tick))}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div>
            {timeline.rows.map((row) => {
              const rowHeight = Math.max(64, row.laneCount * LANE_STEP + 20);

              return (
                <div key={row.resourceName} className="grid border-b border-gray-50 last:border-b-0" style={{ gridTemplateColumns: `${LABEL_WIDTH}px 1fr` }}>
                  <div className="px-5 py-4 border-r border-gray-50">
                    <p className="text-sm font-semibold text-campus-800 truncate">{row.resourceName}</p>
                    {row.locationName && <p className="text-xs text-gray-500 mt-1 truncate">{row.locationName}</p>}
                  </div>

                  <div className="relative px-4 py-3" style={{ height: rowHeight }}>
                    <div className="absolute inset-x-4 top-3 bottom-3 rounded-xl bg-gradient-to-r from-gray-50 to-white" />
                    <div className="absolute inset-x-4 top-3 bottom-3 bg-[linear-gradient(to_right,rgba(148,163,184,0.18)_1px,transparent_1px)] bg-[size:16.666%_100%]" />

                    {row.bookings.map((booking) => {
                      const style = STATUS_STYLES[booking.status];
                      const top = booking.lane * LANE_STEP + 7;
                      const title =
                        scope === 'admin'
                          ? `${booking.userName} · ${booking.purpose} · ${booking.status} · ${new Date(booking.startTime).toLocaleString()} - ${new Date(booking.endTime).toLocaleString()}`
                          : `Booked slot · ${new Date(booking.startTime).toLocaleString()} - ${new Date(booking.endTime).toLocaleString()}`;

                      return (
                        <div
                          key={booking.id}
                          className={`absolute overflow-hidden rounded-xl px-3 py-2 text-[11px] shadow-sm ${scope === 'admin' ? style.bar : USER_BAR_STYLE}`}
                          style={{
                            left: `calc(${booking.leftPct}% + 1px)`,
                            width: `calc(${booking.widthPct}% - 2px)`,
                            top,
                            height: LANE_BAR_HEIGHT,
                            minWidth: 120,
                          }}
                          title={title}
                        >
                          {scope === 'admin' ? (
                            <div className="flex items-start justify-between gap-3 leading-tight">
                              <div className="min-w-0">
                                <p className="truncate font-semibold text-[11px]">{booking.userName}</p>
                                <p className="truncate text-[10px] opacity-90">{booking.purpose}</p>
                              </div>
                              <span className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-bold ${style.text} bg-white/70`}>
                                {booking.status}
                              </span>
                            </div>
                          ) : (
                            <div className="flex items-center justify-between gap-3">
                              <span className="truncate font-semibold text-[11px]">Booked</span>
                              <span className="h-2.5 w-2.5 rounded-full bg-white/80" />
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
        </div>
      )}
    </div>
  );
}