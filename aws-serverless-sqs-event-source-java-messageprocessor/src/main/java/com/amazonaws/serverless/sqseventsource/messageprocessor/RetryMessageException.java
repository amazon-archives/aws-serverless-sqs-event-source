package com.amazonaws.serverless.sqseventsource.messageprocessor;

import lombok.Getter;

/**
 * Exception thrown by Consumer delegates of SQSMessageProcessor to indicate the message cannot be processed at this time
 * and should be retried. A retry delay (in seconds) can be set on this exception to control the delay before the next attempt.
 */
public class RetryMessageException extends RuntimeException {
    @Getter
    private Integer retryDelayInSeconds = null;

    public RetryMessageException(final String message) {
        super(message);
    }

    public RetryMessageException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public RetryMessageException withRetryDelayInSeconds(final Integer retryDelay) {
        this.retryDelayInSeconds = retryDelay;
        return this;
    }
}
