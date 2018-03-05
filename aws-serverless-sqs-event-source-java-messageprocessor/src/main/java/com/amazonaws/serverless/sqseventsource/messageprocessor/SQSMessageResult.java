package com.amazonaws.serverless.sqseventsource.messageprocessor;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * Result of processing an SQS message. This is the response format expected by the sqs event source serverless application.
 */
@Value
@AllArgsConstructor
public class SQSMessageResult {
    /**
     * Message processing status.
     */
    public enum Status {
        /**
         * Message was processed successfully and should be deleted from the queue.
         */
        SUCCESS,

        /**
         * Message cannot be processed at this time and should be retried.
         */
        RETRY,

        /**
         * Message processing encountered an unexpected error.
         */
        ERROR
    }

    /**
     * SQS messageId that was processed.
     */
    @NonNull
    private final String messageId;

    /**
     * Processing result.
     */
    @NonNull
    private final Status status;

    /**
     * If status is RETRY, this value controls how long the app delays before retrying the same message.
     */
    private final Integer retryDelayInSeconds;

    public SQSMessageResult(final String messageId, final Status status) {
        this(messageId, status, null);
    }
}
