package uk.gov.companieshouse.appointments.subdelta;

import static org.springframework.kafka.support.KafkaHeaders.EXCEPTION_MESSAGE;
import static org.springframework.kafka.support.KafkaHeaders.ORIGINAL_OFFSET;
import static org.springframework.kafka.support.KafkaHeaders.ORIGINAL_PARTITION;
import static org.springframework.kafka.support.KafkaHeaders.ORIGINAL_TOPIC;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

/**
 * Routes a message to the invalid letter topic if a non-retryable error has been thrown during
 * message processing.
 */
public class InvalidMessageRouter implements ProducerInterceptor<String, ResourceChangedData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.NAMESPACE);

    private MessageFlags messageFlags;
    private String invalidMessageTopic;

    @Override
    public ProducerRecord<String, ResourceChangedData> onSend(
            ProducerRecord<String, ResourceChangedData> producerRecord) {
        if (messageFlags.isRetryable()) {
            messageFlags.destroy();
            return producerRecord;
        } else {

            String topic = Optional.ofNullable(producerRecord.headers().lastHeader(ORIGINAL_TOPIC))
                    .map(h -> new String(h.value())).orElse("unknown");
            BigInteger partition = Optional.ofNullable(
                            producerRecord.headers().lastHeader(ORIGINAL_PARTITION))
                    .map(h -> new BigInteger(h.value())).orElse(BigInteger.valueOf(-1));
            BigInteger offset = Optional.ofNullable(
                            producerRecord.headers().lastHeader(ORIGINAL_OFFSET))
                    .map(h -> new BigInteger(h.value())).orElse(BigInteger.valueOf(-1));
            String exception = Optional.ofNullable(
                            producerRecord.headers().lastHeader(EXCEPTION_MESSAGE))
                    .map(h -> new String(h.value()))
                    .orElse("unknown");

            ResourceChangedData invalidData = new ResourceChangedData("", "", "", "",
                    String.format(
                            "{ \"invalid_message\": \"exception: [ %s ] passed for topic: %s, partition: %d, offset: %d\" }",
                            exception, topic, partition, offset),
                    new EventRecord("", "", Collections.emptyList()));

            ProducerRecord<String, ResourceChangedData> invalidRecord = new ProducerRecord<>(
                    invalidMessageTopic, producerRecord.key(), invalidData);
            LOGGER.info(String.format("Moving record into topic: [%s]%nMessage content: %s",
                    invalidRecord.topic(), invalidData.getData()));

            return invalidRecord;
        }
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        //
    }

    @Override
    public void close() {
        //
    }

    @Override
    public void configure(Map<String, ?> configs) {
        this.messageFlags = (MessageFlags) configs.get("message.flags");
        this.invalidMessageTopic = (String) configs.get("invalid.message.topic");
    }
}
