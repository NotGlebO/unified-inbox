
package com.unifiedinbox.unified_inbox.core.adapter;

public class MessageSendException extends RuntimeException {

    public enum FailureType {
        PERMANENT,
        RETRYABLE,
        UNKNOWN
    }

    private final FailureType failureType;
    private final Long retryAfterSeconds;

    public MessageSendException(
            FailureType failureType,
            String message
    ) {
        this(failureType, message, null, null);
    }

    public MessageSendException(
            FailureType failureType,
            String message,
            Throwable cause
    ) {
        this(failureType, message, null, cause);
    }

    public MessageSendException(
            FailureType failureType,
            String message,
            Long retryAfterSeconds
    ) {
        this(failureType, message, retryAfterSeconds, null);
    }

    private MessageSendException(
            FailureType failureType,
            String message,
            Long retryAfterSeconds,
            Throwable cause
    ) {
        super(message, cause);
        this.failureType = failureType;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public FailureType getFailureType() {
        return failureType;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
