package uk.gov.companieshouse.appointments.subdelta;

/**
 * Processes an incoming message.
 */
public interface Service {

    /**
     * Processes an incoming message.
     *
     * @param parameters Any parameters required when processing the message.
     */
    void processChangedCompanyAppointment(ServiceParameters parameters);

    void processChangedCompanyProfile(ServiceParameters parameters);
}