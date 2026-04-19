package com.smartcampus.api.event;

import com.smartcampus.api.model.Ticket;
import com.smartcampus.api.model.TicketComment;
import com.smartcampus.api.model.TicketStatus;

public final class TicketEvents {

    private TicketEvents() {}

    public record TicketCreated(Ticket ticket) {}
    public record TicketAssigned(Ticket ticket, Long actorId) {}
    public record TicketStatusChanged(Ticket ticket, TicketStatus previousStatus, Long actorId) {}
    public record TicketCommentAdded(Ticket ticket, TicketComment comment) {}
}
