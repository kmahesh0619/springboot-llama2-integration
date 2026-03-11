package com.example.localchat.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Domain entity: SLA Policy Definition
 */
@Entity
@Table(name = "sla_policies", indexes = {
    @Index(name = "idx_sla_policies_severity", columnList = "severity_level")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaPolicy {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long slaPolicyId;
    
    @Column(nullable = false, unique = true, length = 20)
    private String severityLevel; // CRITICAL, HIGH, MEDIUM, LOW
    
    @Column(nullable = false)
    private Integer targetResolutionMinutes;
    
    private Integer escalationMinutes;
    
    @Column(nullable = false, length = 5)
    private String priorityLevel; // P1, P2, P3, P4
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Builder.Default
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
