package com.example.localchat.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Domain entity: Factory Worker / System User
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_worker_id", columnList = "worker_id"),
    @Index(name = "idx_users_email", columnList = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    
    @Column(nullable = false, unique = true, length = 50)
    private String workerId;
    
    @Column(nullable = false, length = 255)
    private String fullName;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
    
    @Column(length = 255)
    private String email;
    
    @Column(length = 20)
    private String phone;
    
    @Column(nullable = false, length = 50)
    private String role; // WORKER, SUPERVISOR, MANAGER, ADMIN
    
    @Column(nullable = false, length = 255)
    private String password;
    
    @Builder.Default
    @Column(nullable = false)
    private Integer failedAttempts = 0;
    
    @Builder.Default
    @Column(nullable = false)
    private Boolean accountLocked = false;
    
    @Column
    private Instant lockTime;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Builder.Default
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
