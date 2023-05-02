package uk.gov.companieshouse.appointments.subdelta;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.companieshouse.appointments.subdelta.TestUtils.STREAM_COMPANY_OFFICERS_ERROR_TOPIC;
import static uk.gov.companieshouse.appointments.subdelta.TestUtils.STREAM_COMPANY_OFFICERS_INVALID_TOPIC;
import static uk.gov.companieshouse.appointments.subdelta.TestUtils.STREAM_COMPANY_OFFICERS_RETRY_TOPIC;
import static uk.gov.companieshouse.appointments.subdelta.TestUtils.STREAM_COMPANY_OFFICERS_TOPIC;
import static uk.gov.companieshouse.appointments.subdelta.TestUtils.STREAM_COMPANY_PROFILE_ERROR_TOPIC;
import static uk.gov.companieshouse.appointments.subdelta.TestUtils.STREAM_COMPANY_PROFILE_INVALID_TOPIC;
import static uk.gov.companieshouse.appointments.subdelta.TestUtils.STREAM_COMPANY_PROFILE_RETRY_TOPIC;
import static uk.gov.companieshouse.appointments.subdelta.TestUtils.STREAM_COMPANY_PROFILE_TOPIC;

import java.io.ByteArrayOutputStream;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = Application.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EmbeddedKafka(
        topics = {STREAM_COMPANY_OFFICERS_TOPIC,
                STREAM_COMPANY_OFFICERS_RETRY_TOPIC,
                STREAM_COMPANY_OFFICERS_ERROR_TOPIC,
                STREAM_COMPANY_OFFICERS_INVALID_TOPIC,
                STREAM_COMPANY_PROFILE_TOPIC,
                STREAM_COMPANY_PROFILE_RETRY_TOPIC,
                STREAM_COMPANY_PROFILE_ERROR_TOPIC,
                STREAM_COMPANY_PROFILE_INVALID_TOPIC},
        controlledShutdown = true,
        partitions = 1
)
@ActiveProfiles("test_main_nonretryable")
@Import(TestConfig.class)
class ConsumerInvalidTopicTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaConsumer<String, byte[]> testConsumer;

    @Autowired
    private KafkaProducer<String, byte[]> testProducer;

    @Test
    void testPublishToInvalidMessageTopicIfInvalidDataDeserialised() throws Exception {
        //given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<String> writer = new ReflectDatumWriter<>(String.class);
        writer.write("bad data", encoder);

        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(testConsumer);

        //when
        Future<RecordMetadata> future = testProducer.send(
                new ProducerRecord<>(STREAM_COMPANY_OFFICERS_TOPIC,
                        0, System.currentTimeMillis(), "key", outputStream.toByteArray()));
        future.get();
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, 10000L, 2);

        //then
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, STREAM_COMPANY_OFFICERS_TOPIC),
                is(1));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords,
                STREAM_COMPANY_OFFICERS_RETRY_TOPIC), is(0));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords,
                STREAM_COMPANY_OFFICERS_ERROR_TOPIC), is(0));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords,
                STREAM_COMPANY_OFFICERS_INVALID_TOPIC), is(1));
    }
}
