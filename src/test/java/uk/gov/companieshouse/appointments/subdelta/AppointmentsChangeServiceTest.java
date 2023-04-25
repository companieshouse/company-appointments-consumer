package uk.gov.companieshouse.appointments.subdelta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.stream.ResourceChangedData;

@ExtendWith(MockitoExtension.class)
class AppointmentsChangeServiceTest {

    private static final String RESOURCE_URI = "resource URI";
    private static final String CONTEXT_ID = "context id";
    private static final String COMPANY_NAME = "company name";
    private static final String COMPANY_STATUS = "company status";
    private static final String COMPANY_NUMBER = "company number";

    @Mock
    private CompanyProfileClient companyProfileClient;
    @Mock
    private AppointmentsClient appointmentsClient;
    @Mock
    private CompanyNumberExtractor companyNumberExtractor;
    @InjectMocks
    private AppointmentsChangeService service;

    @Test
    @DisplayName("Should process message successfully with no exceptions")
    void processMessage() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setResourceUri(RESOURCE_URI);
        changedData.setContextId(CONTEXT_ID);
        ServiceParameters serviceParameters = new ServiceParameters(changedData);

        Data companyProfile = new Data()
                .companyName(COMPANY_NAME)
                .companyStatus(COMPANY_STATUS);

        when(companyNumberExtractor.extractFromUri(any())).thenReturn(COMPANY_NUMBER);
        when(companyProfileClient.fetchCompanyProfile(any(), any())).thenReturn(Optional.of(companyProfile));

        // when
        service.processMessage(serviceParameters);

        // then
        verify(companyNumberExtractor).extractFromUri(RESOURCE_URI);
        verify(companyProfileClient).fetchCompanyProfile(COMPANY_NUMBER, CONTEXT_ID);
        verify(appointmentsClient).patchCompanyNameAndStatus(RESOURCE_URI, COMPANY_NAME, COMPANY_STATUS, CONTEXT_ID);
    }

    @Test
    @DisplayName("Should not call api clients when company number extractor throws non retryable exception")
    void processMessageBadURI() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setResourceUri(RESOURCE_URI);
        ServiceParameters serviceParameters = new ServiceParameters(changedData);

        when(companyNumberExtractor.extractFromUri(any())).thenThrow(NonRetryableException.class);

        // when
        Executable executable = () -> service.processMessage(serviceParameters);

        // then
        assertThrows(NonRetryableException.class, executable);
        verify(companyNumberExtractor).extractFromUri(RESOURCE_URI);
        verifyNoInteractions(companyProfileClient);
        verifyNoInteractions(appointmentsClient);
    }

    @Test
    @DisplayName("Should throw non retryable exception when company profile is empty")
    void processMessageEmptyCompanyProfile() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setResourceUri(RESOURCE_URI);
        changedData.setContextId(CONTEXT_ID);
        ServiceParameters serviceParameters = new ServiceParameters(changedData);

        when(companyNumberExtractor.extractFromUri(any())).thenReturn(COMPANY_NUMBER);
        when(companyProfileClient.fetchCompanyProfile(any(), any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> service.processMessage(serviceParameters);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, executable);
        assertEquals(String.format("Company profile not found for %s", COMPANY_NUMBER), exception.getMessage());
        verify(companyNumberExtractor).extractFromUri(RESOURCE_URI);
        verify(companyProfileClient).fetchCompanyProfile(COMPANY_NUMBER, CONTEXT_ID);
        verifyNoInteractions(appointmentsClient);
    }

    @Test
    @DisplayName("Should not call appointments client when company profile client throws non retryable exception")
    void processMessageCompanyProfileError() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setResourceUri(RESOURCE_URI);
        changedData.setContextId(CONTEXT_ID);
        ServiceParameters serviceParameters = new ServiceParameters(changedData);

        when(companyNumberExtractor.extractFromUri(any())).thenReturn(COMPANY_NUMBER);
        when(companyProfileClient.fetchCompanyProfile(any(), any())).thenThrow(NonRetryableException.class);

        // when
        Executable executable = () -> service.processMessage(serviceParameters);

        // then
        assertThrows(NonRetryableException.class, executable);
        verify(companyNumberExtractor).extractFromUri(RESOURCE_URI);
        verify(companyProfileClient).fetchCompanyProfile(COMPANY_NUMBER, CONTEXT_ID);
        verifyNoInteractions(appointmentsClient);
    }
}