package com.example.localchat.application.usecase;

import com.example.localchat.adapters.rest.dto.response.TicketDetailResponse;

/**
 * Use case: Retrieve full ticket details for a worker.
 */
public interface GetTicketDetailUseCase {
    TicketDetailResponse getTicketDetail(String ticketNumber);
}
