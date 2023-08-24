package uk.gov.companieshouse.appointments.subdelta.companyprofile;

import uk.gov.companieshouse.stream.ResourceChangedData;

interface Service {

    void processMessage(ResourceChangedData changedData);
}