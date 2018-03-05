package com.amazonaws.serverless.sqseventsource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.sqs.model.Message;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SQSPollerTest {
    private static final Integer PROCESSING_TIME_IN_MS = 350;

    private static final Message MESSAGE1 = new Message().withMessageId("message1").withBody("this is a test message1");
    private static final Message MESSAGE2 = new Message().withMessageId("message2").withBody("this is a test message2");
    private static final Message MESSAGE3 = new Message().withMessageId("message3").withBody("this is a test message3");
    private static final List<Message> MESSAGES = Lists.newArrayList(MESSAGE1, MESSAGE2, MESSAGE3);

    @Mock
    private SQSProxy sqsProxy;

    @Mock
    private Context context;

    @Mock
    private MessageDispatcher messageDispatcher;

    private SQSPoller sqsPoller;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        sqsPoller = new SQSPoller(sqsProxy, PROCESSING_TIME_IN_MS, messageDispatcher);
        when(sqsProxy.receiveMessages()).thenReturn(MESSAGES);
    }

    @Test
    public void poll() throws Exception {
        when(context.getRemainingTimeInMillis()).thenReturn(700, 600, 500, 400);

        sqsPoller.poll(context);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(messageDispatcher, atLeastOnce()).dispatch(captor.capture());
        verify(sqsProxy, atMost(3)).retryMessages(anyList());
    }

    @Test
    public void poll_setToRetry() throws Exception {
        when(context.getRemainingTimeInMillis()).thenReturn(400);
        sqsPoller.poll(context);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(sqsProxy).retryMessages(captor.capture());
        assertThat(captor.getValue().size(), is(3));

        verify(messageDispatcher, never()).dispatch(anyList());
    }
}

