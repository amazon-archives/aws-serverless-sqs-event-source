package com.amazonaws.serverless.sqseventsource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

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
    private static final Instant NOW = Instant.now();

    @Mock
    private SQSProxy sqsProxy;
    @Mock
    private MessageProcessorProxy messageProcessorProxy;

    private MessageDispatcher dispatcher;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        dispatcher = new MessageDispatcher(sqsProxy, messageProcessorProxy, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    public void dispatch_successResults() throws Exception {
        List<Message> messages = Lists.newArrayList(mockMessage("1"), mockMessage("2"));
        mockProcessorResults(
                messageResult("1", SQSMessageResult.Status.SUCCESS),
                messageResult("2", SQSMessageResult.Status.SUCCESS)
        );

        dispatcher.dispatch(messages);

        verify(messageProcessorProxy).invoke(new SQSMessageProcessorRequest(messages));
        verify(sqsProxy).deleteMessages(messages);
        verifyNoMoreInteractions(messageProcessorProxy, sqsProxy);
    }

    @Test
    public void dispatch_retryResults_defaultRetryDelay() throws Exception {
        Message m1 = mockMessage("1");
        Message m2 = mockMessage("2");
        List<Message> messages = Lists.newArrayList(m1, m2);
        mockProcessorResults(
                messageResult("1", SQSMessageResult.Status.RETRY),
                messageResult("2", SQSMessageResult.Status.RETRY)
        );

        dispatcher.dispatch(messages);

        verify(messageProcessorProxy).invoke(new SQSMessageProcessorRequest(messages));
        List<RetryMessageRequest> expected = Lists.newArrayList(
                new RetryMessageRequest(m1, MessageDispatcher.DEFAULT_RETRY_DELAY_IN_SECONDS),
                new RetryMessageRequest(m2, MessageDispatcher.DEFAULT_RETRY_DELAY_IN_SECONDS)
        );
        verify(sqsProxy).retryMessages(expected);
        verifyNoMoreInteractions(messageProcessorProxy, sqsProxy);
    }

    @Test
    public void dispatch_retryResults_customRetryDelay() throws Exception {
        Message m1 = mockMessage("1");
        Message m2 = mockMessage("2");
        List<Message> messages = Lists.newArrayList(m1, m2);
        mockProcessorResults(
                retryResult("1", 13),
                retryResult("2", 29)
        );

        dispatcher.dispatch(messages);

        verify(messageProcessorProxy).invoke(new SQSMessageProcessorRequest(messages));
        List<RetryMessageRequest> expected = Lists.newArrayList(
                new RetryMessageRequest(m1, 13),
                new RetryMessageRequest(m2, 29)
        );
        verify(sqsProxy).retryMessages(expected);
        verifyNoMoreInteractions(messageProcessorProxy, sqsProxy);
    }

    @Test
    public void dispatch_errorResults() throws Exception {
        Message m1 = mockMessage("1");
        Message m2 = mockMessage("2");
        List<Message> messages = Lists.newArrayList(m1, m2);
        mockProcessorResults(
                messageResult("1", SQSMessageResult.Status.ERROR),
                messageResult("2", SQSMessageResult.Status.ERROR)
        );

        dispatcher.dispatch(messages);

        verify(messageProcessorProxy).invoke(new SQSMessageProcessorRequest(messages));
        List<RetryMessageRequest> expected = Lists.newArrayList(
                new RetryMessageRequest(m1, MessageDispatcher.DEFAULT_ERROR_RETRY_DELAY_IN_SECONDS),
                new RetryMessageRequest(m2, MessageDispatcher.DEFAULT_ERROR_RETRY_DELAY_IN_SECONDS)
        );
        verify(sqsProxy).retryMessages(expected);
        verifyNoMoreInteractions(messageProcessorProxy, sqsProxy);
    }

    @Test
    public void dispatch_mixedResults() throws Exception {
        Message success = mockMessage("success");
        Message retry = mockMessage("retry");
        Message error = mockMessage("error");
        List<Message> messages = Lists.newArrayList(success, retry, error);
        mockProcessorResults(
                messageResult("success", SQSMessageResult.Status.SUCCESS),
                messageResult("retry", SQSMessageResult.Status.RETRY),
                messageResult("error", SQSMessageResult.Status.ERROR)
        );

        dispatcher.dispatch(messages);

        verify(messageProcessorProxy).invoke(new SQSMessageProcessorRequest(messages));
        verify(sqsProxy).deleteMessages(Lists.newArrayList(success));
        verify(sqsProxy).retryMessages(Lists.newArrayList(new RetryMessageRequest(retry, MessageDispatcher.DEFAULT_RETRY_DELAY_IN_SECONDS)));
        verify(sqsProxy).retryMessages(Lists.newArrayList(new RetryMessageRequest(error, MessageDispatcher.DEFAULT_ERROR_RETRY_DELAY_IN_SECONDS)));
        verifyNoMoreInteractions(messageProcessorProxy, sqsProxy);
    }

    @Test
    public void reset() throws Exception {
        dispatcher = new MessageDispatcher(sqsProxy, messageProcessorProxy);
        mockProcessorResults(messageResult("1", SQSMessageResult.Status.SUCCESS));
        dispatcher.dispatch(Lists.newArrayList(mockMessage("1")));

        assertThat(dispatcher.getEstimatedCapacity(NOW), is(not(Integer.MAX_VALUE)));
        dispatcher.reset();
        assertThat(dispatcher.getEstimatedCapacity(NOW), is(Integer.MAX_VALUE));
    }

    @Test
    public void getEstimatedCapacity_messagesProcessed() throws Exception {
        dispatcher = new MessageDispatcher(sqsProxy, messageProcessorProxy);
        mockProcessorResults(messageResult("1", SQSMessageResult.Status.SUCCESS));

        dispatcher.dispatch(Lists.newArrayList(mockMessage("1")));
        assertThat(dispatcher.getEstimatedCapacity(NOW), is(not(Integer.MAX_VALUE)));
    }

    @Test
    public void getEstimatedCapacity_noMessagesProcessed() throws Exception {
        assertThat(dispatcher.getEstimatedCapacity(NOW), is(Integer.MAX_VALUE));
    }

    private Message mockMessage(String messageId) {
        Message message = mock(Message.class);
        when(message.getMessageId()).thenReturn(messageId);
        return message;
    }

    private void mockProcessorResults(SQSMessageResult... results) {
        when(messageProcessorProxy.invoke(any())).thenReturn(new SQSMessageProcessorResponse(Arrays.asList(results)));
    }

    private SQSMessageResult messageResult(String messageId, SQSMessageResult.Status status) {
        return new SQSMessageResult(messageId, status);
    }

    private SQSMessageResult retryResult(String messageId, int retryDelayInSeconds) {
        return new SQSMessageResult(messageId, SQSMessageResult.Status.RETRY, retryDelayInSeconds);
    }
}