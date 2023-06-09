package uk.gov.companieshouse.appointments.subdelta;

import static uk.gov.companieshouse.appointments.subdelta.Application.NAMESPACE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
public class CompanyProfileChangedService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String DESERIALISE_FAILED_MESSAGE = "Failed to deserialise company profile data: [%s]";
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
            LOGGER.debug(String.format(DESERIALISE_FAILED_MESSAGE, changedData.getResourceUri()));
            throw new NonRetryableException(String.format(DESERIALISE_FAILED_MESSAGE, changedData.getResourceUri()), ex);
        }

        String uri = changedData.getResourceUri() + EXISTING_APPOINTMENTS_URI_SUFFIX;
        appointmentsClient.patchCompanyNameAndStatus(uri, companyProfileData.getCompanyName(),
                companyProfileData.getCompanyStatus(), changedData.getContextId());
    }
}
