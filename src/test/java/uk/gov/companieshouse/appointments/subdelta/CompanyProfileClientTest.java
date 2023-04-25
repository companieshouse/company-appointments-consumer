package uk.gov.companieshouse.appointments.subdelta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.company.PrivateCompanyResourceHandler;
import uk.gov.companieshouse.api.handler.company.request.PrivateCompanyFullProfileGet;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;

@ExtendWith(MockitoExtension.class)
class CompanyProfileClientTest {

    private static final String CONTEXT_ID = "context id";
    private static final String COMPANY_NUMBER = "company number";

    @Mock
    private Supplier<InternalApiClient> clientSupplier;
    @Mock
    private ResponseHandler responseHandler;
    @InjectMocks
    private CompanyProfileClient client;
    @Mock
    private InternalApiClient apiClient;
    @Mock
    private PrivateCompanyResourceHandler resourceHandler;
    @Mock
    private PrivateCompanyFullProfileGet companyFullProfileGet;
    @Mock
    private Data companyProfile;

    @Test
    @DisplayName("Should fetch company profile successfully with no exceptions")
    void fetchCompanyProfile() throws ApiErrorResponseException, URIValidationException {
        // given
        when(clientSupplier.get()).thenReturn(apiClient);
        when(apiClient.privateCompanyResourceHandler()).thenReturn(resourceHandler);
        when(resourceHandler.getCompanyFullProfile(any())).thenReturn(companyFullProfileGet);
        when(companyFullProfileGet.execute()).thenReturn(new ApiResponse<>(200, Collections.emptyMap(), companyProfile));

        // when
        Optional<Data> actual = client.fetchCompanyProfile(COMPANY_NUMBER, CONTEXT_ID);

        // then
        assertTrue(actual.isPresent());
        assertEquals(companyProfile, actual.get());
        verify(resourceHandler).getCompanyFullProfile(String.format("/company/%s", COMPANY_NUMBER));
    }

    @Test
    @DisplayName("Should fetch company profile with empty data and no exceptions")
    void fetchCompanyProfileEmptyData() throws ApiErrorResponseException, URIValidationException {
        // given
        when(clientSupplier.get()).thenReturn(apiClient);
        when(apiClient.privateCompanyResourceHandler()).thenReturn(resourceHandler);
        when(resourceHandler.getCompanyFullProfile(any())).thenReturn(companyFullProfileGet);
        when(companyFullProfileGet.execute()).thenReturn(new ApiResponse<>(200, Collections.emptyMap()));

        // when
        Optional<Data> actual = client.fetchCompanyProfile(COMPANY_NUMBER, CONTEXT_ID);

        // then
        assertTrue(actual.isEmpty());
        verify(resourceHandler).getCompanyFullProfile(String.format("/company/%s", COMPANY_NUMBER));
    }

    @Test
    @DisplayName("Should delegate to response handler when ApiErrorResponseException caught")
    void patchCompanyNameAndStatusApiErrorResponseException() throws ApiErrorResponseException, URIValidationException {
        // given
        HttpResponseException.Builder builder = new HttpResponseException.Builder(503, "service unavailable", new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException = new ApiErrorResponseException(builder);

        when(clientSupplier.get()).thenReturn(apiClient);
        when(apiClient.privateCompanyResourceHandler()).thenReturn(resourceHandler);
        when(resourceHandler.getCompanyFullProfile(any())).thenReturn(companyFullProfileGet);
        when(companyFullProfileGet.execute()).thenThrow(apiErrorResponseException);

        // when
        client.fetchCompanyProfile(COMPANY_NUMBER, CONTEXT_ID);

        // then
        verify(resourceHandler).getCompanyFullProfile(String.format("/company/%s", COMPANY_NUMBER));
        verify(responseHandler).handle(String.format(
                "HTTP response code 503 when fetching profile for company %s with context id %s", COMPANY_NUMBER, CONTEXT_ID),
                apiErrorResponseException);
    }

    @Test
    @DisplayName("Should delegate to response handler when IllegalArgumentException caught")
    void patchCompanyNameAndStatusIllegalArgumentException() throws ApiErrorResponseException, URIValidationException {
        // given
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException();
        when(clientSupplier.get()).thenReturn(apiClient);
        when(apiClient.privateCompanyResourceHandler()).thenReturn(resourceHandler);
        when(resourceHandler.getCompanyFullProfile(any())).thenReturn(companyFullProfileGet);
        when(companyFullProfileGet.execute()).thenThrow(illegalArgumentException);

        // when
        client.fetchCompanyProfile(COMPANY_NUMBER, CONTEXT_ID);

        // then
        verify(resourceHandler).getCompanyFullProfile(String.format("/company/%s", COMPANY_NUMBER));
        verify(responseHandler).handle(String.format(
                "Failed fetching profile for company %s with context id %s", COMPANY_NUMBER, CONTEXT_ID),
                illegalArgumentException);
    }

    @Test
    @DisplayName("Should delegate to response handler when URIValidationException caught")
    void patchCompanyNameAndStatusURIValidationException() throws ApiErrorResponseException, URIValidationException {
        // given
        URIValidationException uriValidationException = new URIValidationException("Invalid URI");
        when(clientSupplier.get()).thenReturn(apiClient);
        when(apiClient.privateCompanyResourceHandler()).thenReturn(resourceHandler);
        when(resourceHandler.getCompanyFullProfile(any())).thenReturn(companyFullProfileGet);
        when(companyFullProfileGet.execute()).thenThrow(uriValidationException);

        // when
        client.fetchCompanyProfile(COMPANY_NUMBER, CONTEXT_ID);

        // then
        verify(resourceHandler).getCompanyFullProfile(String.format("/company/%s", COMPANY_NUMBER));
        verify(responseHandler).handle(String.format(
                "Failed fetching profile for company %s with context id %s", COMPANY_NUMBER, CONTEXT_ID),
                uriValidationException);
    }
}