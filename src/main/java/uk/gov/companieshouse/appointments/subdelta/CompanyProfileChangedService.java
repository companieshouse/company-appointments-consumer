package uk.gov.companieshouse.appointments.subdelta;

import static uk.gov.companieshouse.appointments.subdelta.Application.LOGGER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.stream.ResourceChangedData;

public class CompanyProfileChangedService implements Service {

    private static final String DESERIALISE_FAILED_MESSAGE = "Failed to deserialise company profile data: [%s]";
    private static final String EXISTING_APPOINTMENTS_URI_SUFFIX = "/appointments";

    private final AppointmentsClient appointmentsClient;

    public CompanyProfileChangedService(AppointmentsClient appointmentsClient) {
        this.appointmentsClient = appointmentsClient;
    }

    @Override
    public void processMessage(ResourceChangedData changedData) {
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
    }
}
