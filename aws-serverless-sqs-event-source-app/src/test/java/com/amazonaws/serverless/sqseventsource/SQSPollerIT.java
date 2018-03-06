package com.amazonaws.serverless.sqseventsource;

import static com.amazonaws.serverless.sqseventsource.EventualConsistency.waitUntil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;

import org.junit.Before;
import org.junit.Test;

public class SQSPollerIT {
    private static final Integer SUCCESS_MESSAGE_COUNT = 6;
    private static final Integer RETRY_MESSAGE_COUNT = 2;
    private static final Integer ERROR_MESSAGE_COUNT = 2;

    private TestStackHelper testStackHelper;
    private AmazonSQS sqs;

    @Before
    public void setup() throws Exception {
        testStackHelper = new TestStackHelper(AmazonCloudFormationClientBuilder.standard().build());
        sqs = AmazonSQSClientBuilder.standard().build();
    }

    private void sendMessages() {
        List<SendMessageBatchRequestEntry> entries = new ArrayList<>();

        entries.addAll(createMessages("SUCCESS", SUCCESS_MESSAGE_COUNT));
        entries.addAll(createMessages("RETRY", RETRY_MESSAGE_COUNT));
        entries.addAll(createMessages("ERROR", ERROR_MESSAGE_COUNT));

        sqs.sendMessageBatch(new SendMessageBatchRequest().withQueueUrl(testStackHelper.getQueueUrl())
                .withEntries(entries));
    }

    private List<SendMessageBatchRequestEntry> createMessages(String messageBody, Integer num) {
        List<SendMessageBatchRequestEntry> entries = new ArrayList<>();
        for (Integer n = 0; n < num; n++) {
            SendMessageBatchRequestEntry entry = new SendMessageBatchRequestEntry()
                    .withId(UUID.randomUUID().toString())
                    .withMessageBody(String.format("%s_%s", messageBody, UUID.randomUUID().toString()));

            entries.add(entry);
        }
        return entries;
    }

    @Test
    public void sqsPoller() throws Exception {
        waitUntil(getQueueSize(),
                (result) -> result == RETRY_MESSAGE_COUNT + ERROR_MESSAGE_COUNT,
                120000,
                "Timed out waiting for success messages to be processed.");
    }

    private Supplier<Integer> getQueueSize() {
        return () -> {
            GetQueueAttributesRequest request = new GetQueueAttributesRequest()
                    .withQueueUrl(testStackHelper.getQueueUrl())
                    .withAttributeNames(QueueAttributeName.ApproximateNumberOfMessages, QueueAttributeName.ApproximateNumberOfMessagesNotVisible);

            GetQueueAttributesResult result = sqs.getQueueAttributes(request);

            return Integer.parseInt(result.getAttributes().get(QueueAttributeName.ApproximateNumberOfMessages.toString())) +
                    Integer.parseInt(result.getAttributes().get(QueueAttributeName.ApproximateNumberOfMessagesNotVisible.toString()));
        };
    }
}
