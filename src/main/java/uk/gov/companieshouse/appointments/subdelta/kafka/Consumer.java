package uk.gov.companieshouse.appointments.subdelta.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.FixedDelayStrategy;
import org.springframework.kafka.retrytopic.RetryTopicHeaders;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.appointments.subdelta.companyprofile.ServiceRouter;
import uk.gov.companieshouse.appointments.subdelta.exception.RetryableException;
import uk.gov.companieshouse.appointments.subdelta.logging.DataMapHolder;
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
     * Consume a message from the company-profile Kafka topic.
     *
     * @param message A message containing a payload.
     */
    @KafkaListener(
            id = "${consumer.group_id}",
            containerFactory = "kafkaListenerContainerFactory",
            topics = {"${consumer.profile.topic}"},
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
            @Header(name = RetryTopicHeaders.DEFAULT_HEADER_ATTEMPTS, required = false) Integer attempt,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) Integer partition,
            @Header(KafkaHeaders.OFFSET) Long offset) {
        try {
            DataMapHolder.get().companyNumber(message.getPayload().getResourceId());
            router.route(message.getPayload());
        } catch (RetryableException exception) {
            messageFlags.setRetryable(true);
            throw exception;
        }
    }
}
