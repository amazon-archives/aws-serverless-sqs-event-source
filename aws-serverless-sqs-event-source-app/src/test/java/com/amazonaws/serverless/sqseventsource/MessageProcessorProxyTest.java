package com.amazonaws.serverless.sqseventsource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.List;

import com.amazonaws.serverless.sqseventsource.messageprocessor.SQSMessageProcessorRequest;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.sqs.model.Message;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


public class MessageProcessorProxyTest {

    private static final String MESSAGE_PROCESSOR_FUNCTION_NAME = "messageProcessorLambda";
    private static final Message MESSAGE1 = new Message().withMessageId("message1").withBody("this is a test message1");
    private static final Message MESSAGE2 = new Message().withMessageId("message2").withBody("this is a test message2");
    private static final Message MESSAGE3 = new Message().withMessageId("message3").withBody("this is a test message3");
    private static final List<Message> MESSAGES = Lists.newArrayList(MESSAGE1, MESSAGE2, MESSAGE3);

    @Mock
    private AWSLambda lambda;

    private MessageProcessorProxy messageProcessorProxy;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        messageProcessorProxy = new MessageProcessorProxy(MESSAGE_PROCESSOR_FUNCTION_NAME, lambda);
    }

    @Test
    public void invoke() throws Exception {
        SQSMessageProcessorRequest sqsMessageProcessorRequest = new SQSMessageProcessorRequest();
        sqsMessageProcessorRequest.setMessages(MESSAGES);

        //fake json string
        ByteBuffer byteBuffer = ByteBuffer.wrap("{\"key1\":value1,\"key2\":value2}".getBytes());

        InvokeResult invokeResult = mock(InvokeResult.class);
        when(invokeResult.getPayload()).thenReturn(byteBuffer);
        when(lambda.invoke(any(InvokeRequest.class))).thenReturn(invokeResult);

        messageProcessorProxy.invoke(sqsMessageProcessorRequest);
    }

    @Test(expected = MessageProcessorException.class)
    public void invoke_functionError() throws Exception {
        SQSMessageProcessorRequest sqsMessageProcessorRequest = new SQSMessageProcessorRequest();
        sqsMessageProcessorRequest.setMessages(MESSAGES);

        InvokeResult invokeResult = mock(InvokeResult.class);
        when(invokeResult.getFunctionError()).thenReturn("Unhandled");
        when(lambda.invoke(any(InvokeRequest.class))).thenReturn(invokeResult);

        messageProcessorProxy.invoke(sqsMessageProcessorRequest);
    }
}
