package uk.gov.companieshouse.appointments.subdelta.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import uk.gov.companieshouse.appointments.subdelta.exception.InvalidPayloadException;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

class ResourceChangedDataDeserialiserTest {

    @Test
    @DisplayName("Deserialise a ResourceChangedData serialised as Avro")
    void testDeserialiseDelta() throws IOException {
        try (ResourceChangedDataDeserialiser deserialiser = new ResourceChangedDataDeserialiser()) {

            // given
            ResourceChangedData changeData = new ResourceChangedData("resource_kind",
                    "resource_uri", "context_id", "resource_id", "data",
                    new EventRecord("published_at", "event_type", Collections.emptyList()));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
            DatumWriter<ResourceChangedData> writer = new ReflectDatumWriter<>(
                    ResourceChangedData.class);
            writer.write(changeData, encoder);

            // when
            ResourceChangedData actual = deserialiser.deserialize("topic",
                    outputStream.toByteArray());

            // then
            assertThat(actual).isEqualTo(changeData);
        }
    }

    @Test
    @DisplayName("Throws InvalidPayloadException if IOException encountered when deserialising a message")
    void testDeserialiseDataThrowsInvalidPayloadExceptionIfIOExceptionEncountered()
            throws IOException {
        // given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<String> writer = new SpecificDatumWriter<>(String.class);
        writer.write("hello", encoder);
        ResourceChangedDataDeserialiser deserialiser = new ResourceChangedDataDeserialiser();

        // when
        Executable actual = () -> deserialiser.deserialize("topic", outputStream.toByteArray());

        // then
        InvalidPayloadException exception = assertThrows(InvalidPayloadException.class, actual);
        // Note the '\n' is the length prefix of the invalid data sent to the deserialiser
        assertThat(exception.getMessage()).isEqualTo("Invalid payload: [\nhello] was provided.");
        assertThat(exception.getCause()).isInstanceOf(IOException.class);
        deserialiser.close();
    }

    @Test
    @DisplayName("Throws InvalidPayloadException if AvroRuntimeException encountered when deserialising a message")
    void testDeserialiseDataThrowsInvalidPayloadExceptionIfAvroRuntimeExceptionEncountered() {
        // given
        ResourceChangedDataDeserialiser deserialiser = new ResourceChangedDataDeserialiser();

        // when
        Executable actual = () -> deserialiser.deserialize("topic", "invalid".getBytes(
                StandardCharsets.UTF_8));

        // then
        InvalidPayloadException exception = assertThrows(InvalidPayloadException.class, actual);
        assertThat(exception.getMessage()).isEqualTo("Invalid payload: [invalid] was provided.");
        assertThat(exception.getCause()).isInstanceOf(AvroRuntimeException.class);
        deserialiser.close();
    }
}