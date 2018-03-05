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
    @Provides
    @Singleton
    public SQSPoller provideSQSPoller(final SQSProxy sqsProxy, final MessageDispatcher messageDispatcher) {
        return new SQSPoller(sqsProxy, 1000, messageDispatcher);
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
        return new SQSProxy(sqs, Env.getQueueUrl());
    }

    @Provides
    @Singleton
    public MessageProcessorProxy provideMessageProcessorProxy(final AWSLambda lambda) {
        return new MessageProcessorProxy(Env.getMessageProcessorFunctionName(), lambda);
    }

    @Provides
    @Singleton
    public AWSLambda provideAWSLambda() {
        return AWSLambdaClientBuilder.standard()
                .build();
    }
}
