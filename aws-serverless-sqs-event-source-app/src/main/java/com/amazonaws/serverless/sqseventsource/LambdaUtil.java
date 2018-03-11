package com.amazonaws.serverless.sqseventsource;

import java.util.Base64;

import com.amazonaws.services.lambda.model.InvokeResult;

import com.google.common.base.Charsets;

/**
 * Utility methods for working with Lambdas.
 */
final class LambdaUtil {
    public static String getPayloadAsString(final InvokeResult invokeResult) {
        if (invokeResult.getPayload() == null) {
            return null;
        }
        byte[] bytes = invokeResult.getPayload().array();
        return new String(bytes, Charsets.UTF_8);
    }

    public static String getDecodedLog(final InvokeResult invokeResult) {
        if (invokeResult.getLogResult() == null) {
            return null;
        }
        byte[] decoded = Base64.getDecoder().decode(invokeResult.getLogResult());
        return new String(decoded, Charsets.UTF_8);
    }
}
