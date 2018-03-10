package com.amazonaws.serverless.sqseventsource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Duration;

import org.junit.Before;
import org.junit.Test;

public class MessageProcessingStatsTest {
    private MessageProcessingStats stats;

    @Before
    public void setup() throws Exception {
        stats = new MessageProcessingStats();
    }

    @Test
    public void endToEnd() throws Exception {
        assertThat(stats.hasSamples(), is(false));

        stats.record(Duration.ofMillis(200), 2);
        assertThat(stats.hasSamples(), is(true));

        assertThat(stats.getEstimatedCapacity(Duration.ofMillis(100)), is(1));
        assertThat(stats.getEstimatedCapacity(Duration.ofMillis(200)), is(2));
        assertThat(stats.getEstimatedCapacity(Duration.ofMillis(400)), is(4));
        assertThat(stats.getEstimatedCapacity(Duration.ofMillis(800)), is(8));

        stats.record(Duration.ofMillis(100), 2);
        assertThat(stats.hasSamples(), is(true));

        assertThat(stats.getEstimatedCapacity(Duration.ofMillis(100)), is(1));
        assertThat(stats.getEstimatedCapacity(Duration.ofMillis(200)), is(2));
        assertThat(stats.getEstimatedCapacity(Duration.ofMillis(400)), is(5));
        assertThat(stats.getEstimatedCapacity(Duration.ofMillis(800)), is(10));
        assertThat(stats.getEstimatedCapacity(Duration.ofMillis(1000)), is(13));
    }
}