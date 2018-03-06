package com.amazonaws.serverless.sqseventsource;

import java.util.List;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Output;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import lombok.RequiredArgsConstructor;

/**
 * Helper for integ tests to get test environment stack information.
 */
@RequiredArgsConstructor
class TestStackHelper {
    private static final String INTEG_TEST_ENVIRONMENT_STACK_NAME = "integ-test-environment-" + System.getenv("TRAVIS_BUILD_ID");
    private static final String QUEUE_URL_OUTPUT_KEY = "MessageQueueUrl";

    private final Table<String, String, String> cache = HashBasedTable.create();

    private final AmazonCloudFormation cloudFormation;

    String getQueueUrl() {
        return getStackOutput(INTEG_TEST_ENVIRONMENT_STACK_NAME, QUEUE_URL_OUTPUT_KEY);
    }

    private String getStackOutput(String stackName, String outputKey) {
        if (!cache.contains(stackName, outputKey)) {
            List<Output> outputs = cloudFormation.describeStacks(new DescribeStacksRequest()
                    .withStackName(stackName))
                    .getStacks().get(0).getOutputs();
            outputs.forEach(o -> cache.put(stackName, o.getOutputKey(), o.getOutputValue()));
        }
        return cache.get(stackName, outputKey);
    }
}
