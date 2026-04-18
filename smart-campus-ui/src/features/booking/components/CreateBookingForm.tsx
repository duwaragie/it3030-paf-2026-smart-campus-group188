import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { bookingService } from '@/services/bookingService';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card } from '@/components/ui/card';
import { useToast } from '@/components/ui/use-toast';

const bookingSchema = z.object({
  resourceId: z.number().min(1, 'Resource is required'),
  startTime: z.string().min(1, 'Start time is required'),
  endTime: z.string().min(1, 'End time is required'),
  purpose: z.string().min(5, 'Purpose must be at least 5 characters').max(500),
  expectedAttendees: z.number().min(1, 'Must have at least 1 attendee').optional(),
});

type BookingFormData = z.infer<typeof bookingSchema>;

interface CreateBookingFormProps {
  resourceId?: number;
  resourceName?: string;
  onSuccess?: () => void;
}

export function CreateBookingForm({ resourceId, resourceName, onSuccess }: CreateBookingFormProps) {
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<BookingFormData>({
    resolver: zodResolver(bookingSchema),
    defaultValues: {
      resourceId: resourceId || undefined,
    },
  });

  const onSubmit = async (data: BookingFormData) => {
    setLoading(true);
    try {
      await bookingService.create(data);
      toast({
        title: 'Success',
        description: 'Booking request submitted successfully. Awaiting admin approval.',
      });
      reset();
      onSuccess?.();
    } catch (error: any) {
      toast({
        title: 'Error',
        description: error.response?.data?.message || 'Failed to create booking',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card className="p-6 w-full max-w-2xl">
      <h2 className="text-2xl font-bold mb-6">Request a Booking</h2>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {!resourceId && (
          <div>
            <Label htmlFor="resourceId">Resource</Label>
            <Input
              id="resourceId"
              type="number"
              placeholder="Enter resource ID"
              {...register('resourceId', { valueAsNumber: true })}
              className={errors.resourceId ? 'border-red-500' : ''}
            />
            {errors.resourceId && <p className="text-red-500 text-sm mt-1">{errors.resourceId.message}</p>}
          </div>
        )}

        {resourceName && <p className="text-sm text-gray-600">Resource: {resourceName}</p>}

        <div className="grid grid-cols-2 gap-4">
          <div>
            <Label htmlFor="startTime">Start Time</Label>
            <Input
              id="startTime"
              type="datetime-local"
              {...register('startTime')}
              className={errors.startTime ? 'border-red-500' : ''}
            />
            {errors.startTime && <p className="text-red-500 text-sm mt-1">{errors.startTime.message}</p>}
          </div>

          <div>
            <Label htmlFor="endTime">End Time</Label>
            <Input
              id="endTime"
              type="datetime-local"
              {...register('endTime')}
              className={errors.endTime ? 'border-red-500' : ''}
            />
            {errors.endTime && <p className="text-red-500 text-sm mt-1">{errors.endTime.message}</p>}
          </div>
        </div>

        <div>
          <Label htmlFor="purpose">Purpose</Label>
          <Input
            id="purpose"
            placeholder="Describe the purpose of the booking"
            {...register('purpose')}
            className={errors.purpose ? 'border-red-500' : ''}
          />
          {errors.purpose && <p className="text-red-500 text-sm mt-1">{errors.purpose.message}</p>}
        </div>

        <div>
          <Label htmlFor="expectedAttendees">Expected Attendees (Optional)</Label>
          <Input
            id="expectedAttendees"
            type="number"
            placeholder="Number of attendees"
            {...register('expectedAttendees', { valueAsNumber: true })}
            className={errors.expectedAttendees ? 'border-red-500' : ''}
          />
          {errors.expectedAttendees && (
            <p className="text-red-500 text-sm mt-1">{errors.expectedAttendees.message}</p>
          )}
        </div>

        <Button type="submit" disabled={loading} className="w-full">
          {loading ? 'Submitting...' : 'Submit Booking Request'}
        </Button>
      </form>
    </Card>
  );
}
