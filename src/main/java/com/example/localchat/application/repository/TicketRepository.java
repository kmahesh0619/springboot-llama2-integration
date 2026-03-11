package com.example.localchat.application.repository;

import com.example.localchat.domain.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    List<Ticket> findByStatus(String status);

    List<Ticket> findBySlaDeadlineBeforeAndStatus(Instant deadline, String status);

    Page<Ticket> findByDepartmentDepartmentIdAndStatus(Long departmentId, String status, Pageable pageable);

    Page<Ticket> findByCreatedByUserId(Long userId, Pageable pageable);

    Page<Ticket> findByAssignedToUserId(Long userId, Pageable pageable);

    long countByStatus(String status);

    long countByStatusNotIn(Collection<String> statuses);

    /**
     * Flexible filter query for the paginated incident list API.
     * Any parameter that is null is ignored (wildcard match).
     */
    @Query("""
            SELECT t FROM Ticket t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:severity IS NULL OR t.severity = :severity)
              AND (:departmentCode IS NULL OR t.department.departmentCode = :departmentCode)
            ORDER BY t.createdAt DESC
            """)
    Page<Ticket> findByFilters(
            @Param("status") String status,
            @Param("severity") String severity,
            @Param("departmentCode") String departmentCode,
            Pageable pageable
    );

    /**
     * Fetch all active tickets for the SLA escalation scheduler.
     * Only OPEN, IN_PROGRESS, and ESCALATED tickets are eligible for further escalation.
     */
    @Query("SELECT t FROM Ticket t WHERE t.status IN ('OPEN', 'IN_PROGRESS', 'ESCALATED') ORDER BY t.slaDeadline ASC")
    List<Ticket> findActiveTicketsForEscalationCheck();
}
