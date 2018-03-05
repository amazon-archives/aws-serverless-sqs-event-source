package com.amazonaws.serverless.sqseventsource.messageprocessor;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.amazonaws.services.sqs.model.Message;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for processing SQS messages.
 *
 * @param <T> type expected by delegate consumer.
 */
@RequiredArgsConstructor
@Slf4j
public class SQSMessageProcessor<T> implements Function<SQSMessageProcessorRequest, SQSMessageProcessorResponse> {
    @NonNull
    private final SQSMessageDeserializer<T> deserializer;
    @NonNull
    private final Consumer<T> delegate;

    @Override
    public SQSMessageProcessorResponse apply(final SQSMessageProcessorRequest request) {
        List<SQSMessageResult> messageResults = request.getMessages().stream()
                .map(this::processMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new SQSMessageProcessorResponse(messageResults);
    }

    private SQSMessageResult processMessage(final Message message) {
        try {
            T body = deserializer.deserialize(message);
            delegate.accept(body);
            return new SQSMessageResult(message.getMessageId(), SQSMessageResult.Status.SUCCESS);
        } catch (RetryMessageException e) {
            log.info("Retrying message {}", message, e);
            return new SQSMessageResult(message.getMessageId(), SQSMessageResult.Status.RETRY, e.getRetryDelayInSeconds());
        } catch (Exception e) {
            log.error("Unhandled exception while processing message {}", message, e);
            return new SQSMessageResult(message.getMessageId(), SQSMessageResult.Status.ERROR);
        }
    }
}
