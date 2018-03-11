package com.amazonaws.serverless.sqseventsource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.amazonaws.serverless.sqseventsource.messageprocessor.SQSMessageProcessorRequest;
import com.amazonaws.serverless.sqseventsource.messageprocessor.SQSMessageProcessorResponse;
import com.amazonaws.serverless.sqseventsource.messageprocessor.SQSMessageResult;
import com.amazonaws.services.sqs.model.Message;

import com.google.common.base.Preconditions;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles dispatching messages to message processor and acking/nacking SQS messages. Also provides capacity estimates based on
 * measuring processing times of previous messages.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class MessageDispatcher {
    static final int DEFAULT_RETRY_DELAY_IN_SECONDS = 10;

    @NonNull
    private final SQSProxy sqsProxy;
    @NonNull
    private final MessageProcessorProxy messageProcessorProxy;
    @NonNull
    private final Clock clock;

    private MessageProcessingStats stats = new MessageProcessingStats();

    public MessageDispatcher(final SQSProxy sqsProxy, final MessageProcessorProxy messageProcessorProxy) {
        this(sqsProxy, messageProcessorProxy, Clock.systemUTC());
    }

    public void dispatch(final List<Message> messages) {
        Preconditions.checkArgument(!messages.isEmpty(), "messages cannot be empty");

        Instant start = Instant.now(clock);
        SQSMessageProcessorResponse response;
        try {
            response = messageProcessorProxy.invoke(new SQSMessageProcessorRequest(messages));
            stats.record(Duration.between(start, Instant.now(clock)), messages.size());
        } catch (MessageProcessorException e) {
            log.warn("MessageProcessor encountered an error", e);
            return;
        }

        Map<SQSMessageResult.Status, List<SQSMessageResult>> resultsByStatus = response.getMessageResults()
                .stream()
                .collect(Collectors.groupingBy(SQSMessageResult::getStatus));

        deleteMessages(messages, resultsByStatus.getOrDefault(SQSMessageResult.Status.SUCCESS, Collections.emptyList()));
        retryMessages(messages, resultsByStatus.getOrDefault(SQSMessageResult.Status.RETRY, Collections.emptyList()));

        // Just log failed messages, but do not do anything to the queue. Let user control this behavior through SQS queue's visibility timeout setting
        logFailedMessageResults(resultsByStatus.getOrDefault(SQSMessageResult.Status.ERROR, Collections.emptyList()));
    }

    private void logFailedMessageResults(List<SQSMessageResult> results) {
        if (results.isEmpty()) {
            // nothing to do
            return;
        }
        log.info("{} messages encountered errors during processing: {}", results.size(), results);
    }

    private void deleteMessages(final List<Message> messages, final List<SQSMessageResult> results) {
        if (results.isEmpty()) {
            // nothing to do
            return;
        }

        Set<String> messageIds = results.stream()
                .map(SQSMessageResult::getMessageId)
                .collect(Collectors.toSet());

        log.info("Deleting {} successful messages from the queue. messageIds: {}", messageIds.size(), messageIds);

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

        log.info("Retrying {} messages. retry results: {}", results.size(), results);

        sqsProxy.retryMessages(retryMessageRequests);
    }

    private int getRetryDelay(final SQSMessageResult result) {
        return Optional.ofNullable(result.getRetryDelayInSeconds()).orElse(DEFAULT_RETRY_DELAY_IN_SECONDS);
    }

    public void reset() {
        stats = new MessageProcessingStats();
    }

    public int getEstimatedCapacity(Instant cutoff) {
        // special case: if we haven't processed anything yet, just say we can process a lot of messages
        if (!stats.hasSamples()) {
            log.info("No message processing stats yet. Returning estimated capacity of INT_MAX");
            return Integer.MAX_VALUE;
        }
        return stats.getEstimatedCapacity(Duration.between(Instant.now(clock), cutoff));
    }
}
