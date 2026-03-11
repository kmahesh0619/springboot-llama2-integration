package com.example.localchat.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain entity: Incident Ticket (Core Entity)
 */
@Entity
@Table(name = "tickets", indexes = {
    @Index(name = "idx_tickets_number", columnList = "ticket_number"),
    @Index(name = "idx_tickets_worker_id", columnList = "worker_id"),
    @Index(name = "idx_tickets_status", columnList = "status"),
    @Index(name = "idx_tickets_department_id", columnList = "department_id"),
    @Index(name = "idx_tickets_assigned_to", columnList = "assigned_to_user_id"),
    @Index(name = "idx_tickets_sla_deadline", columnList = "sla_deadline"),
    @Index(name = "idx_tickets_status_sla", columnList = "status, sla_deadline")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ticketId;
    
    @Column(nullable = false, unique = true, length = 50)
    private String ticketNumber; // INC-1001, INC-1002, etc.
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private User createdBy;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String originalMessage;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_type_id", nullable = false)
    private IncidentType incidentType;
    
    @Column(nullable = false, length = 20)
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;
    
    @Column(nullable = false, length = 5)
    private String priority; // P1, P2, P3, P4
    
    @Column(nullable = false, length = 50)
    private String status = "OPEN"; // OPEN, IN_PROGRESS, ON_HOLD, RESOLVED, CLOSED
    
    @Column(nullable = false)
    private Integer slaTargetMinutes;
    
    @Column(nullable = false)
    private Instant slaDeadline;
    
    @Column(name = "suggested_actions", columnDefinition = "text[]")
    private List<String> suggestedActions = new ArrayList<>();
    
    @Column(columnDefinition = "TEXT")
    private String resolutionNotes;
    
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    private Instant resolvedAt;
    
    @Builder.Default
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
    
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TicketComment> comments = new ArrayList<>();
}
