import { CreateBookingForm } from '../components/CreateBookingForm';
import { MyBookingsList } from '../components/MyBookingsList';
import { useState } from 'react';

export function BookingPage() {
  const [refreshList, setRefreshList] = useState(false);

  const handleBookingSuccess = () => {
    setRefreshList(!refreshList);
  };

  return (
    <div className="flex-1 space-y-6 p-4 md:p-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Resource Booking</h1>
        <p className="text-gray-600 mt-2">Request and manage your resource bookings</p>
      </div>

      <div className="grid grid-cols-1 gap-6">
        <CreateBookingForm onSuccess={handleBookingSuccess} />
        <MyBookingsList key={refreshList.toString()} />
      </div>
    </div>
  );
}
