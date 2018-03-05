package com.amazonaws.serverless.sqseventsource.dagger;


import javax.inject.Singleton;

import com.amazonaws.serverless.sqseventsource.MessageDispatcher;
import com.amazonaws.serverless.sqseventsource.MessageProcessorProxy;
import com.amazonaws.serverless.sqseventsource.SQSPoller;
import com.amazonaws.serverless.sqseventsource.SQSProxy;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

import dagger.Module;
import dagger.Provides;

/**
 * Application DI wiring.
 */
@Module
public class AppModule {
    private static final String QUEUE_URL_KEY = "QUEUE_URL";
    private static final String PROCESSING_TIME_IN_MILLISECONDS_KEY = "PROCESSING_TIME_IN_MILLISECONDS";
    private static final String MESSAGE_PROCESSOR_ARN_KEY = "MESSAGE_PROCESSOR_ARN";

    @Provides
    @Singleton
    public SQSPoller provideSQSPoller(final SQSProxy sqsProxy, final MessageDispatcher messageDispatcher) {
        Integer processingTime = Integer.valueOf(System.getenv(PROCESSING_TIME_IN_MILLISECONDS_KEY));
        return new SQSPoller(sqsProxy, processingTime, messageDispatcher);
    }

    @Provides
    @Singleton
    public MessageDispatcher providesMessageDispatcher(final SQSProxy sqsProxy, final MessageProcessorProxy messageProcessorProxy) {
        return new MessageDispatcher(sqsProxy, messageProcessorProxy);
    }

    @Provides
    @Singleton
    public AmazonSQS provideAmazonSQS() {
        return AmazonSQSClientBuilder.standard()
                .build();
    }

    @Provides
    @Singleton
    public SQSProxy provideSQSProxy(final AmazonSQS sqs) {
        String queueUrl = System.getenv(QUEUE_URL_KEY);
        return new SQSProxy(sqs, queueUrl);
    }

    @Provides
    @Singleton
    public MessageProcessorProxy provideMessageProcessorProxy(final AWSLambda lambda) {
        String messageProcessorArn = System.getenv(MESSAGE_PROCESSOR_ARN_KEY);
        return new MessageProcessorProxy(messageProcessorArn, lambda);
    }

    @Provides
    @Singleton
    public AWSLambda provideAWSLambda() {
        return AWSLambdaClientBuilder.standard()
                .build();
    }
}
