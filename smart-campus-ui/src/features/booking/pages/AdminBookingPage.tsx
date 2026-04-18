import { AdminBookingManagement } from '../components/AdminBookingManagement';

export function AdminBookingPage() {
  return (
    <div className="flex-1 space-y-6 p-4 md:p-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Booking Management</h1>
        <p className="text-gray-600 mt-2">Review and manage booking requests</p>
      </div>

      <AdminBookingManagement />
    </div>
  );
}
