package com.example.localchat.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Domain entity: Factory Department
 */
@Entity
@Table(name = "departments", indexes = {
    @Index(name = "idx_departments_code", columnList = "department_code")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long departmentId;
    
    @Column(nullable = false, unique = true, length = 100)
    private String departmentName;
    
    @Column(nullable = false, unique = true, length = 20)
    private String departmentCode;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_user_id")
    private User manager;
    
    @Column(length = 255)
    private String email;
    
    @Column(length = 20)
    private String phone;
    
    @Column(length = 100)
    private String slackChannel;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Builder.Default
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
