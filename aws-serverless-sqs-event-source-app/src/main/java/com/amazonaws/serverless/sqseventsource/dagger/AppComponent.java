package com.amazonaws.serverless.sqseventsource.dagger;

import javax.inject.Singleton;

import com.amazonaws.serverless.sqseventsource.SQSPoller;

import dagger.Component;

/**
 * Application component interface.
 */
@Singleton
@Component(modules = AppModule.class)
public interface AppComponent {
    SQSPoller getSQSPoller();
}
