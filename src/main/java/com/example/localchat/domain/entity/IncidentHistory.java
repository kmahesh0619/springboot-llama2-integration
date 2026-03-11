package com.example.localchat.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * Domain entity: Incident History / Audit Log
 */
@Entity
@Table(name = "incident_history", indexes = {
    @Index(name = "idx_history_ticket_id", columnList = "ticket_id"),
    @Index(name = "idx_history_event_type", columnList = "event_type"),
    @Index(name = "idx_history_timestamp", columnList = "event_timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;
    
    @Column(nullable = false, length = 100)
    private String eventType; // CREATED, ASSIGNED, UPDATED, RESOLVED, CLOSED
    
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant eventTimestamp = Instant.now();
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode changedFields;
    
    @Column(columnDefinition = "TEXT")
    private String oldValue;
    
    @Column(columnDefinition = "TEXT")
    private String newValue;
    
    @Column(length = 50)
    private String changedByUserRole;

    @Column(length = 50)
    private String fromRole; // For escalation tracking

    @Column(length = 50)
    private String toRole;   // For escalation tracking

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id")
    private User changedByUser;
}
