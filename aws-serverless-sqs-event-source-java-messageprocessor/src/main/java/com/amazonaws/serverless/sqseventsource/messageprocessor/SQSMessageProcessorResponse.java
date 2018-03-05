package com.amazonaws.serverless.sqseventsource.messageprocessor;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Object to be returned to SQS poller from delegate lambda function. Contains message processing results.
 */
@Data
@NoArgsConstructor
// Default constructor required by Jackson
@AllArgsConstructor
public class SQSMessageProcessorResponse {
    @NonNull
    private List<SQSMessageResult> messageResults;
}
