package uk.gov.companieshouse.appointments.subdelta.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

public final class TestUtils {

    static final String STREAM_COMPANY_PROFILE_TOPIC = "stream-company-profile";
    static final String STREAM_COMPANY_PROFILE_RETRY_TOPIC = "stream-company-profile-company-appointments-consumer-retry";
    static final String STREAM_COMPANY_PROFILE_ERROR_TOPIC = "stream-company-profile-company-appointments-consumer-error";
    static final String STREAM_COMPANY_PROFILE_INVALID_TOPIC = "stream-company-profile-company-appointments-consumer-invalid";

    private TestUtils() {
    }

    public static int noOfRecordsForTopic(ConsumerRecords<?, ?> records, String topic) {
        int count = 0;
        for (ConsumerRecord<?, ?> ignored : records.records(topic)) {
            count++;
        }
        return count;
    }
}
