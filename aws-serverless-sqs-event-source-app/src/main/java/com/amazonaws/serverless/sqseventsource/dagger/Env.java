package com.amazonaws.serverless.sqseventsource.dagger;

/**
 * Helper class for fetching environment values.
 */
public final class Env {
    public static final String QUEUE_URL_KEY = "QUEUE_URL";
    public static final String PROCESSING_TIME_IN_MILLISECONDS_KEY = "PROCESSING_TIME_IN_MILLISECONDS";
    public static final String MESSAGE_PROCESSOR_ARN_KEY = "MESSAGE_PROCESSOR_ARN";

    private Env() {
    }

    public static String getQueueUrl() {
        return System.getenv(QUEUE_URL_KEY);
    }

    public static String getProcessingTimeInMilliseconds() {
        return System.getenv(PROCESSING_TIME_IN_MILLISECONDS_KEY);
    }

    public static String getMessageProcessorArn() {
        return System.getenv(MESSAGE_PROCESSOR_ARN_KEY);
    }
}
