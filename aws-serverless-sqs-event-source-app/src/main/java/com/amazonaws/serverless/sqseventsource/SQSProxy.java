package com.amazonaws.serverless.sqseventsource;

import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Convenience proxy for interactions with the SQS queue.
 */
@RequiredArgsConstructor
public class SQSProxy {
    private static final Integer SQS_MAX_NUMBER_OF_MESSAGES_LIMIT = 10;

    @NonNull
    private final AmazonSQS sqs;
    @NonNull
    private final String queueUrl;

    public List<Message> receiveMessages(int limit) {
        ReceiveMessageResult receiveMessageResult = sqs.receiveMessage(new ReceiveMessageRequest()
                .withQueueUrl(queueUrl)
                .withMaxNumberOfMessages(Math.min(limit, SQS_MAX_NUMBER_OF_MESSAGES_LIMIT)));
        return receiveMessageResult.getMessages();
    }

    public void deleteMessages(final List<Message> messages) {
        // TODO: change to DeleteMessageBatch once policy template is updated
        messages.forEach(m -> sqs.deleteMessage(queueUrl, m.getReceiptHandle()));
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
}
