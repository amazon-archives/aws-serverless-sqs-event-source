package com.amazonaws.serverless.sqseventsource.dagger;

/**
 * Helper class for fetching environment values.
 */
public final class Env {
    public static final String QUEUE_URL_KEY = "QUEUE_URL";
    public static final String MESSAGE_PROCESSOR_FUNCTION_NAME_KEY = "MESSAGE_PROCESSOR_FUNCTION_NAME";

    private Env() {
    }

    public static String getQueueUrl() {
        return System.getenv(QUEUE_URL_KEY);
    }

    public static String getMessageProcessorFunctionName() {
        return System.getenv(MESSAGE_PROCESSOR_FUNCTION_NAME_KEY);
    }
}
