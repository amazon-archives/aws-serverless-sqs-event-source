package com.amazonaws.serverless.sqseventsource;

import com.amazonaws.services.sqs.model.Message;

import lombok.Value;

/**
 * Request parameters for retrying a message.
 */
@Value
public class RetryMessageRequest {
    private final Message message;
    private final int retryDelayInSeconds;
}
