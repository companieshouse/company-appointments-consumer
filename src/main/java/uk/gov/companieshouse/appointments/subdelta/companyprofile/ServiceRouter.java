package uk.gov.companieshouse.appointments.subdelta.companyprofile;

import static uk.gov.companieshouse.appointments.subdelta.Application.NAMESPACE;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.appointments.subdelta.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
public class ServiceRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String NOT_PROCESSED_MESSAGE = "Message was not processed as event type was not 'changed'";
    private static final String EVENT_TYPE_CHANGED = "changed";

    private final Service companyProfileChangedService;

    public ServiceRouter(Service companyProfileService) {
        this.companyProfileChangedService = companyProfileService;
    }

    public void route(ResourceChangedData changedData) {
        if (EVENT_TYPE_CHANGED.equals(changedData.getEvent().getType())) {
            companyProfileChangedService.processMessage(changedData);
        } else {
            LOGGER.debug(NOT_PROCESSED_MESSAGE, DataMapHolder.getLogMap());
        }
    }
}
