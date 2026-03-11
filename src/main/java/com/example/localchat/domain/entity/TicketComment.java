package com.example.localchat.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Domain entity: Ticket Comment / Audit Trail Entry
 */
@Entity
@Table(name = "ticket_comments", indexes = {
    @Index(name = "idx_comments_ticket_id", columnList = "ticket_id"),
    @Index(name = "idx_comments_user_id", columnList = "user_id"),
    @Index(name = "idx_comments_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketComment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    public Ticket ticket;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String commentText;
    
    @Column(nullable = false, length = 50)
    private String commentType = "NOTE"; // NOTE, STATUS_CHANGE, ASSIGNMENT, RESOLUTION
    
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
