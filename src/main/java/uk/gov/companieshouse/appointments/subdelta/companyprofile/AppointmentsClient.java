package uk.gov.companieshouse.appointments.subdelta.companyprofile;

import static uk.gov.companieshouse.appointments.subdelta.Application.NAMESPACE;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.appointment.PatchAppointmentNameStatusApi;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.appointments.subdelta.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class AppointmentsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    private static final String FAILED_MSG = "Failed updating appointment(s) for resource URI %s";
    private static final String ERROR_MSG = "HTTP response code %s when updating appointment(s) for resource URI %s";

    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final ResponseHandler responseHandler;

    public AppointmentsClient(Supplier<InternalApiClient> internalApiClientFactory,
            ResponseHandler responseHandler) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.responseHandler = responseHandler;
    }

    public void patchCompanyNameAndStatusForAllAppointments(String resourceUri, String companyName, String status) {
        InternalApiClient client = internalApiClientFactory.get();
        client.getHttpClient().setRequestId(DataMapHolder.getRequestId());
        try {
            client.privateDeltaResourceHandler()
                    .patchCompanyAppointment(resourceUri, new PatchAppointmentNameStatusApi()
                            .companyName(companyName)
                            .companyStatus(status))
                    .execute();
        } catch (ApiErrorResponseException ex) {
            responseHandler.handle(
                    String.format(ERROR_MSG, ex.getStatusCode(), resourceUri), ex);
        } catch (IllegalArgumentException ex) {
            responseHandler.handle(String.format(FAILED_MSG, resourceUri), ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(String.format(FAILED_MSG, resourceUri), ex);
        }
        LOGGER.info("Successfully called PATCH endpoint on Company Profile API", DataMapHolder.getLogMap());
    }
}
