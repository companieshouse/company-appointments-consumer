package uk.gov.companieshouse.appointments.subdelta;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

public final class TestUtils {

    public static final String STREAM_COMPANY_OFFICERS_TOPIC = "stream-company-officers";
    public static final String STREAM_COMPANY_OFFICERS_RETRY_TOPIC = "stream-company-officers-company-appointments-consumer-retry";
    public static final String STREAM_COMPANY_OFFICERS_ERROR_TOPIC = "stream-company-officers-company-appointments-consumer-error";
    public static final String STREAM_COMPANY_OFFICERS_INVALID_TOPIC = "stream-company-officers-company-appointments-consumer-invalid";
    public static final String STREAM_COMPANY_PROFILE_TOPIC = "stream-company-profile";
    public static final String STREAM_COMPANY_PROFILE_RETRY_TOPIC = "stream-company-profile-company-appointments-consumer-retry";
    public static final String STREAM_COMPANY_PROFILE_ERROR_TOPIC = "stream-company-profile-company-appointments-consumer-error";
    public static final String STREAM_COMPANY_PROFILE_INVALID_TOPIC = "stream-company-profile-company-appointments-consumer-invalid";

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
