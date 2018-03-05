package com.amazonaws.serverless.sqseventsource;

import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.sqs.model.Message;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Polls an SQS queue and delegates messages processing to a user-defined lambda function.
 */
@Slf4j
@RequiredArgsConstructor
public class SQSPoller {
    //this is for making sure lambda function doesn't use up all of remaining time forwarding messages to consumers.
    private static final Integer TIMEOUT_IN_MS = 200;
    @NonNull
    private final SQSProxy sqsProxy;
    // TODO: kill this. We shouldn't rely on clients to be able to tell us how long it'll take them to process a message. Should just measure it ourselves.
    @NonNull
    private final Integer processingTimeInMilliseconds;
    @NonNull
    private final MessageDispatcher messageDispatcher;

    public void poll(final Context context) {
        do {
            List<Message> messagesReceivedFromQueue = sqsProxy.receiveMessages();

            if (!isRemainingTimeEnoughToProcessMessages(context)) {
                sqsProxy.retryMessages(messagesReceivedFromQueue.stream()
                        .map(this::toImmediateRetryRequests)
                        .collect(Collectors.toList()));
                break;
            }

            Integer numOfMsgsCanBeProcessed = (context.getRemainingTimeInMillis() - TIMEOUT_IN_MS) / processingTimeInMilliseconds;

            List<Message> messagesSentToDispatcher = messagesReceivedFromQueue.stream()
                    .limit(numOfMsgsCanBeProcessed)
                    .collect(Collectors.toList());

            List<Message> messagesSentBackToQueue = messagesReceivedFromQueue.stream()
                    .skip(numOfMsgsCanBeProcessed)
                    .collect(Collectors.toList());

            if (!messagesSentBackToQueue.isEmpty()) {
                sqsProxy.retryMessages(messagesSentBackToQueue.stream()
                        .map(this::toImmediateRetryRequests)
                        .collect(Collectors.toList()));
            }

            if (!messagesSentToDispatcher.isEmpty()) {
                messageDispatcher.dispatch(messagesSentToDispatcher);
            }
        } while (true);
    }

    private RetryMessageRequest toImmediateRetryRequests(final Message message) {
        return new RetryMessageRequest(message, 0);
    }

    private boolean isRemainingTimeEnoughToProcessMessages(final Context context) {
        return (context.getRemainingTimeInMillis() - TIMEOUT_IN_MS) > TIMEOUT_IN_MS;
    }
}
