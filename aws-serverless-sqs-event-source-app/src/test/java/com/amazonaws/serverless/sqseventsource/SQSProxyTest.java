package com.amazonaws.serverless.sqseventsource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SQSProxyTest {

    private static String QUEUE_URL = "queueUrl";
    private static final Integer WAIT_TIME_SECONDS = 10;
    private static final Integer MAX_NUM_OF_MESSAGES = 10;

    private static final Message MESSAGE1 = new Message().withMessageId("message1").withBody("this is a test message1");
    private static final Message MESSAGE2 = new Message().withMessageId("message2").withBody("this is a test message2");
    private static final Message MESSAGE3 = new Message().withMessageId("message3").withBody("this is a test message3");
    private static final List<Message> MESSAGES = Lists.newArrayList(MESSAGE1, MESSAGE2, MESSAGE3);

    @Mock
    private AmazonSQS sqs;

    private SQSProxy sqsProxy;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        ReceiveMessageResult receiveMessageResult = new ReceiveMessageResult();
        receiveMessageResult.setMessages(MESSAGES);

        when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(receiveMessageResult);

        sqsProxy = new SQSProxy(sqs, QUEUE_URL);
    }

    @Test
    public void receiveMessages() throws Exception {
        List<Message> messages = sqsProxy.receiveMessages();
        verify(sqs).receiveMessage(new ReceiveMessageRequest().withQueueUrl(QUEUE_URL).withMaxNumberOfMessages(MAX_NUM_OF_MESSAGES).withWaitTimeSeconds(WAIT_TIME_SECONDS));
    }

    @Test
    public void deleteMessages() throws Exception {
        sqsProxy.deleteMessages(Lists.newArrayList(MESSAGE1));
        List<DeleteMessageBatchRequestEntry> entries = Lists.newArrayList(new DeleteMessageBatchRequestEntry()
                .withId(MESSAGE1.getMessageId())
                .withReceiptHandle(MESSAGE1.getReceiptHandle()));

        verify(sqs).deleteMessageBatch(QUEUE_URL, entries);
    }

    @Test
    public void retryMessages() throws Exception {
        sqsProxy.retryMessages(Lists.newArrayList(new RetryMessageRequest(MESSAGE1, 20)));
        List<ChangeMessageVisibilityBatchRequestEntry> entries = Lists.newArrayList(new ChangeMessageVisibilityBatchRequestEntry()
                .withId(MESSAGE1.getMessageId())
                .withReceiptHandle(MESSAGE1.getReceiptHandle())
                .withVisibilityTimeout(20));

        verify(sqs).changeMessageVisibilityBatch(QUEUE_URL, entries);
    }
}