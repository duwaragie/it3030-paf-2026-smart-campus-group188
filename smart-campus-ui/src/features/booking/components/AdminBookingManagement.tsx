import { useEffect, useState } from 'react';
import type { BookingDTO } from '@/services/bookingService';
import { bookingService } from '@/services/bookingService';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useToast } from '@/components/ui/use-toast';

export function AdminBookingManagement() {
  const [bookings, setBookings] = useState<BookingDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [rejectionReasons, setRejectionReasons] = useState<Record<number, string>>({});
  const [expandedBooking, setExpandedBooking] = useState<number | null>(null);
  const { toast } = useToast();

  useEffect(() => {
    loadBookings();
  }, []);

  const loadBookings = async () => {
    try {
      setLoading(true);
      const response = await bookingService.getPendingBookings();
      setBookings(response.data.content);
      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load bookings');
      setBookings([]);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (bookingId: number) => {
    if (!window.confirm('Are you sure you want to approve this booking?')) return;

    try {
      await bookingService.approve(bookingId);
      toast({ title: 'Success', description: 'Booking approved successfully' });
      loadBookings();
    } catch (err: any) {
      toast({
        title: 'Error',
        description: err.response?.data?.message || 'Failed to approve booking',
        variant: 'destructive',
      });
    }
  };

  const handleReject = async (bookingId: number) => {
    const reason = rejectionReasons[bookingId];
    if (!reason) {
      toast({
        title: 'Error',
        description: 'Please provide a rejection reason',
        variant: 'destructive',
      });
      return;
    }

    if (!window.confirm('Are you sure you want to reject this booking?')) return;

    try {
      await bookingService.reject(bookingId, reason);
      toast({ title: 'Success', description: 'Booking rejected successfully' });
      setRejectionReasons((prev) => {
        const updated = { ...prev };
        delete updated[bookingId];
        return updated;
      });
      loadBookings();
    } catch (err: any) {
      toast({
        title: 'Error',
        description: err.response?.data?.message || 'Failed to reject booking',
        variant: 'destructive',
      });
    }
  };

  if (loading) return <div className="text-center py-8">Loading bookings...</div>;
  if (error) return <div className="text-center py-8 text-red-600">{error}</div>;

  return (
    <div className="w-full">
      <div className="mb-6">
        <h2 className="text-2xl font-bold">Pending Booking Requests</h2>
        <p className="text-gray-600">Total pending: {bookings.length}</p>
      </div>

      {bookings.length === 0 ? (
        <p className="text-center text-gray-500 py-8">No pending bookings</p>
      ) : (
        <div className="grid gap-4">
          {bookings.map((booking) => (
            <Card key={booking.id} className="p-4">
              <div className="flex justify-between items-start mb-4">
                <div>
                  <p className="font-semibold text-lg">{booking.resourceName}</p>
                  <p className="text-sm text-gray-600">
                    Requested by: {booking.userName} ({booking.userEmail})
                  </p>
                </div>
                <Badge className="bg-yellow-100 text-yellow-800">PENDING</Badge>
              </div>

              <div className="grid grid-cols-2 gap-4 mb-4">
                <div>
                  <p className="text-sm text-gray-600">Requested Date</p>
                  <p className="text-sm">{new Date(booking.requestedAt).toLocaleString()}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-600">Booking Date Range</p>
                  <p className="text-sm">
                    {new Date(booking.startTime).toLocaleDateString()} (
                    {new Date(booking.startTime).toLocaleTimeString([], {
                      hour: '2-digit',
                      minute: '2-digit',
                    })}{' '}
                    -{' '}
                    {new Date(booking.endTime).toLocaleTimeString([], {
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                    )
                  </p>
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

              <div className="space-y-4 pt-4 border-t">
                <div className="flex gap-2">
                  <Button
                    onClick={() => handleApprove(booking.id)}
                    className="bg-green-600 hover:bg-green-700"
                  >
                    Approve
                  </Button>
                  <Button
                    variant="outline"
                    onClick={() =>
                      setExpandedBooking(expandedBooking === booking.id ? null : booking.id)
                    }
                  >
                    {expandedBooking === booking.id ? 'Hide Rejection' : 'Reject'}
                  </Button>
                </div>

                {expandedBooking === booking.id && (
                  <div className="space-y-3 p-4 bg-gray-50 rounded">
                    <div>
                      <Label htmlFor={`reason-${booking.id}`}>Rejection Reason</Label>
                      <Input
                        id={`reason-${booking.id}`}
                        placeholder="Provide reason for rejection"
                        value={rejectionReasons[booking.id] || ''}
                        onChange={(e) =>
                          setRejectionReasons((prev) => ({
                            ...prev,
                            [booking.id]: e.target.value,
                          }))
                        }
                        minLength={5}
                        maxLength={500}
                      />
                    </div>
                    <Button
                      variant="destructive"
                      onClick={() => handleReject(booking.id)}
                      className="w-full"
                    >
                      Confirm Rejection
                    </Button>
                  </div>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
