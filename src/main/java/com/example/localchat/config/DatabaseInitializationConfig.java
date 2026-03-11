package com.example.localchat.config;

import com.example.localchat.domain.entity.*;
import com.example.localchat.application.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

/**
 * Database Initialization Configuration
 * 
 * Automatically seeds the database with initial data on application startup.
 * This includes departments, incident types, SLA policies, and sample users.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DatabaseInitializationConfig {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final IncidentTypeRepository incidentTypeRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /**
     * Initialize database with sample data on application startup.
     */
    @Bean
    public CommandLineRunner initializeDatabase() {
        return args -> {
            try {
                log.info("Starting database initialization...");

                // 0. Ensure ShedLock table exists
                try {
                    jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS shedlock (
                            name VARCHAR(64) NOT NULL,
                            lock_until TIMESTAMP NOT NULL,
                            locked_at TIMESTAMP NOT NULL,
                            locked_by VARCHAR(255) NOT NULL,
                            PRIMARY KEY (name)
                        )
                        """);
                    log.info("✓ ShedLock table ensured");
                } catch (Exception e) {
                    log.warn("Could not ensure shedlock table: {}", e.getMessage());
                }

                // 1. Create departments
                if (departmentRepository.count() == 0) {
                    log.info("Creating departments...");
                    
                    Department production = Department.builder()
                            .departmentName("Production")
                            .departmentCode("PROD")
                            .description("Factory production floor operations")
                            .isActive(true)
                            .build();
                    departmentRepository.save(production);

                    Department maintenance = Department.builder()
                            .departmentName("Maintenance")
                            .departmentCode("MAINT")
                            .description("Equipment maintenance and repairs")
                            .isActive(true)
                            .build();
                    departmentRepository.save(maintenance);

                    Department quality = Department.builder()
                            .departmentName("Quality Assurance")
                            .departmentCode("QA")
                            .description("Product quality control")
                            .isActive(true)
                            .build();
                    departmentRepository.save(quality);

                    Department safety = Department.builder()
                            .departmentName("Safety & Compliance")
                            .departmentCode("SAFETY")
                            .description("Worker safety and regulatory compliance")
                            .isActive(true)
                            .build();
                    departmentRepository.save(safety);

                    Department environment = Department.builder()
                            .departmentName("Environmental")
                            .departmentCode("ENV")
                            .description("Environmental management and sustainability")
                            .isActive(true)
                            .build();
                    departmentRepository.save(environment);

                    log.info("✓ Departments created");
                }

                // 2. Create incident types
                if (incidentTypeRepository.count() == 0) {
                    log.info("Creating incident types...");
                    
                    Department production = departmentRepository.findByDepartmentCode("PROD")
                            .orElseThrow(() -> new RuntimeException("Production department not found"));
                    Department maintenance = departmentRepository.findByDepartmentCode("MAINT")
                            .orElseThrow(() -> new RuntimeException("Maintenance department not found"));
                    Department quality = departmentRepository.findByDepartmentCode("QA")
                            .orElseThrow(() -> new RuntimeException("QA department not found"));
                    Department safety = departmentRepository.findByDepartmentCode("SAFETY")
                            .orElseThrow(() -> new RuntimeException("Safety department not found"));
                    Department environment = departmentRepository.findByDepartmentCode("ENV")
                            .orElseThrow(() -> new RuntimeException("Environment department not found"));

                    incidentTypeRepository.save(IncidentType.builder()
                            .typeName("Machine Failure")
                            .typeCode("MACHINE_FAILURE")
                            .description("Equipment malfunction or breakdown")
                            .defaultSeverity("HIGH")
                            .primaryDepartment(maintenance)
                            .isActive(true)
                            .build());

                    incidentTypeRepository.save(IncidentType.builder()
                            .typeName("Safety Hazard")
                            .typeCode("SAFETY_HAZARD")
                            .description("Worker safety risk or accident")
                            .defaultSeverity("CRITICAL")
                            .primaryDepartment(safety)
                            .isActive(true)
                            .build());

                    incidentTypeRepository.save(IncidentType.builder()
                            .typeName("Quality Issue")
                            .typeCode("QUALITY_ISSUE")
                            .description("Product defect or non-conformance")
                            .defaultSeverity("MEDIUM")
                            .primaryDepartment(quality)
                            .isActive(true)
                            .build());

                    incidentTypeRepository.save(IncidentType.builder()
                            .typeName("Maintenance Request")
                            .typeCode("MAINTENANCE_REQUEST")
                            .description("Preventive or corrective maintenance")
                            .defaultSeverity("MEDIUM")
                            .primaryDepartment(maintenance)
                            .isActive(true)
                            .build());

                    incidentTypeRepository.save(IncidentType.builder()
                            .typeName("Environmental Issue")
                            .typeCode("ENVIRONMENTAL_ISSUE")
                            .description("Waste, emissions, or environmental concern")
                            .defaultSeverity("HIGH")
                            .primaryDepartment(environment)
                            .isActive(true)
                            .build());

                    log.info("✓ Incident types created");
                }


                if (slaPolicyRepository.count() == 0) {
                    log.info("Creating SLA policies...");

                    slaPolicyRepository.save(SlaPolicy.builder()
                            .severityLevel("CRITICAL")
                            .targetResolutionMinutes(1)
                            .escalationMinutes(1)
                            .priorityLevel("P1")
                            .description("Critical production impact - immediate response required")
                            .isActive(true)
                            .build());

                    slaPolicyRepository.save(SlaPolicy.builder()
                            .severityLevel("HIGH")
                            .targetResolutionMinutes(2)
                            .escalationMinutes(2)
                            .priorityLevel("P2")
                            .description("Significant impact - response within 4 hours")
                            .isActive(true)
                            .build());

                    slaPolicyRepository.save(SlaPolicy.builder()
                            .severityLevel("MEDIUM")
                            .targetResolutionMinutes(3)
                            .escalationMinutes(3)
                            .priorityLevel("P3")
                            .description("Moderate impact - resolution within 24 hours")
                            .isActive(true)
                            .build());

                    slaPolicyRepository.save(SlaPolicy.builder()
                            .severityLevel("LOW")
                            .targetResolutionMinutes(5)
                            .escalationMinutes(5)
                            .priorityLevel("P4")
                            .description("Minor impact - resolution within 72 hours")
                            .isActive(true)
                            .build());

                    log.info("✓ SLA policies created");
                }

                // 4. Create sample users
                if (userRepository.count() == 0) {
                    log.info("Creating sample users...");
                    
                    Department production = departmentRepository.findByDepartmentCode("PROD").get();

                    userRepository.save(User.builder()
                            .workerId("EMP001")
                            .fullName("John Smith")
                            .email("john.smith@factory.com")
                            .password(passwordEncoder.encode("password123"))
                            .department(production)
                            .role("WORKER")
                            .isActive(true)
                            .build());

                    userRepository.save(User.builder()
                            .workerId("EMP003")
                            .fullName("Robert Wilson")
                            .email("robert.wilson@factory.com")
                            .password(passwordEncoder.encode("password123"))
                            .department(production)
                            .role("WORKER")
                            .isActive(true)
                            .build());

                    userRepository.save(User.builder()
                            .workerId("EMP002")
                            .fullName("Alice Johnson")
                            .email("alice.johnson@factory.com")
                            .password(passwordEncoder.encode("password123"))
                            .department(production)
                            .role("SUPERVISOR")
                            .isActive(true)
                            .build());

                    userRepository.save(User.builder()
                            .workerId("MGR001")
                            .fullName("David Lee")
                            .email("david.lee@factory.com")
                            .password(passwordEncoder.encode("password123"))
                            .department(production)
                            .role("MANAGER")
                            .isActive(true)
                            .build());

                    userRepository.save(User.builder()
                            .workerId("ADMIN001")
                            .fullName("Admin User")
                            .email("admin@factory.com")
                            .password(passwordEncoder.encode("password123"))
                            .role("ADMIN")
                            .isActive(true)
                            .build());

                    log.info("✓ Sample users created");
                }

                log.info("✓ Database initialization completed successfully!");

            } catch (Exception ex) {
                log.error("Error initializing database: {}", ex.getMessage(), ex);
                throw new RuntimeException("Database initialization failed", ex);
            }
        };
    }
}
