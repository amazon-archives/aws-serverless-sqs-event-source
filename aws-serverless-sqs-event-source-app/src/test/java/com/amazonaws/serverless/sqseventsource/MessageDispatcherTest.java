package com.amazonaws.serverless.sqseventsource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.serverless.sqseventsource.messageprocessor.SQSMessageProcessorRequest;
import com.amazonaws.serverless.sqseventsource.messageprocessor.SQSMessageProcessorResponse;
import com.amazonaws.serverless.sqseventsource.messageprocessor.SQSMessageResult;
import com.amazonaws.services.sqs.model.Message;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MessageDispatcherTest {
    private static final Message MESSAGE1 = new Message().withMessageId("message1").withBody("this is a test message1");
    private static final Message MESSAGE2 = new Message().withMessageId("message2").withBody("this is a test message2");
    private static final Message MESSAGE3 = new Message().withMessageId("message3").withBody("this is a test message3");

    @Mock
    private SQSProxy sqsProxy;
    @Mock
    private MessageProcessorProxy messageProcessorProxy;

    private MessageDispatcher messageDispatcher;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        messageDispatcher = new MessageDispatcher(sqsProxy, messageProcessorProxy);
    }

    @Test
    public void dispatch_successMessage() throws Exception {
        SQSMessageProcessorResponse response = new SQSMessageProcessorResponse(
                Lists.newArrayList(new SQSMessageResult(MESSAGE1.getMessageId(), SQSMessageResult.Status.SUCCESS))
        );
        when(messageProcessorProxy.invoke(any(SQSMessageProcessorRequest.class))).thenReturn(response);

        messageDispatcher.dispatch(Lists.newArrayList(MESSAGE1));

        verify(sqsProxy).deleteMessages(Lists.newArrayList(MESSAGE1));
        verifyNoMoreInteractions(sqsProxy);
    }

    @Test
    public void dispatch_retryMessage() throws Exception {
        SQSMessageProcessorResponse response = new SQSMessageProcessorResponse(
                Lists.newArrayList(new SQSMessageResult(MESSAGE2.getMessageId(), SQSMessageResult.Status.RETRY))
        );
        when(messageProcessorProxy.invoke(any(SQSMessageProcessorRequest.class))).thenReturn(response);

        messageDispatcher.dispatch(Lists.newArrayList(MESSAGE2));

        verify(sqsProxy).retryMessages(Lists.newArrayList(new RetryMessageRequest(MESSAGE2, MessageDispatcher.DEFAULT_RETRY_DELAY_IN_SECONDS)));
        verifyNoMoreInteractions(sqsProxy);
    }

    @Test
    public void dispatch_retryMessage_explicitRetryDelay() throws Exception {
        SQSMessageProcessorResponse response = new SQSMessageProcessorResponse(
                Lists.newArrayList(new SQSMessageResult(MESSAGE2.getMessageId(), SQSMessageResult.Status.RETRY, 30))
        );
        when(messageProcessorProxy.invoke(any(SQSMessageProcessorRequest.class))).thenReturn(response);

        messageDispatcher.dispatch(Lists.newArrayList(MESSAGE2));

        verify(sqsProxy).retryMessages(Lists.newArrayList(new RetryMessageRequest(MESSAGE2, 30)));
        verifyNoMoreInteractions(sqsProxy);
    }

    @Test
    public void dispatch_errorMessage() throws Exception {
        SQSMessageProcessorResponse response = new SQSMessageProcessorResponse(
                Lists.newArrayList(new SQSMessageResult(MESSAGE3.getMessageId(), SQSMessageResult.Status.ERROR))
        );
        when(messageProcessorProxy.invoke(any(SQSMessageProcessorRequest.class))).thenReturn(response);

        messageDispatcher.dispatch(Lists.newArrayList(MESSAGE3));

        verify(sqsProxy).retryMessages(Lists.newArrayList(new RetryMessageRequest(MESSAGE3, MessageDispatcher.DEFAULT_ERROR_RETRY_DELAY_IN_SECONDS)));
        verifyNoMoreInteractions(sqsProxy);
    }

    @Test
    public void dispatch_mixedMessages() throws Exception {
        SQSMessageProcessorResponse response = new SQSMessageProcessorResponse(Lists.newArrayList(
                new SQSMessageResult(MESSAGE1.getMessageId(), SQSMessageResult.Status.SUCCESS),
                new SQSMessageResult(MESSAGE2.getMessageId(), SQSMessageResult.Status.RETRY),
                new SQSMessageResult(MESSAGE3.getMessageId(), SQSMessageResult.Status.ERROR)
        ));
        when(messageProcessorProxy.invoke(any(SQSMessageProcessorRequest.class))).thenReturn(response);

        messageDispatcher.dispatch(Lists.newArrayList(MESSAGE1, MESSAGE2, MESSAGE3));

        verify(sqsProxy).deleteMessages(Lists.newArrayList(MESSAGE1));
        verify(sqsProxy).retryMessages(Lists.newArrayList(new RetryMessageRequest(MESSAGE2, MessageDispatcher.DEFAULT_RETRY_DELAY_IN_SECONDS)));
        verify(sqsProxy).retryMessages(Lists.newArrayList(new RetryMessageRequest(MESSAGE3, MessageDispatcher.DEFAULT_ERROR_RETRY_DELAY_IN_SECONDS)));
    }
}
