package com.example.localchat.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Domain entity: Incident Type Definition
 */
@Entity
@Table(name = "incident_types", indexes = {
    @Index(name = "idx_incident_types_code", columnList = "type_code"),
    @Index(name = "idx_incident_types_department", columnList = "primary_department_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long incidentTypeId;
    
    @Column(nullable = false, unique = true, length = 100)
    private String typeName;
    
    @Column(nullable = false, unique = true, length = 50)
    private String typeCode; // MACHINE_FAILURE, SAFETY_HAZARD, QUALITY_ISSUE, etc.
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "primary_department_id", nullable = false)
    private Department primaryDepartment;
    
    @Column(nullable = false, length = 20)
    private String defaultSeverity = "MEDIUM"; // CRITICAL, HIGH, MEDIUM, LOW
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
