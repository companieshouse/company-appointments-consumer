package uk.gov.companieshouse.appointments.subdelta;

import uk.gov.companieshouse.stream.ResourceChangedData;

/**
 * Contains all parameters required by {@link Service service implementations}.
 */
public class ServiceParameters {

    private final ResourceChangedData resourceChangedData;

    public ServiceParameters(ResourceChangedData resourceChangedData) {
        this.resourceChangedData = resourceChangedData;
    }

    public ResourceChangedData getResourceChangedData() {
        return resourceChangedData;
    }
}
