package uk.gov.companieshouse.appointments.subdelta.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.companieshouse.appointments.subdelta.kafka.TestUtils.INTEGRATION;
import static uk.gov.companieshouse.appointments.subdelta.kafka.TestUtils.STREAM_COMPANY_PROFILE_ERROR_TOPIC;
import static uk.gov.companieshouse.appointments.subdelta.kafka.TestUtils.STREAM_COMPANY_PROFILE_INVALID_TOPIC;
import static uk.gov.companieshouse.appointments.subdelta.kafka.TestUtils.STREAM_COMPANY_PROFILE_RETRY_TOPIC;
import static uk.gov.companieshouse.appointments.subdelta.kafka.TestUtils.STREAM_COMPANY_PROFILE_TOPIC;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.concurrent.Future;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.wait.strategy.Wait;
import static java.time.temporal.ChronoUnit.SECONDS;

@Tag(INTEGRATION)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsumerInvalidTopicTest extends AbstractKafkaTest {

    @Autowired
    private KafkaConsumer<String, byte[]> testConsumer;
    @Autowired
    private KafkaProducer<String, byte[]> testProducer;
    @Autowired
    private TestConsumerAspect testConsumerAspect;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("steps", () -> 1);
    }

    @BeforeEach
    public void setup() {
                kafka.setWaitStrategy(Wait.defaultWaitStrategy()
                .withStartupTimeout(Duration.of(300, SECONDS)));
        testConsumerAspect.resetLatch();
        testConsumer.poll(Duration.ofMillis(1000));
    }

    @Test
    void testPublishToCompanyProfileInvalidMessageTopicIfInvalidDataDeserialised() throws Exception {
        //given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<String> writer = new ReflectDatumWriter<>(String.class);
        writer.write("bad data", encoder);

        //when
        Future<RecordMetadata> future = testProducer.send(
                new ProducerRecord<>(STREAM_COMPANY_PROFILE_TOPIC,
                        0, System.currentTimeMillis(), "key", outputStream.toByteArray()));
        future.get();
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(10000L), 2);

        //then
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, STREAM_COMPANY_PROFILE_TOPIC)).isEqualTo(1);
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, STREAM_COMPANY_PROFILE_RETRY_TOPIC)).isZero();
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, STREAM_COMPANY_PROFILE_ERROR_TOPIC)).isZero();
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, STREAM_COMPANY_PROFILE_INVALID_TOPIC)).isEqualTo(1);
    }
}
