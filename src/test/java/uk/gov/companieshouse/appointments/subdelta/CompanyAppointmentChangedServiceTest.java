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
class CompanyAppointmentChangedServiceTest {

    private static final String CHANGED_APPOINTMENT_RESOURCE_URI = "/company/12345678/appointments/abc123";
    private static final String CONTEXT_ID = "context id";
    private static final String COMPANY_NAME = "COMPANY LIMITED";
    private static final String COMPANY_STATUS = "active";
    private static final String COMPANY_NUMBER = "company number";

    @Mock
    private CompanyNumberExtractor companyNumberExtractor;
    @Mock
    private CompanyProfileClient companyProfileClient;
    @Mock
    private AppointmentsClient appointmentsClient;
    @InjectMocks
    private CompanyAppointmentChangedService service;

    @Test
    @DisplayName("Should process changed company appointment successfully with no exceptions")
    void processChangedCompanyAppointment() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setResourceUri(CHANGED_APPOINTMENT_RESOURCE_URI);
        changedData.setContextId(CONTEXT_ID);

        Data companyProfile = new Data()
                .companyName(COMPANY_NAME)
                .companyStatus(COMPANY_STATUS);

        when(companyNumberExtractor.extractFromUri(any())).thenReturn(COMPANY_NUMBER);
        when(companyProfileClient.fetchCompanyProfile(any(), any())).thenReturn(Optional.of(companyProfile));

        // when
        service.processMessage(changedData);

        // then
        verify(companyNumberExtractor).extractFromUri(CHANGED_APPOINTMENT_RESOURCE_URI);
        verify(companyProfileClient).fetchCompanyProfile(COMPANY_NUMBER, CONTEXT_ID);
        verify(appointmentsClient).patchCompanyNameAndStatus(CHANGED_APPOINTMENT_RESOURCE_URI, COMPANY_NAME, COMPANY_STATUS, CONTEXT_ID);
    }



    @Test
    @DisplayName("Should not call api clients when company number extractor throws non retryable exception")
    void processChangedCompanyAppointmentBadURI() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setResourceUri(CHANGED_APPOINTMENT_RESOURCE_URI);

        when(companyNumberExtractor.extractFromUri(any())).thenThrow(NonRetryableException.class);

        // when
        Executable executable = () -> service.processMessage(changedData);

        // then
        assertThrows(NonRetryableException.class, executable);
        verify(companyNumberExtractor).extractFromUri(CHANGED_APPOINTMENT_RESOURCE_URI);
        verifyNoInteractions(companyProfileClient);
        verifyNoInteractions(appointmentsClient);
    }

    @Test
    @DisplayName("Should throw non retryable exception when company profile is empty")
    void processChangedCompanyAppointmentEmptyCompanyProfile() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setResourceUri(CHANGED_APPOINTMENT_RESOURCE_URI);
        changedData.setContextId(CONTEXT_ID);

        when(companyNumberExtractor.extractFromUri(any())).thenReturn(COMPANY_NUMBER);
        when(companyProfileClient.fetchCompanyProfile(any(), any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> service.processMessage(changedData);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, executable);
        assertEquals(String.format("Company profile not found for %s", COMPANY_NUMBER), exception.getMessage());
        verify(companyNumberExtractor).extractFromUri(CHANGED_APPOINTMENT_RESOURCE_URI);
        verify(companyProfileClient).fetchCompanyProfile(COMPANY_NUMBER, CONTEXT_ID);
        verifyNoInteractions(appointmentsClient);
    }

    @Test
    @DisplayName("Should not call appointments client when company profile client throws non retryable exception")
    void processChangedCompanyAppointmentCompanyProfileError() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setResourceUri(CHANGED_APPOINTMENT_RESOURCE_URI);
        changedData.setContextId(CONTEXT_ID);

        when(companyNumberExtractor.extractFromUri(any())).thenReturn(COMPANY_NUMBER);
        when(companyProfileClient.fetchCompanyProfile(any(), any())).thenThrow(NonRetryableException.class);

        // when
        Executable executable = () -> service.processMessage(changedData);

        // then
        assertThrows(NonRetryableException.class, executable);
        verify(companyNumberExtractor).extractFromUri(CHANGED_APPOINTMENT_RESOURCE_URI);
        verify(companyProfileClient).fetchCompanyProfile(COMPANY_NUMBER, CONTEXT_ID);
        verifyNoInteractions(appointmentsClient);
    }
}