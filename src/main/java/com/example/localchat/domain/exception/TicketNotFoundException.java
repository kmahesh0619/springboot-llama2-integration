package com.example.localchat.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a ticket is not found by its number.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(String ticketNumber) {
        super("Ticket not found with number: " + ticketNumber);
    }
}
