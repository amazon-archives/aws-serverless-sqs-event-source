package com.amazonaws.serverless.sqseventsource;

import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Proxy implementation of SQS.
 */
@RequiredArgsConstructor
public class SQSProxy {
    private static final Integer WAIT_TIME_SECONDS = 10;
    private static final Integer MAX_NUM_OF_MESSAGES = 10;

    @NonNull
    private final AmazonSQS sqs;
    @NonNull
    private final String queueUrl;

    public List<Message> receiveMessages() {
        ReceiveMessageResult receiveMessageResult = sqs.receiveMessage(new ReceiveMessageRequest()
                .withQueueUrl(queueUrl)
                .withWaitTimeSeconds(WAIT_TIME_SECONDS)
                .withMaxNumberOfMessages(MAX_NUM_OF_MESSAGES));
        return receiveMessageResult.getMessages();
    }

    public void deleteMessages(final List<Message> messages) {
        List<DeleteMessageBatchRequestEntry> entries =
                messages.stream()
                        .map(this::toDeleteMessageBatchRequestEntry)
                        .collect(Collectors.toList());

        sqs.deleteMessageBatch(queueUrl, entries);
    }

    public void retryMessages(final List<RetryMessageRequest> retryRequests) {
        List<ChangeMessageVisibilityBatchRequestEntry> entries =
                retryRequests.stream()
                        .map(this::toChangeMessageVisibilityEntry)
                        .collect(Collectors.toList());

        sqs.changeMessageVisibilityBatch(queueUrl, entries);
    }

    private ChangeMessageVisibilityBatchRequestEntry toChangeMessageVisibilityEntry(final RetryMessageRequest retryRequest) {
        return new ChangeMessageVisibilityBatchRequestEntry()
                .withId(retryRequest.getMessage().getMessageId())
                .withReceiptHandle(retryRequest.getMessage().getReceiptHandle())
                .withVisibilityTimeout(retryRequest.getRetryDelayInSeconds());
    }

    private DeleteMessageBatchRequestEntry toDeleteMessageBatchRequestEntry(final Message message) {
        return new DeleteMessageBatchRequestEntry()
                .withId(message.getMessageId())
                .withReceiptHandle(message.getReceiptHandle());
    }
}
