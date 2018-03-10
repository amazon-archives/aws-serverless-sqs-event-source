package com.amazonaws.serverless.sqseventsource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.google.common.base.Preconditions;

import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for managing message processing timing statistics.
 */
@Slf4j
class MessageProcessingStats {
    private final List<Long> samples = new ArrayList<>();

    public void record(final Duration processingDuration, final int numMessages) {
        long processingTimeInMillis = processingDuration.toMillis();
        long perMessageAverage = processingTimeInMillis / numMessages;
        log.info("Processed {} messages in {}ms. perMessageAverage={}ms", numMessages, processingTimeInMillis, perMessageAverage);
        samples.addAll(
                LongStream.iterate(perMessageAverage, LongUnaryOperator.identity())
                        .limit(numMessages)
                        .boxed()
                        .collect(Collectors.toList()));
    }

    /**
     * @return <code>true</code> if messages processing stats have been recorded.
     */
    public boolean hasSamples() {
        return !samples.isEmpty();
    }

    /**
     * Calculates estimated number of messages that can be processed in the given time duration based on previous processing time samples.
     *
     * @param duration Time duration to use for estimating capacity.
     * @return The estimated number of messages that can be processed in the given time duration.
     */
    public int getEstimatedCapacity(Duration duration) {
        Preconditions.checkState(hasSamples(), "Cannot compute estimated capacity without any timing samples recorded.");
        double perMessageProcessingAverageInMillis = samples.stream()
                .collect(Collectors.summarizingLong(Long::longValue))
                .getAverage();

        int estimatedCapacity = (int) (((double) duration.toMillis()) / perMessageProcessingAverageInMillis);
        log.info("Estimated capacity of {} messages can be processed in {}ms based on samples: {}", estimatedCapacity, duration.toMillis(), samples);
        return estimatedCapacity;
    }
}
