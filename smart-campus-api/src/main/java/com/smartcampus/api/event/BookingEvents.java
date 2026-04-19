package com.smartcampus.api.event;

import com.smartcampus.api.model.Booking;

// Ticket module should follow the same pattern: publish events, don't
// call NotificationService directly.
public final class BookingEvents {

    private BookingEvents() {}

    public record BookingCreated(Booking booking) {}
    public record BookingApproved(Booking booking, Long adminId) {}
    public record BookingRejected(Booking booking, Long adminId, String reason) {}
    public record BookingCancelled(Booking booking, Long actorId) {}
}
