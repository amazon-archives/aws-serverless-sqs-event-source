package com.amazonaws.serverless.sqseventsource;

import com.amazonaws.services.lambda.model.InvokeResult;

import com.google.common.base.Preconditions;

import lombok.Getter;

/**
 * Indicates MessageProcessor lambda function encountered an unexpected error while processing a batch of messages.
 */
public class MessageProcessorException extends Exception {
    @Getter
    private final InvokeResult invokeResult;

    public MessageProcessorException(InvokeResult invokeResult) {
        super(functionErrorMessage(invokeResult));
        this.invokeResult = invokeResult;
    }

    private static String functionErrorMessage(InvokeResult invokeResult) {
        Preconditions.checkArgument(invokeResult.getFunctionError() != null, "InvokeResult does not contain function error");
        return String.format("MessageProcessor function encountered an %s error. Result: %s, Log: %s", invokeResult.getFunctionError(), LambdaUtil.getPayloadAsString(invokeResult), LambdaUtil.getDecodedLog(invokeResult));
    }
}
