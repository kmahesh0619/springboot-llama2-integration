package com.example.localchat.adapters.llm;

/**
 * Thrown when the Ollama server returns an error or cannot be reached.
 */
public class OllamaException extends RuntimeException {

    private final int httpStatus;

    public OllamaException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public OllamaException(String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    /** HTTP status returned by Ollama, or {@code -1} for connection failures. */
    public int getHttpStatus() { return httpStatus; }

    /** {@code true} when Ollama is completely unreachable. */
    public boolean isUnavailable() { return httpStatus == -1 || httpStatus == 503; }
}
