package uk.gov.companieshouse.appointments.subdelta;

import uk.gov.companieshouse.stream.ResourceChangedData;

/**
 * Contains all parameters required by {@link Service service implementations}.
 */
public class ServiceParameters {

    private final ResourceChangedData resourceChangedData;
    private final String updatedBy;

    public ServiceParameters(ResourceChangedData resourceChangedData, String updatedBy) {
        this.resourceChangedData = resourceChangedData;
        this.updatedBy = updatedBy;
    }

    public ResourceChangedData getResourceChangedData() {
        return resourceChangedData;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}
