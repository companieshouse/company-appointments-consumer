package uk.gov.companieshouse.appointments.subdelta.companyprofile;

import static uk.gov.companieshouse.appointments.subdelta.Application.NAMESPACE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.appointments.subdelta.exception.NonRetryableException;
import uk.gov.companieshouse.appointments.subdelta.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
public class CompanyProfileChangedService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String DESERIALISE_FAILED_MESSAGE = "Failed to deserialise company profile data";
    private static final String DESERIALISE_SUCCEEDED_MESSAGE = "Deserialise company profile data succeeded";
    private static final String EXISTING_APPOINTMENTS_URI_SUFFIX = "/appointments";

    private final AppointmentsClient appointmentsClient;
    private final ObjectMapper objectMapper;

    public CompanyProfileChangedService(AppointmentsClient appointmentsClient, ObjectMapper objectMapper) {
        this.appointmentsClient = appointmentsClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void processMessage(ResourceChangedData changedData) {
        Data companyProfileData;
        try {
            companyProfileData = objectMapper.readValue(changedData.getData(), Data.class);
        } catch (JsonProcessingException ex) {
            LOGGER.error(DESERIALISE_FAILED_MESSAGE, DataMapHolder.getLogMap());
            throw new NonRetryableException(DESERIALISE_FAILED_MESSAGE, ex);
        }

        LOGGER.debug(DESERIALISE_SUCCEEDED_MESSAGE, DataMapHolder.getLogMap());
        String uri = changedData.getResourceUri() + EXISTING_APPOINTMENTS_URI_SUFFIX;

        appointmentsClient.patchCompanyNameAndStatusForAllAppointments(uri, companyProfileData.getCompanyName(),
                companyProfileData.getCompanyStatus());
    }
}
