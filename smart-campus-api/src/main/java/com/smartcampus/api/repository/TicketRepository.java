package com.smartcampus.api.repository;

import com.smartcampus.api.model.Ticket;
import com.smartcampus.api.model.TicketStatus;
import com.smartcampus.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByCreatedByOrderByCreatedAtDesc(User user);
    List<Ticket> findAllByOrderByCreatedAtDesc();
    List<Ticket> findByStatus(TicketStatus status);
    List<Ticket> findByAssignedTo(User user);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTo = :technician AND t.assignedAt >= :dayStart AND t.assignedAt < :dayEnd")
    long countAssignedToday(@Param("technician") User technician,
                            @Param("dayStart") LocalDateTime dayStart,
                            @Param("dayEnd") LocalDateTime dayEnd);
}
