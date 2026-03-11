package com.example.localchat.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Injects {@code requestId} (from the {@code X-Request-Id} header, or a generated UUID)
 * and {@code httpMethod} / {@code path} into the MDC for correlated structured logging.
 *
 * <p>Example log output:
 * <pre>
 *   INFO ChatService - sessionId=test123 message="Explain Clean Architecture"
 * </pre>
 *
 * <p>MDC is fully cleared in the {@code finally} block to prevent thread-pool leaks.
 */
@Slf4j
@Component
public class MdcRequestFilter extends OncePerRequestFilter {

    private static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain          filterChain
    ) throws ServletException, IOException {

        String requestId = request.getHeader(HEADER_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        try {
            MDC.put("requestId",  requestId);
            MDC.put("httpMethod", request.getMethod());
            MDC.put("path",       request.getRequestURI());

            response.setHeader(HEADER_REQUEST_ID, requestId); // echo back for client correlation
            filterChain.doFilter(request, response);

        } finally {
            MDC.clear();
        }
    }
}
