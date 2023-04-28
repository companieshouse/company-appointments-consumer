package uk.gov.companieshouse.appointments.subdelta;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.stream.ResourceChangedData;

/**
 * The default service.
 */
@Component
class AppointmentsChangeService implements Service {

    private static final String CHANGED = "changed";
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
    public void processMessage(ServiceParameters parameters) {
        ResourceChangedData changedData = parameters.getResourceChangedData();

        if (CHANGED.equals(changedData.getEvent().getType())) {
            String companyNumber = companyNumberExtractor.extractFromUri(changedData.getResourceUri());

            Data companyProfile = companyProfileClient.fetchCompanyProfile(companyNumber,
                    changedData.getContextId())
                    .orElseThrow(() -> new NonRetryableException(
                            String.format("Company profile not found for %s", companyNumber)));

            appointmentsClient.patchCompanyNameAndStatus(changedData.getResourceUri(),
                    companyProfile.getCompanyName(), companyProfile.getCompanyStatus(),
                    changedData.getContextId());
        }
    }

    @Override
    public void processMessageForExistingAppointment(ServiceParameters parameters) {
        ResourceChangedData changedData = parameters.getResourceChangedData();
        String companyNumber = companyNumberExtractor.extractFromUri(changedData.getResourceUri());

        Data companyProfile = companyProfileClient.fetchCompanyProfile(companyNumber,
                        changedData.getContextId())
                .orElseThrow(() -> new NonRetryableException(
                        String.format("Company profile not found for %s", companyNumber)));

        appointmentsClient.patchExistingCompanyNameAndStatus(changedData.getResourceUri(),
                companyProfile.getCompanyName(), companyProfile.getCompanyStatus(),
                changedData.getContextId());
    }
}