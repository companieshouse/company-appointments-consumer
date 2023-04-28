package uk.gov.companieshouse.appointments.subdelta;

import static uk.gov.companieshouse.appointments.subdelta.Application.LOGGER;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
public class ServiceRouter {

    private static final String NOT_PROCESSED_MESSAGE = "Message was not processed as event type was not 'changed'";
    private static final String EVENT_TYPE_CHANGED = "changed";

    private final Service companyAppointmentService;
    private final Service companyProfileService;
    private final String companyOfficersTopic;
    private final String companyProfileTopic;

    public ServiceRouter(Service companyAppointmentService, Service companyProfileService,
            @Value("${consumer.officers.topic}") String companyOfficersTopic,
            @Value("${consumer.profile.topic}") String companyProfileTopic) {
        this.companyOfficersTopic = companyOfficersTopic;
        this.companyProfileTopic = companyProfileTopic;
        this.companyAppointmentService = companyAppointmentService;
        this.companyProfileService = companyProfileService;
    }

    public void route(RouterParameters parameters) {
        ResourceChangedData changedData = parameters.getResourceChangedData();

        if (EVENT_TYPE_CHANGED.equals(changedData.getEvent().getType())) {
            String topic = parameters.getTopic();

            if (topic.startsWith(companyOfficersTopic)) {
                companyAppointmentService.processMessage(changedData);
            } else if (topic.startsWith(companyProfileTopic)) {
                companyProfileService.processMessage(changedData);
            } else {
                throw new NonRetryableException(String.format("Invalid topic name: [%s]", topic));
            }
        } else {
            LOGGER.debug(NOT_PROCESSED_MESSAGE);
        }
    }
}
