package com.amazonaws.serverless.sqseventsource.lambda;

import com.amazonaws.serverless.sqseventsource.SQSPoller;
import com.amazonaws.serverless.sqseventsource.dagger.AppComponent;
import com.amazonaws.serverless.sqseventsource.dagger.DaggerAppComponent;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;

/**
 * SQS Poller lambda invoked by Cloudwatch events.
 */
public class SQSPollerHandler implements RequestHandler<ScheduledEvent, Void> {
    private SQSPoller sqsPoller;

    public SQSPollerHandler() {
        AppComponent component = DaggerAppComponent.create();
        sqsPoller = component.getSQSPoller();
    }

    @Override
    public Void handleRequest(final ScheduledEvent event, final Context context) {
        sqsPoller.poll(context.getRemainingTimeInMillis());
        return null;
    }
}
