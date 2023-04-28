package uk.gov.companieshouse.appointments.subdelta;

import uk.gov.companieshouse.stream.ResourceChangedData;

public class RouterParameters {

    private final ResourceChangedData resourceChangedData;
    private final String topic;

    public RouterParameters(ResourceChangedData resourceChangedData, String topic) {
        this.resourceChangedData = resourceChangedData;
        this.topic = topic;
    }

    public ResourceChangedData getResourceChangedData() {
        return resourceChangedData;
    }

    public String getTopic() {
        return topic;
    }
}
