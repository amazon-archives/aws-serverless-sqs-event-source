package com.amazonaws.serverless.sqseventsource;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.LongUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.amazonaws.serverless.sqseventsource.messageprocessor.SQSMessageProcessorRequest;
import com.amazonaws.serverless.sqseventsource.messageprocessor.SQSMessageProcessorResponse;
import com.amazonaws.serverless.sqseventsource.messageprocessor.SQSMessageResult;
import com.amazonaws.services.sqs.model.Message;

import com.google.common.base.Preconditions;

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

    private final List<Long> messageProcessingTimings = new ArrayList<>();

    public void dispatch(final List<Message> messages) {
        Preconditions.checkArgument(!messages.isEmpty(), "messages cannot be empty");

        Instant start = Instant.now();
        SQSMessageProcessorResponse response = messageProcessorProxy.invoke(new SQSMessageProcessorRequest(messages));
        updateTimings(Duration.between(start, Instant.now()).toMillis(), messages.size());

        Map<SQSMessageResult.Status, List<SQSMessageResult>> resultsByStatus = response.getMessageResults()
                .stream()
                .collect(Collectors.groupingBy(SQSMessageResult::getStatus));

        deleteMessages(messages, resultsByStatus.getOrDefault(SQSMessageResult.Status.SUCCESS, Collections.emptyList()));
        retryMessages(messages, resultsByStatus.getOrDefault(SQSMessageResult.Status.RETRY, Collections.emptyList()));
        retryMessages(messages, resultsByStatus.getOrDefault(SQSMessageResult.Status.ERROR, Collections.emptyList()));
    }

    private synchronized void updateTimings(long processingTimeInMillis, int numMessagesProcessed) {
        long perMessageAverage = processingTimeInMillis / numMessagesProcessed;
        log.info("Processed {} messages in {}ms. perMessageAverage={}ms", numMessagesProcessed, processingTimeInMillis, perMessageAverage);
        messageProcessingTimings.addAll(
                LongStream.iterate(perMessageAverage, LongUnaryOperator.identity())
                        .limit(numMessagesProcessed)
                        .boxed()
                        .collect(Collectors.toList()));
    }

    private void deleteMessages(final List<Message> messages, final List<SQSMessageResult> results) {
        log.info("Deleting {} messages.", results.size());
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
        log.info("Retrying {} messages.", results.size());
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

    public synchronized void reset() {
        messageProcessingTimings.clear();
    }

    public synchronized int getEstimatedCapacity(Instant cutoff) {
        // special case: if we haven't processed anything yet, just say we can process a lot of messages
        if (messageProcessingTimings.isEmpty()) {
            log.info("No message processing timings yet. Returning estimated capacity of INT_MAX");
            return Integer.MAX_VALUE;
        }

        // otherwise, use average of messages processed to estimate how many more we can handle
        LongSummaryStatistics stats = messageProcessingTimings.stream()
                .collect(Collectors.summarizingLong(Long::longValue));

        double averagePerMessageProcessingTimeInMillis = stats.getAverage();
        long remainingMillis = Duration.between(Instant.now(), cutoff).toMillis();
        int estimatedCapacity = (int) (((double) remainingMillis) / averagePerMessageProcessingTimeInMillis);

        log.info("Estimated capacity of {} messages in remaining {}ms from timings: {}", estimatedCapacity, remainingMillis, messageProcessingTimings);

        return estimatedCapacity;
    }
}
