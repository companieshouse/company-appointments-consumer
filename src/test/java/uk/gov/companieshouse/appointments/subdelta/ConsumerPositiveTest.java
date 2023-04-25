package uk.gov.companieshouse.appointments.subdelta;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

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
@TestPropertySource(locations = "classpath:application-test_main_positive.properties")
@Import(TestConfig.class)
@ActiveProfiles("test_main_positive")
class ConsumerPositiveTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaConsumer<String, byte[]> testConsumer;

    @Autowired
    private KafkaProducer<String, byte[]> testProducer;

    @Autowired
    private CountDownLatch latch;

    @MockBean
    private Service service;

    @Test
    void testConsumeFromMainTopic() throws Exception {
        //given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<ResourceChangedData> writer = new ReflectDatumWriter<>(
                ResourceChangedData.class);
        writer.write(new ResourceChangedData("", "", "", "", "{}",
                new EventRecord("", "", Collections.emptyList())), encoder);

        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(testConsumer);

        //when
        testProducer.send(
                new ProducerRecord<>("stream-company-officers", 0, System.currentTimeMillis(),
                        "key", outputStream.toByteArray()));
        if (!latch.await(5L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }

        //then
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, 10000L, 1);
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords, "stream-company-officers"),
                is(1));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords,
                "stream-company-officers-company-appointments-consumer-retry"), is(0));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords,
                "stream-company-officers-company-appointments-consumer-error"), is(0));
        assertThat(TestUtils.noOfRecordsForTopic(consumerRecords,
                "stream-company-officers-company-appointments-consumer-invalid"), is(0));
        verify(service).processMessage(any());
    }
}
