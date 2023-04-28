package uk.gov.companieshouse.appointments.subdelta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

/**
 * The default service.
 */
@Component
class AppointmentsChangeService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.NAMESPACE);
    private static final String NOT_PROCESSED_MESSAGE = "Message was not processed as event type was not 'changed'";
    private static final String COMPANY_PROFILE_NOT_FOUND_MESSAGE = "Company profile not found for %s";
    private static final String DESERIALISE_FAILED_MESSAGE = "Failed to deserialise company profile data: [%s]";
    private static final String EXISTING_APPOINTMENTS_URI_SUFFIX = "/appointments";
    private static final String EVENT_TYPE_CHANGED = "changed";
    private final CompanyProfileClient companyProfileClient;
    private final AppointmentsClient appointmentsClient;
    private final CompanyNumberExtractor companyNumberExtractor;

    public AppointmentsChangeService(CompanyProfileClient companyProfileClient,
                                     AppointmentsClient appointmentsClient, CompanyNumberExtractor companyNumberExtractor) {
        this.companyProfileClient = companyProfileClient;
        this.appointmentsClient = appointmentsClient;
        this.companyNumberExtractor = companyNumberExtractor;
    }

    @Override
    public void processChangedCompanyAppointment(ServiceParameters parameters) {
        ResourceChangedData changedData = parameters.getResourceChangedData();

        if (EVENT_TYPE_CHANGED.equals(changedData.getEvent().getType())) {
            String companyNumber = companyNumberExtractor.extractFromUri(changedData.getResourceUri());

            Data companyProfile = companyProfileClient.fetchCompanyProfile(companyNumber,
                    changedData.getContextId())
                    .orElseThrow(() -> {
                        LOGGER.debug(String.format(COMPANY_PROFILE_NOT_FOUND_MESSAGE, companyNumber));
                        return new NonRetryableException(
                            String.format(COMPANY_PROFILE_NOT_FOUND_MESSAGE, companyNumber));
                    });

            appointmentsClient.patchCompanyNameAndStatus(changedData.getResourceUri(),
                    companyProfile.getCompanyName(), companyProfile.getCompanyStatus(),
                    changedData.getContextId());
        } else {
            LOGGER.debug(NOT_PROCESSED_MESSAGE);
        }
    }

    @Override
    public void processChangedCompanyProfile(ServiceParameters parameters) {
        ResourceChangedData changedData = parameters.getResourceChangedData();

        if (EVENT_TYPE_CHANGED.equals(changedData.getEvent().getType())) {
            Data companyProfileData;
            try {
                companyProfileData = new ObjectMapper().readValue(changedData.getData(), Data.class);
            } catch (JsonProcessingException ex) {
                LOGGER.debug(String.format(DESERIALISE_FAILED_MESSAGE, changedData.getResourceUri()));
                throw new NonRetryableException(String.format(DESERIALISE_FAILED_MESSAGE, changedData.getResourceUri()), ex);
            }

            String uri = changedData.getResourceUri() + EXISTING_APPOINTMENTS_URI_SUFFIX;
            appointmentsClient.patchCompanyNameAndStatus(uri, companyProfileData.getCompanyName(),
                    companyProfileData.getCompanyStatus(), changedData.getContextId());
        } else {
            LOGGER.debug(NOT_PROCESSED_MESSAGE);
        }
    }
}