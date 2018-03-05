package com.amazonaws.serverless.sqseventsource;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.amazonaws.serverless.sqseventsource.messageprocessor.SQSMessageProcessorRequest;
import com.amazonaws.serverless.sqseventsource.messageprocessor.SQSMessageProcessorResponse;
import com.amazonaws.serverless.sqseventsource.messageprocessor.SQSMessageResult;
import com.amazonaws.services.sqs.model.Message;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Delegates messages to processor implementation.
 * Acks/Nacks messages from SQS.
 */
@Slf4j
@RequiredArgsConstructor
public class MessageDispatcher {
    static final int DEFAULT_RETRY_DELAY_IN_SECONDS = 10;
    static final int DEFAULT_ERROR_RETRY_DELAY_IN_SECONDS = 5;

    @NonNull
    private final SQSProxy sqsProxy;

    @NonNull
    private final MessageProcessorProxy messageProcessorProxy;

    public void dispatch(final List<Message> messages) {
        SQSMessageProcessorResponse response = messageProcessorProxy.invoke(new SQSMessageProcessorRequest(messages));

        Map<SQSMessageResult.Status, List<SQSMessageResult>> resultsByStatus = response.getMessageResults()
                .stream()
                .collect(Collectors.groupingBy(SQSMessageResult::getStatus));

        deleteMessages(messages, resultsByStatus.getOrDefault(SQSMessageResult.Status.SUCCESS, Collections.emptyList()));
        retryMessages(messages, resultsByStatus.getOrDefault(SQSMessageResult.Status.RETRY, Collections.emptyList()));
        retryMessages(messages, resultsByStatus.getOrDefault(SQSMessageResult.Status.ERROR, Collections.emptyList()));
    }

    private void deleteMessages(final List<Message> messages, final List<SQSMessageResult> results) {
        if (results.isEmpty()) {
            return;
        }

        Set<String> messageIds = results.stream()
                .map(SQSMessageResult::getMessageId)
                .collect(Collectors.toSet());

        List<Message> msgsProcessed = messages.stream()
                .filter(message -> messageIds.contains(message.getMessageId()))
                .collect(Collectors.toList());

        sqsProxy.deleteMessages(msgsProcessed);
    }

    private void retryMessages(final List<Message> messages, final List<SQSMessageResult> results) {
        if (results.isEmpty()) {
            return;
        }

        Map<String, Message> messageIdToMessage = messages.stream()
                .collect(Collectors.toMap(Message::getMessageId, Function.identity()));

        List<RetryMessageRequest> retryMessageRequests = results.stream()
                .map(r -> new RetryMessageRequest(messageIdToMessage.get(r.getMessageId()), getRetryDelay(r)))
                .collect(Collectors.toList());

        sqsProxy.retryMessages(retryMessageRequests);
    }

    private int getRetryDelay(final SQSMessageResult result) {
        if (result.getRetryDelayInSeconds() != null) {
            return result.getRetryDelayInSeconds();
        }
        if (result.getStatus() == SQSMessageResult.Status.RETRY) {
            return DEFAULT_RETRY_DELAY_IN_SECONDS;
        }
        return DEFAULT_ERROR_RETRY_DELAY_IN_SECONDS;
    }
}
