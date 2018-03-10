package com.amazonaws.serverless.sqseventsource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import com.amazonaws.services.sqs.model.Message;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SQSPollerTest {
    private static final Instant NOW = Instant.now();

    @Mock
    private SQSProxy sqsProxy;
    @Mock
    private MessageDispatcher messageDispatcher;

    private SQSPoller poller;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        poller = new SQSPoller(sqsProxy, messageDispatcher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    public void poll_messagesReceived() throws Exception {
        when(messageDispatcher.getEstimatedCapacity(any()))
                .thenReturn(4)
                .thenReturn(2)
                .thenReturn(0);
        List<Message> messages = Lists.newArrayList(mock(Message.class), mock(Message.class));
        when(sqsProxy.receiveMessages(anyInt())).thenReturn(messages);

        poller.poll(SQSPoller.TIMEOUT_BUFFER_IN_MILLIS * 2);

        Instant expectedCutoff = NOW.plusMillis(SQSPoller.TIMEOUT_BUFFER_IN_MILLIS);
        InOrder inOrder = inOrder(messageDispatcher, sqsProxy);
        inOrder.verify(messageDispatcher).reset();
        inOrder.verify(messageDispatcher).getEstimatedCapacity(expectedCutoff);
        inOrder.verify(sqsProxy).receiveMessages(4);
        inOrder.verify(messageDispatcher).dispatch(messages);
        inOrder.verify(messageDispatcher).getEstimatedCapacity(expectedCutoff);
        inOrder.verify(sqsProxy).receiveMessages(2);
        inOrder.verify(messageDispatcher).dispatch(messages);
        inOrder.verify(messageDispatcher).getEstimatedCapacity(expectedCutoff);

        verifyNoMoreInteractions(sqsProxy, messageDispatcher);
    }

    @Test
    public void poll_noMessagesReceived() throws Exception {
        when(messageDispatcher.getEstimatedCapacity(any())).thenReturn(1);
        when(sqsProxy.receiveMessages(anyInt())).thenReturn(Collections.emptyList());

        poller.poll(SQSPoller.TIMEOUT_BUFFER_IN_MILLIS * 2);

        verify(messageDispatcher, never()).dispatch(any());
    }
}