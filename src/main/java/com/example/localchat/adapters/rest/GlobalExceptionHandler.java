package com.example.localchat.adapters.rest;

import com.example.localchat.adapters.llm.OllamaException;
import com.example.localchat.adapters.rest.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global API Error Handler.
 * Returns standardized ApiResponse<T> for all exceptions.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Bean Validation (400) ─────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));

        log.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure("Request validation failed", fieldErrors));
    }

    // ── Missing / malformed body (400) ────────────────────────────────────────

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.warn("Unreadable body: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure("Request body is missing or contains malformed JSON"));
    }

    // ── Resource Not Found (404) ──────────────────────────────────────────────

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        log.warn("Resource not found: {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure("Endpoint not found. Ensure you are using the correct role-based prefix (e.g., /api/v1/worker/...)"));
    }

    // ── Ollama failure (502 / 503) ────────────────────────────────────────────

    @ExceptionHandler(OllamaException.class)
    public ResponseEntity<ApiResponse<Void>> handleOllama(OllamaException ex) {
        HttpStatus status = ex.isUnavailable() ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
        log.error("Ollama error [{}]: {}", ex.getHttpStatus(), ex.getMessage());
        return ResponseEntity.status(status)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    // ── IllegalArgument (400) ─────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("IllegalArgument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    // ── RuntimeException (404 for "not found", else 400) ────────────────────────

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException ex) {
        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        boolean isNotFound = message.contains("not found") || message.contains("no such");
        HttpStatus status = isNotFound ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        
        if (isNotFound) log.warn("Not found: {}", ex.getMessage());
        else log.warn("RuntimeException: {}", ex.getMessage());
        
        return ResponseEntity.status(status)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    // ── Security Exceptions (401 / 403) ───────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure("Access denied: You do not have permission for this resource"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure("Authentication failed: " + ex.getMessage()));
    }

    // ── Catch-all (500) ───────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("An unexpected error occurred: " + ex.getMessage()));
    }
}
