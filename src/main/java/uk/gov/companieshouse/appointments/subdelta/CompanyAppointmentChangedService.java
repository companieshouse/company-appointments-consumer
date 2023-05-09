package uk.gov.companieshouse.appointments.subdelta;

import static uk.gov.companieshouse.appointments.subdelta.Application.NAMESPACE;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

/**
 * The default service.
 */
@Component
class CompanyAppointmentChangedService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String COMPANY_PROFILE_NOT_FOUND_MESSAGE = "Company profile not found for %s";

    private final CompanyProfileClient companyProfileClient;
    private final AppointmentsClient appointmentsClient;
    private final CompanyNumberExtractor companyNumberExtractor;

    public CompanyAppointmentChangedService(CompanyProfileClient companyProfileClient,
            AppointmentsClient appointmentsClient, CompanyNumberExtractor companyNumberExtractor) {
        this.companyProfileClient = companyProfileClient;
        this.appointmentsClient = appointmentsClient;
        this.companyNumberExtractor = companyNumberExtractor;
    }

    @Override
    public void processMessage(ResourceChangedData changedData) {
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
    }
}