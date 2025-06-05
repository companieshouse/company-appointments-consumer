package uk.gov.companieshouse.appointments.subdelta.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.companieshouse.appointments.subdelta.kafka.TestUtils.INTEGRATION;
import static uk.gov.companieshouse.appointments.subdelta.kafka.TestUtils.STREAM_COMPANY_PROFILE_ERROR_TOPIC;
import static uk.gov.companieshouse.appointments.subdelta.kafka.TestUtils.STREAM_COMPANY_PROFILE_INVALID_TOPIC;
import static uk.gov.companieshouse.appointments.subdelta.kafka.TestUtils.STREAM_COMPANY_PROFILE_RETRY_TOPIC;
import static uk.gov.companieshouse.appointments.subdelta.kafka.TestUtils.STREAM_COMPANY_PROFILE_TOPIC;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import static java.time.temporal.ChronoUnit.SECONDS;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import uk.gov.companieshouse.appointments.subdelta.companyprofile.ServiceRouter;
import uk.gov.companieshouse.appointments.subdelta.exception.RetryableException;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Tag(INTEGRATION)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsumerRetryableExceptionTest extends AbstractKafkaTest {

    @Autowired
    private KafkaConsumer<String, byte[]> testConsumer;
    @Autowired
    private KafkaProducer<String, byte[]> testProducer;
    @Autowired
    private TestConsumerAspect testConsumerAspect;

    @MockitoBean
    private ServiceRouter router;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("steps", () -> 5);
    }

    @BeforeEach
    public void setup() {
        kafka.withStartupTimeout(Duration.of(300, SECONDS));
        testConsumerAspect.resetLatch();
        testConsumer.poll(Duration.ofMillis(1000));
    }

    @Test
    void testRepublishToCompanyProfileErrorTopicThroughRetryTopics() throws Exception {
        //given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<ResourceChangedData> writer = new ReflectDatumWriter<>(ResourceChangedData.class);
        writer.write(new ResourceChangedData("", "", "context_id", "12345678", "{}",
                new EventRecord("", "", Collections.emptyList())), encoder);

        doThrow(RetryableException.class).when(router).route(any());

        //when
        testProducer.send(
                new ProducerRecord<>(STREAM_COMPANY_PROFILE_TOPIC, 0, System.currentTimeMillis(),
                        "key", outputStream.toByteArray()));
        if (!testConsumerAspect.getLatch().await(30L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }

        //then
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(10000L), 6);
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, STREAM_COMPANY_PROFILE_TOPIC)).isEqualTo(1);
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, STREAM_COMPANY_PROFILE_RETRY_TOPIC))
                .isEqualTo(4);
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, STREAM_COMPANY_PROFILE_ERROR_TOPIC))
                .isEqualTo(1);
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, STREAM_COMPANY_PROFILE_INVALID_TOPIC)).isZero();
        verify(router, times(5)).route(any());
    }
}
