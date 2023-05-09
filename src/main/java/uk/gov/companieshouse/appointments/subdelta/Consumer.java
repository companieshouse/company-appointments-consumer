package uk.gov.companieshouse.appointments.subdelta;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.FixedDelayStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.stream.ResourceChangedData;

/**
 * Consumes messages from the configured main Kafka topics.
 */
@Component
public class Consumer {

    private final ServiceRouter router;
    private final MessageFlags messageFlags;

    public Consumer(ServiceRouter router, MessageFlags messageFlags) {
        this.router = router;
        this.messageFlags = messageFlags;
    }

    /**
     * Consume a message from one of the main Kafka topics.
     *
     * @param message A message containing a payload.
     */
    @KafkaListener(
            id = "${consumer.group_id}",
            containerFactory = "kafkaListenerContainerFactory",
            topics = {"${consumer.officers.topic}", "${consumer.profile.topic}"},
            groupId = "${consumer.group_id}"
    )
    @RetryableTopic(
            attempts = "${consumer.max_attempts}",
            autoCreateTopics = "false",
            backoff = @Backoff(delayExpression = "${consumer.backoff_delay}"),
            retryTopicSuffix = "-${consumer.group_id}-retry",
            dltTopicSuffix = "-${consumer.group_id}-error",
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            fixedDelayTopicStrategy = FixedDelayStrategy.SINGLE_TOPIC,
            include = RetryableException.class
    )
    public void consume(Message<ResourceChangedData> message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            router.route(new RouterParameters(message.getPayload(), topic));
        } catch (RetryableException exception) {
            messageFlags.setRetryable(true);
            throw exception;
        }
    }
}
