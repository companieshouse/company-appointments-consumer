package uk.gov.companieshouse.appointments.subdelta;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.appointment.PatchAppointmentNameStatusApi;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;

@Component
public class AppointmentsClient {

    private static final String FAILED_MSG = "Failed updating appointment for resource URI %s with context id %s";
    private static final String ERROR_MSG = "HTTP response code %s when updating appointment for resource URI %s with context id %s";

    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final ResponseHandler responseHandler;

    public AppointmentsClient(Supplier<InternalApiClient> internalApiClientFactory,
            ResponseHandler responseHandler) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.responseHandler = responseHandler;
    }

    public void patchCompanyNameAndStatus(String resourceUri, String companyName, String status,
            String contextId) {
        InternalApiClient client = internalApiClientFactory.get();

        try {
            client.privateDeltaResourceHandler()
                    .patchCompanyAppointment(resourceUri, new PatchAppointmentNameStatusApi()
                            .companyName(companyName)
                            .companyStatus(status))
                    .execute();
        } catch (ApiErrorResponseException ex) {
            responseHandler.handle(
                    String.format(ERROR_MSG, ex.getStatusCode(), resourceUri, contextId), ex);
        } catch (IllegalArgumentException ex) {
            responseHandler.handle(String.format(FAILED_MSG, resourceUri, contextId), ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(String.format(FAILED_MSG, resourceUri, contextId), ex);
        }
    }

    public void patchExistingCompanyNameAndStatus(String resourceUri, String companyName,
                                                  String status, String contextId) {
        InternalApiClient client = internalApiClientFactory.get();

        try {
            client.privateDeltaResourceHandler()
                    .patchExistingCompanyAppointment(resourceUri, new PatchAppointmentNameStatusApi()
                            .companyName(companyName)
                            .companyStatus(status))
                    .execute();
        } catch (ApiErrorResponseException ex) {
            responseHandler.handle(
                    String.format(ERROR_MSG, ex.getStatusCode(), resourceUri, contextId), ex);
        } catch (IllegalArgumentException ex) {
            responseHandler.handle(String.format(FAILED_MSG, resourceUri, contextId), ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(String.format(FAILED_MSG, resourceUri, contextId), ex);
        }
    }
}
