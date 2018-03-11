package com.amazonaws.serverless.sqseventsource;

import com.amazonaws.serverless.sqseventsource.messageprocessor.SQSMessageProcessorRequest;
import com.amazonaws.serverless.sqseventsource.messageprocessor.SQSMessageProcessorResponse;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Proxy for interacting with the message processor lambda function.
 */
@Slf4j
@RequiredArgsConstructor
public class MessageProcessorProxy {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    @NonNull
    private final String messageProcessorFunctionName;
    @NonNull
    private final AWSLambda lambda;

    public SQSMessageProcessorResponse invoke(final SQSMessageProcessorRequest request) throws MessageProcessorException {
        log.info("Invoking message processor lambda to process {} messages", request.getMessages().size());
        String requestPayload = GSON.toJson(request);

        InvokeRequest invokeRequest = new InvokeRequest()
                .withFunctionName(messageProcessorFunctionName)
                .withInvocationType(InvocationType.RequestResponse)
                .withPayload(requestPayload);

        InvokeResult result = lambda.invoke(invokeRequest);

        if (result.getFunctionError() != null) {
            throw new MessageProcessorException(result);
        }

        return GSON.fromJson(LambdaUtil.getPayloadAsString(result), SQSMessageProcessorResponse.class);
    }
}
