package uk.gov.companieshouse.appointments.subdelta.companyprofile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.appointment.PatchAppointmentNameStatusApi;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.delta.PrivateDeltaResourceHandler;
import uk.gov.companieshouse.api.handler.delta.company.appointment.request.PrivateCompanyAppointmentPatch;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.http.HttpClient;

@ExtendWith(MockitoExtension.class)
class AppointmentsClientTest {

    private static final String RESOURCE_URI = "resource URI";
    private static final String COMPANY_NAME = "company name";
    private static final String COMPANY_STATUS = "company status";

    @Mock
    private Supplier<InternalApiClient> clientSupplier;
    @Mock
    private ResponseHandler responseHandler;
    @InjectMocks
    private AppointmentsClient client;
    @Mock
    private InternalApiClient apiClient;
    @Mock
    private HttpClient httpClient;
    @Mock
    private PrivateDeltaResourceHandler resourceHandler;
    @Mock
    private PrivateCompanyAppointmentPatch appointmentPatch;

    @BeforeEach
    void setUp() {
        when(clientSupplier.get()).thenReturn(apiClient);
        when(apiClient.getHttpClient()).thenReturn(httpClient);
        when(apiClient.privateDeltaResourceHandler()).thenReturn(resourceHandler);
        when(resourceHandler.patchCompanyAppointment(any(), any())).thenReturn(appointmentPatch);
    }

    @Test
    @DisplayName("Should execute http request successfully with no exceptions")
    void patchCompanyNameAndStatusForAllAppointments() {
        // given

        // when
        client.patchCompanyNameAndStatusForAllAppointments(RESOURCE_URI, COMPANY_NAME, COMPANY_STATUS);

        // then
        verify(resourceHandler).patchCompanyAppointment(RESOURCE_URI,
                new PatchAppointmentNameStatusApi()
                        .companyName(COMPANY_NAME)
                        .companyStatus(COMPANY_STATUS));
    }

    @Test
    @DisplayName("Should delegate to response handler when ApiErrorResponseException caught")
    void patchCompanyNameAndStatusForAllAppointmentsApiErrorResponseException()
            throws ApiErrorResponseException, URIValidationException {
        // given
        HttpResponseException.Builder builder = new HttpResponseException.Builder(503, "service unavailable",
                new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(builder);

        when(appointmentPatch.execute()).thenThrow(apiErrorResponseException);

        // when
        client.patchCompanyNameAndStatusForAllAppointments(RESOURCE_URI, COMPANY_NAME, COMPANY_STATUS);

        // then
        verify(resourceHandler).patchCompanyAppointment(RESOURCE_URI,
                new PatchAppointmentNameStatusApi()
                        .companyName(COMPANY_NAME)
                        .companyStatus(COMPANY_STATUS));
        verify(responseHandler).handle(
                String.format("HTTP response code 503 when updating appointment(s) for resource URI %s", RESOURCE_URI),
                apiErrorResponseException);
    }

    @Test
    @DisplayName("Should delegate to response handler when URIValidationException caught")
    void patchCompanyNameAndStatusForAllAppointmentsURIValidationException()
            throws ApiErrorResponseException, URIValidationException {
        // given
        URIValidationException uriValidationException = new URIValidationException("Invalid URI");
        when(appointmentPatch.execute()).thenThrow(uriValidationException);

        // when
        client.patchCompanyNameAndStatusForAllAppointments(RESOURCE_URI, COMPANY_NAME, COMPANY_STATUS);

        // then
        verify(resourceHandler).patchCompanyAppointment(RESOURCE_URI,
                new PatchAppointmentNameStatusApi()
                        .companyName(COMPANY_NAME)
                        .companyStatus(COMPANY_STATUS));
        verify(responseHandler).handle(
                String.format("Failed updating appointment(s) for resource URI %s", RESOURCE_URI),
                uriValidationException);
    }
}