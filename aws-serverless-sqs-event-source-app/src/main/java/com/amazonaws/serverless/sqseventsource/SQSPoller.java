package com.amazonaws.serverless.sqseventsource;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.amazonaws.services.sqs.model.Message;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Polls an SQS queue and delegates messages processing to a user-defined lambda function.
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class SQSPoller {
    static final int TIMEOUT_BUFFER_IN_MILLIS = 5000;

    @NonNull
    private final SQSProxy sqsProxy;
    @NonNull
    private final MessageDispatcher messageDispatcher;
    @NonNull
    private final Clock clock;

    public SQSPoller(final SQSProxy sqsProxy, final MessageDispatcher messageDispatcher) {
        this(sqsProxy, messageDispatcher, Clock.systemUTC());
    }

    public void poll(final int remainingTimeInMillis) {
        Instant cutoff = Instant.now(clock)
                .plusMillis(remainingTimeInMillis)
                .minusMillis(TIMEOUT_BUFFER_IN_MILLIS);
        messageDispatcher.reset();
        int estimatedCapacity;
        while ((estimatedCapacity = messageDispatcher.getEstimatedCapacity(cutoff)) > 0) {
            List<Message> toProcess = sqsProxy.receiveMessages(estimatedCapacity);

            if (toProcess.isEmpty()) {
                log.info("No messages received from queue. Returning until next polling cycle to save cost.");
                return;
            }

            messageDispatcher.dispatch(toProcess);
        }
    }
}
