package com.example.localchat.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentEventDTO {

    private Long ticketId;

    private String ticketNumber;

    private String status;

    private String severity;

    private String priority;

    private String department;

    private String assignedTo;

    private String worker;

    private Instant createdAt;

    private Instant slaDeadline;

    private String eventType; // CREATED, ASSIGNED, UPDATED, ESCALATED
}