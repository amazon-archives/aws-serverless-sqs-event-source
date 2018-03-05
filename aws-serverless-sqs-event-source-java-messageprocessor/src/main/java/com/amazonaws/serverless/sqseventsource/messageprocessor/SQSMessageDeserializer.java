package com.amazonaws.serverless.sqseventsource.messageprocessor;

import com.amazonaws.services.sqs.model.Message;

/**
 * Deserializes an SQS message into the parameterized type.
 *
 * @param <T> deserialize type.
 */
public interface SQSMessageDeserializer<T> {
    /**
     * Deserializes the given message into an object.
     *
     * @param message SQS message to deserialize.
     * @return deserialized result.
     */
    T deserialize(Message message);
}
