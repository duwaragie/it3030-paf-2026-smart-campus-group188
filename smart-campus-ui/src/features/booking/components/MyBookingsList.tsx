import { useEffect, useState } from 'react';
import type { BookingDTO, BookingStatus } from '@/services/bookingService';
import { bookingService } from '@/services/bookingService';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { useToast } from '@/components/ui/use-toast';

const statusColorMap: Record<BookingStatus, string> = {
  PENDING: 'bg-yellow-100 text-yellow-800',
  APPROVED: 'bg-green-100 text-green-800',
  REJECTED: 'bg-red-100 text-red-800',
  CANCELLED: 'bg-gray-100 text-gray-800',
};

export function MyBookingsList() {
  const [bookings, setBookings] = useState<BookingDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedStatus, setSelectedStatus] = useState<BookingStatus | undefined>();
  const { toast } = useToast();

  useEffect(() => {
    loadBookings();
  }, [selectedStatus]);

  const loadBookings = async () => {
    try {
      setLoading(true);
      const response = await bookingService.getMyBookings(selectedStatus);
      setBookings(response.data.content);
      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load bookings');
      setBookings([]);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = async (bookingId: number) => {
    if (!window.confirm('Are you sure you want to cancel this booking?')) return;

    try {
      await bookingService.cancel(bookingId);
      toast({ title: 'Success', description: 'Booking cancelled successfully' });
      loadBookings();
    } catch (err: any) {
      toast({
        title: 'Error',
        description: err.response?.data?.message || 'Failed to cancel booking',
        variant: 'destructive',
      });
    }
  };

  if (loading) return <div className="text-center py-8">Loading bookings...</div>;
  if (error) return <div className="text-center py-8 text-red-600">{error}</div>;

  return (
    <div className="w-full">
      <div className="mb-6">
        <h2 className="text-2xl font-bold mb-4">My Bookings</h2>
        <div className="flex gap-2 flex-wrap">
          <Button
            variant={selectedStatus === undefined ? 'default' : 'outline'}
            onClick={() => setSelectedStatus(undefined)}
          >
            All
          </Button>
          {(['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'] as BookingStatus[]).map((status) => (
            <Button
              key={status}
              variant={selectedStatus === status ? 'default' : 'outline'}
              onClick={() => setSelectedStatus(status)}
            >
              {status}
            </Button>
          ))}
        </div>
      </div>

      {bookings.length === 0 ? (
        <p className="text-center text-gray-500 py-8">No bookings found</p>
      ) : (
        <div className="grid gap-4">
          {bookings.map((booking) => (
            <Card key={booking.id} className="p-4">
              <div className="grid grid-cols-2 gap-4 mb-4">
                <div>
                  <p className="text-sm text-gray-600">Resource</p>
                  <p className="font-semibold">{booking.resourceName}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-600">Status</p>
                  <Badge className={statusColorMap[booking.status]}>{booking.status}</Badge>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4 mb-4">
                <div>
                  <p className="text-sm text-gray-600">Start Time</p>
                  <p className="text-sm">{new Date(booking.startTime).toLocaleString()}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-600">End Time</p>
                  <p className="text-sm">{new Date(booking.endTime).toLocaleString()}</p>
                </div>
              </div>

              <div className="mb-4">
                <p className="text-sm text-gray-600">Purpose</p>
                <p className="text-sm">{booking.purpose}</p>
              </div>

              {booking.expectedAttendees && (
                <div className="mb-4">
                  <p className="text-sm text-gray-600">Expected Attendees</p>
                  <p className="text-sm">{booking.expectedAttendees}</p>
                </div>
              )}

              {booking.rejectionReason && (
                <div className="mb-4 p-3 bg-red-50 rounded">
                  <p className="text-sm text-gray-600">Rejection Reason</p>
                  <p className="text-sm text-red-800">{booking.rejectionReason}</p>
                </div>
              )}

              {booking.status === 'APPROVED' && new Date(booking.startTime) > new Date() && (
                <Button
                  variant="destructive"
                  size="sm"
                  onClick={() => handleCancel(booking.id)}
                >
                  Cancel Booking
                </Button>
              )}
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
