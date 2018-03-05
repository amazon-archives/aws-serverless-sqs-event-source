package com.amazonaws.serverless.sqseventsource.messageprocessor;

import java.util.List;

import com.amazonaws.services.sqs.model.Message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Object passed from SQS poller to delegate lambda function. Contains a batch of messages to be processed.
 */
@Data
@AllArgsConstructor
// Default constructor required by Jackson
@NoArgsConstructor
public class SQSMessageProcessorRequest {
    private List<Message> messages;
}
