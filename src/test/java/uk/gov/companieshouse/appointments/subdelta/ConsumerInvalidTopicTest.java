package uk.gov.companieshouse.appointments.subdelta;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
        topics = {"stream-company-officers",
                "stream-company-officers-company-appointments-consumer-retry",
                "stream-company-officers-company-appointments-consumer-error",
                "stream-company-officers-company-appointments-consumer-invalid"},
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
                new ProducerRecord<>("stream-company-officers",
                        0, System.currentTimeMillis(), "key", outputStream.toByteArray()));
        future.get();
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, 10000L, 2);

        //then
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-officers"),
                is(1));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords,
                "stream-company-officers-company-appointments-consumer-retry"), is(0));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords,
                "stream-company-officers-company-appointments-consumer-error"), is(0));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords,
                "stream-company-officers-company-appointments-consumer-invalid"), is(1));
    }
}
