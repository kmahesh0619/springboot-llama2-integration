package com.example.localchat.application.sla;

import com.example.localchat.application.repository.SlaPolicyRepository;
import com.example.localchat.domain.entity.SlaPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * SLA (Service Level Agreement) Engine.
 * 
 * Responsibilities:
 * - Map incident severity to SLA targets
 * - Calculate resolution deadlines
 * - Handle priority-based SLA assignment
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlaEngineService {

    private final SlaPolicyRepository slaPolicyRepository;

    @Value("${incident.sla.critical-minutes:30}")
    private Integer criticalMinutes;

    @Value("${incident.sla.high-minutes:240}")
    private Integer highMinutes;

    @Value("${incident.sla.medium-minutes:1440}")
    private Integer mediumMinutes;

    @Value("${incident.sla.low-minutes:4320}")
    private Integer lowMinutes;

    /**
     * Calculate SLA minutes based on severity level.
     * 
     * SLA Mapping:
     * - CRITICAL → 30 minutes
     * - HIGH → 240 minutes (4 hours)
     * - MEDIUM → 1440 minutes (24 hours)
     * - LOW → 4320 minutes (72 hours)
     */
    public Integer calculateSlaMinutes(String severity) {
        log.debug("Calculating SLA for severity: {}", severity);

        Integer slaMinutes = switch (severity.toUpperCase()) {
            case "CRITICAL" -> criticalMinutes;
            case "HIGH" -> highMinutes;
            case "MEDIUM" -> mediumMinutes;
            case "LOW" -> lowMinutes;
            default -> {
                log.warn("Unknown severity level: {}, defaulting to MEDIUM", severity);
                yield mediumMinutes;
            }
        };

        log.info("SLA minutes calculated: severity={}, minutes={}", severity, slaMinutes);
        return slaMinutes;
    }

    /**
     * Calculate SLA deadline given severity.
     * 
     * Returns: currentTime + slaMinutes
     */
    public Instant calculateSlaDeadline(String severity) {
        Integer slaMinutes = calculateSlaMinutes(severity);
        Instant deadline = Instant.now().plusSeconds(slaMinutes * 60L);
        log.info("SLA deadline calculated: severity={}, deadline={}", severity, deadline);
        return deadline;
    }

    /**
     * Get escalation time (when ticket should be escalated to management).
     */
    public Integer getEscalationMinutes(String severity) {
        SlaPolicy policy = slaPolicyRepository
                .findBySeverityLevel(severity)
                .orElse(null);

        if (policy != null && policy.getEscalationMinutes() != null) {
            return policy.getEscalationMinutes();
        }

        // Default escalation: 1/3 of SLA target
        Integer slaMinutes = calculateSlaMinutes(severity);
        Integer escalation = slaMinutes / 3;
        log.info("Escalation time: severity={}, minutes={}", severity, escalation);
        return escalation;
    }

    /**
     * Determine if an incident is SLA breached.
     */
    public boolean isSlaBreached(Instant slaDeadline) {
        boolean breached = Instant.now().isAfter(slaDeadline);
        if (breached) {
            log.warn("SLA BREACHED - deadline was: {}, current: {}", slaDeadline, Instant.now());
        }
        return breached;
    }

    /**
     * Get time remaining until SLA deadline (in minutes).
     */
    public Long getMinutesUntilSla(Instant slaDeadline) {
        long remainingSeconds = slaDeadline.getEpochSecond() - Instant.now().getEpochSecond();
        long remainingMinutes = remainingSeconds / 60;
        log.debug("Minutes until SLA deadline: {}", remainingMinutes);
        return remainingMinutes;
    }

    /**
     * Check if escalation is warranted (SLA deadline within escalation window).
     */
    public boolean shouldEscalate(String severity, Instant slaDeadline) {
        Integer escalationMinutes = getEscalationMinutes(severity);
        Long minutesRemaining = getMinutesUntilSla(slaDeadline);

        boolean shouldEscalate = minutesRemaining <= escalationMinutes;
        if (shouldEscalate) {
            log.warn("ESCALATION NEEDED: severity={}, minutes remaining={}, escalation threshold={}",
                    severity, minutesRemaining, escalationMinutes);
        }
        return shouldEscalate;
    }
}
