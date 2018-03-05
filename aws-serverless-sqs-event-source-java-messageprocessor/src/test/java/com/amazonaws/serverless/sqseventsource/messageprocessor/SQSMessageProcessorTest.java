package com.amazonaws.serverless.sqseventsource.messageprocessor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import com.amazonaws.services.sqs.model.Message;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import lombok.Value;

public class SQSMessageProcessorTest {
    private static final Foo FIRST = new Foo("first");
    private static final Foo SECOND = new Foo("second");
    private static final Foo THIRD = new Foo("third");

    @Mock
    private SQSMessageDeserializer<Foo> deserializer;
    @Mock
    private Consumer<Foo> delegate;

    private SQSMessageProcessor<Foo> messageProcessor;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(deserializer.deserialize(any(Message.class)))
                .thenReturn(FIRST)
                .thenReturn(SECOND)
                .thenReturn(THIRD);

        messageProcessor = new SQSMessageProcessor<>(deserializer, delegate);
    }

    @Test
    public void apply() throws Exception {
        doNothing()
                .doThrow(new RetryMessageException("try again later...").withRetryDelayInSeconds(60))
                .doThrow(RuntimeException.class)
                .when(delegate).accept(any(Foo.class));

        Message m1 = new Message().withMessageId("1");
        Message m2 = new Message().withMessageId("2");
        Message m3 = new Message().withMessageId("3");

        SQSMessageProcessorRequest request = new SQSMessageProcessorRequest(Lists.newArrayList(m1, m2, m3));
        SQSMessageProcessorResponse response = messageProcessor.apply(request);

        SQSMessageProcessorResponse expected = new SQSMessageProcessorResponse(Lists.newArrayList(
                new SQSMessageResult("1", SQSMessageResult.Status.SUCCESS),
                new SQSMessageResult("2", SQSMessageResult.Status.RETRY, 60),
                new SQSMessageResult("3", SQSMessageResult.Status.ERROR)
        ));
        assertThat(response, is(expected));
    }

    @Value
    private static class Foo {
        private final String foo;
    }
}