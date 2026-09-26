package com.codereviewer.infrastructure.ai;

/** Signals that the configured AI service could not produce a usable response. */
public class AiApiException extends RuntimeException {

    public AiApiException(String message) {
        super(message);
    }

    public AiApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
