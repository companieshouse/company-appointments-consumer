package uk.gov.companieshouse.appointments.subdelta;

import static uk.gov.companieshouse.appointments.subdelta.Application.LOGGER;

import java.io.IOException;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.kafka.common.serialization.Deserializer;
import uk.gov.companieshouse.stream.ResourceChangedData;

public class ResourceChangedDataDeserialiser implements Deserializer<ResourceChangedData> {

    @Override
    public ResourceChangedData deserialize(String topic, byte[] data) {
        try {
            Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            DatumReader<ResourceChangedData> reader = new ReflectDatumReader<>(
                    ResourceChangedData.class);
            return reader.read(null, decoder);
        } catch (IOException | AvroRuntimeException e) {
            LOGGER.error("Error deserialising message.", e);
            throw new InvalidPayloadException(
                    String.format("Invalid payload: [%s] was provided.", new String(data)), e);
        }
    }
}
