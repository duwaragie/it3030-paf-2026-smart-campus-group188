package com.smartcampus.api.service.notification;

import com.smartcampus.api.event.BookingEvents;
import com.smartcampus.api.event.TicketEvents;
import com.smartcampus.api.model.Booking;
import com.smartcampus.api.model.NotificationPriority;
import com.smartcampus.api.model.NotificationType;
import com.smartcampus.api.model.Role;
import com.smartcampus.api.model.Ticket;
import com.smartcampus.api.model.TicketComment;
import com.smartcampus.api.model.TicketPriority;
import com.smartcampus.api.model.TicketStatus;
import com.smartcampus.api.model.User;
import com.smartcampus.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Single place where domain events become notifications — the only file
// that changes when a new event type should produce a notification.
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationDispatcher dispatcher;
    private final UserRepository userRepository;

    @EventListener
    public void onBookingCreated(BookingEvents.BookingCreated event) {
        Booking b = event.booking();
        dispatcher.dispatch(NotificationRequest.builder()
                .recipient(b.getUser())
                .type(NotificationType.BOOKING_CREATED)
                .priority(NotificationPriority.MEDIUM)
                .title("Booking request submitted")
                .message(String.format(
                        "Your booking for %s from %s to %s is awaiting admin approval.",
                        b.getResource().getName(), b.getStartTime(), b.getEndTime()))
                .link("/bookings")
                .build());

        userRepository.findByRole(Role.ADMIN)
                .forEach(admin -> dispatcher.dispatch(NotificationRequest.builder()
                        .recipient(admin)
                        .type(NotificationType.BOOKING_CREATED)
                        .priority(NotificationPriority.LOW)
                        .title("New booking request")
                        .message(String.format(
                                "%s requested %s (%s – %s).",
                                b.getUser().getName(), b.getResource().getName(),
                                b.getStartTime(), b.getEndTime()))
                        .link("/admin/bookings")
                        .build()));
    }

    @EventListener
    public void onBookingApproved(BookingEvents.BookingApproved event) {
        Booking b = event.booking();
        dispatcher.dispatch(NotificationRequest.builder()
                .recipient(b.getUser())
                .type(NotificationType.BOOKING_APPROVED)
                .priority(NotificationPriority.HIGH)
                .title("Booking approved")
                .message(String.format(
                        "Your booking for %s (%s – %s) has been approved.",
                        b.getResource().getName(), b.getStartTime(), b.getEndTime()))
                .link("/bookings")
                .build());
    }

    @EventListener
    public void onBookingRejected(BookingEvents.BookingRejected event) {
        Booking b = event.booking();
        String reason = event.reason() != null ? " Reason: " + event.reason() : "";
        dispatcher.dispatch(NotificationRequest.builder()
                .recipient(b.getUser())
                .type(NotificationType.BOOKING_REJECTED)
                .priority(NotificationPriority.HIGH)
                .title("Booking rejected")
                .message(String.format(
                        "Your booking for %s (%s – %s) was rejected.%s",
                        b.getResource().getName(), b.getStartTime(), b.getEndTime(), reason))
                .link("/bookings")
                .build());
    }

    @EventListener
    public void onBookingCancelled(BookingEvents.BookingCancelled event) {
        Booking b = event.booking();
        // Skip self-notifications when a user cancels their own booking.
        boolean cancelledByAdmin = event.actorId() != null
                && !event.actorId().equals(b.getUser().getId());
        if (!cancelledByAdmin) return;

        dispatcher.dispatch(NotificationRequest.builder()
                .recipient(b.getUser())
                .type(NotificationType.BOOKING_CANCELLED)
                .priority(NotificationPriority.MEDIUM)
                .title("Booking cancelled")
                .message(String.format(
                        "An administrator cancelled your booking for %s (%s – %s).",
                        b.getResource().getName(), b.getStartTime(), b.getEndTime()))
                .link("/bookings")
                .build());
    }

    @EventListener
    public void onTicketCreated(TicketEvents.TicketCreated event) {
        Ticket t = event.ticket();
        userRepository.findByRole(Role.ADMIN).forEach(admin ->
                dispatcher.dispatch(NotificationRequest.builder()
                        .recipient(admin)
                        .type(NotificationType.TICKET_CREATED)
                        .priority(mapTicketPriority(t.getPriority()))
                        .title("New ticket: " + t.getTitle())
                        .message(String.format(
                                "%s reported a %s %s issue at %s.",
                                t.getCreatedBy().getName(), t.getPriority(),
                                t.getCategory(), t.getLocation()))
                        .link("/admin/incidents")
                        .build()));
    }

    @EventListener
    public void onTicketAssigned(TicketEvents.TicketAssigned event) {
        Ticket t = event.ticket();
        User assignee = t.getAssignedTo();
        if (assignee == null) return;
        dispatcher.dispatch(NotificationRequest.builder()
                .recipient(assignee)
                .type(NotificationType.TICKET_ASSIGNED)
                .priority(mapTicketPriority(t.getPriority()))
                .title("Ticket assigned to you")
                .message(String.format(
                        "\"%s\" (%s) at %s has been assigned to you.",
                        t.getTitle(), t.getPriority(), t.getLocation()))
                .link("/maintenance/tickets")
                .build());
    }

    @EventListener
    public void onTicketStatusChanged(TicketEvents.TicketStatusChanged event) {
        Ticket t = event.ticket();
        TicketStatus status = t.getStatus();
        NotificationType type = status == TicketStatus.RESOLVED
                ? NotificationType.TICKET_RESOLVED
                : NotificationType.TICKET_UPDATED;

        String extra = "";
        if (status == TicketStatus.REJECTED && t.getRejectionReason() != null) {
            extra = " Reason: " + t.getRejectionReason();
        } else if ((status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED)
                && t.getResolutionNotes() != null && !t.getResolutionNotes().isBlank()) {
            extra = " Notes: " + t.getResolutionNotes();
        }

        User creator = t.getCreatedBy();
        if (creator != null && !creator.getId().equals(event.actorId())) {
            dispatcher.dispatch(NotificationRequest.builder()
                    .recipient(creator)
                    .type(type)
                    .priority(NotificationPriority.MEDIUM)
                    .title("Your ticket is now " + status)
                    .message(String.format("\"%s\" is now %s.%s", t.getTitle(), status, extra))
                    .link("/maintenance/tickets")
                    .build());
        }

        User assignee = t.getAssignedTo();
        if (assignee != null
                && !assignee.getId().equals(event.actorId())
                && (creator == null || !assignee.getId().equals(creator.getId()))) {
            dispatcher.dispatch(NotificationRequest.builder()
                    .recipient(assignee)
                    .type(type)
                    .priority(NotificationPriority.LOW)
                    .title("Ticket status: " + status)
                    .message(String.format("\"%s\" is now %s.%s", t.getTitle(), status, extra))
                    .link("/maintenance/tickets")
                    .build());
        }
    }

    @EventListener
    public void onTicketCommentAdded(TicketEvents.TicketCommentAdded event) {
        Ticket t = event.ticket();
        TicketComment c = event.comment();
        Long authorId = c.getAuthor().getId();
        String content = c.getContent() == null ? "" : c.getContent();
        String preview = content.length() > 80 ? content.substring(0, 77) + "…" : content;

        User creator = t.getCreatedBy();
        if (creator != null && !creator.getId().equals(authorId)) {
            dispatcher.dispatch(NotificationRequest.builder()
                    .recipient(creator)
                    .type(NotificationType.TICKET_UPDATED)
                    .priority(NotificationPriority.LOW)
                    .title("New comment on your ticket")
                    .message(String.format("%s commented on \"%s\": %s",
                            c.getAuthor().getName(), t.getTitle(), preview))
                    .link("/maintenance/tickets")
                    .build());
        }

        User assignee = t.getAssignedTo();
        if (assignee != null
                && !assignee.getId().equals(authorId)
                && (creator == null || !assignee.getId().equals(creator.getId()))) {
            dispatcher.dispatch(NotificationRequest.builder()
                    .recipient(assignee)
                    .type(NotificationType.TICKET_UPDATED)
                    .priority(NotificationPriority.LOW)
                    .title("New comment on assigned ticket")
                    .message(String.format("%s commented on \"%s\": %s",
                            c.getAuthor().getName(), t.getTitle(), preview))
                    .link("/maintenance/tickets")
                    .build());
        }
    }

    private NotificationPriority mapTicketPriority(TicketPriority p) {
        if (p == null) return NotificationPriority.MEDIUM;
        return switch (p) {
            case CRITICAL, HIGH -> NotificationPriority.HIGH;
            case MEDIUM -> NotificationPriority.MEDIUM;
            case LOW -> NotificationPriority.LOW;
        };
    }
}
