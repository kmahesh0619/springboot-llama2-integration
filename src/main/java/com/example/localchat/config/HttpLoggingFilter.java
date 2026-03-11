package com.example.localchat.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HttpLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper =
                new ContentCachingRequestWrapper(request);

        ContentCachingResponseWrapper responseWrapper =
                new ContentCachingResponseWrapper(response);

        long start = System.currentTimeMillis();

        filterChain.doFilter(requestWrapper, responseWrapper);

        long duration = System.currentTimeMillis() - start;

        String requestBody = new String(
                requestWrapper.getContentAsByteArray(),
                StandardCharsets.UTF_8);

        String responseBody = new String(
                responseWrapper.getContentAsByteArray(),
                StandardCharsets.UTF_8);

        String headers = Collections.list(request.getHeaderNames())
                .stream()
                .map(h -> h + ":" + request.getHeader(h))
                .collect(Collectors.joining(", "));

        String fullUrl = request.getRequestURL() +
                (request.getQueryString() != null ? "?" + request.getQueryString() : "");

        log.info("""
                ================= HTTP REQUEST =================
                Method  : {}
                URL     : {}
                Headers : {}
                Body    : {}

                ================= HTTP RESPONSE =================
                Status  : {}
                Body    : {}

                Duration: {} ms
                =================================================
                """,
                request.getMethod(),
                fullUrl,
                headers,
                requestBody,
                responseWrapper.getStatus(),
                responseBody,
                duration
        );

        responseWrapper.copyBodyToResponse();
    }
}