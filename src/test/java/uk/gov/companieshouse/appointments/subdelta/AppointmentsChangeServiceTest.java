package uk.gov.companieshouse.appointments.subdelta;

import static java.util.Collections.emptyList;
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
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

@ExtendWith(MockitoExtension.class)
class AppointmentsChangeServiceTest {

    private static final String CHANGED_APPOINTMENT_RESOURCE_URI = "/company/12345678/appointments/abc123";
    private static final String CHANGED_COMPANY_PROFILE_RESOURCE_URI = "/company/12345678";
    private static final String CHANGED_COMPANY_PROFILE_PATCH_URI = "/company/12345678/appointments";
    private static final String DESERIALISE_FAILED_MESSAGE = String.format("Failed to deserialise company profile data: [%s]", CHANGED_COMPANY_PROFILE_RESOURCE_URI);
    private static final String CONTEXT_ID = "context id";
    private static final String COMPANY_NAME = "COMPANY LIMITED";
    private static final String COMPANY_STATUS = "active";
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
    @DisplayName("Should process changed company appointment successfully with no exceptions")
    void processChangedCompanyAppointment() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setResourceUri(CHANGED_APPOINTMENT_RESOURCE_URI);
        changedData.setContextId(CONTEXT_ID);
        changedData.setEvent(new EventRecord("", "changed", emptyList()));
        ServiceParameters serviceParameters = new ServiceParameters(changedData);

        Data companyProfile = new Data()
                .companyName(COMPANY_NAME)
                .companyStatus(COMPANY_STATUS);

        when(companyNumberExtractor.extractFromUri(any())).thenReturn(COMPANY_NUMBER);
        when(companyProfileClient.fetchCompanyProfile(any(), any())).thenReturn(Optional.of(companyProfile));

        // when
        service.processChangedCompanyAppointment(serviceParameters);

        // then
        verify(companyNumberExtractor).extractFromUri(CHANGED_APPOINTMENT_RESOURCE_URI);
        verify(companyProfileClient).fetchCompanyProfile(COMPANY_NUMBER, CONTEXT_ID);
        verify(appointmentsClient).patchCompanyNameAndStatus(CHANGED_APPOINTMENT_RESOURCE_URI, COMPANY_NAME, COMPANY_STATUS, CONTEXT_ID);
    }

    @Test
    @DisplayName("Should not process a changed company appointment when event type is deleted")
    void processChangedCompanyAppointmentDeleted() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setEvent(new EventRecord("", "deleted", emptyList()));
        ServiceParameters serviceParameters = new ServiceParameters(changedData);

        // when
        service.processChangedCompanyAppointment(serviceParameters);

        // then
        verifyNoInteractions(companyNumberExtractor);
        verifyNoInteractions(companyProfileClient);
        verifyNoInteractions(appointmentsClient);
    }

    @Test
    @DisplayName("Should not call api clients when company number extractor throws non retryable exception")
    void processChangedCompanyAppointmentBadURI() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setResourceUri(CHANGED_APPOINTMENT_RESOURCE_URI);
        changedData.setEvent(new EventRecord("", "changed", emptyList()));
        ServiceParameters serviceParameters = new ServiceParameters(changedData);

        when(companyNumberExtractor.extractFromUri(any())).thenThrow(NonRetryableException.class);

        // when
        Executable executable = () -> service.processChangedCompanyAppointment(serviceParameters);

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
        changedData.setEvent(new EventRecord("", "changed", emptyList()));
        ServiceParameters serviceParameters = new ServiceParameters(changedData);

        when(companyNumberExtractor.extractFromUri(any())).thenReturn(COMPANY_NUMBER);
        when(companyProfileClient.fetchCompanyProfile(any(), any())).thenReturn(Optional.empty());

        // when
        Executable executable = () -> service.processChangedCompanyAppointment(serviceParameters);

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
        changedData.setEvent(new EventRecord("", "changed", emptyList()));
        ServiceParameters serviceParameters = new ServiceParameters(changedData);

        when(companyNumberExtractor.extractFromUri(any())).thenReturn(COMPANY_NUMBER);
        when(companyProfileClient.fetchCompanyProfile(any(), any())).thenThrow(NonRetryableException.class);

        // when
        Executable executable = () -> service.processChangedCompanyAppointment(serviceParameters);

        // then
        assertThrows(NonRetryableException.class, executable);
        verify(companyNumberExtractor).extractFromUri(CHANGED_APPOINTMENT_RESOURCE_URI);
        verify(companyProfileClient).fetchCompanyProfile(COMPANY_NUMBER, CONTEXT_ID);
        verifyNoInteractions(appointmentsClient);
    }

    @Test
    @DisplayName("Should process changed company profile successfully with no exceptions")
    void processChangedCompanyProfile() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setResourceUri(CHANGED_COMPANY_PROFILE_RESOURCE_URI);
        changedData.setContextId(CONTEXT_ID);
        changedData.setEvent(new EventRecord("", "changed", emptyList()));
        String companyProfileData = "{ \"company_name\": \"COMPANY LIMITED\", \"company_status\": \"active\" }";
        changedData.setData(companyProfileData);
        ServiceParameters serviceParameters = new ServiceParameters(changedData);

        // when
        service.processChangedCompanyProfile(serviceParameters);

        // then
        verify(appointmentsClient).patchCompanyNameAndStatus(CHANGED_COMPANY_PROFILE_PATCH_URI, COMPANY_NAME, COMPANY_STATUS, CONTEXT_ID);
    }

    @Test
    @DisplayName("Should throw NonRetryableException when JsonProcessingException is caught")
    void processChangedCompanyProfileThrowsNonRetryableException() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setResourceUri(CHANGED_COMPANY_PROFILE_RESOURCE_URI);
        changedData.setContextId(CONTEXT_ID);
        changedData.setEvent(new EventRecord("", "changed", emptyList()));
        String companyProfileData = "{ \"invalid_field\": \"COMPANY LIMITED\", \"company_status\": \"active\" }";
        changedData.setData(companyProfileData);
        ServiceParameters serviceParameters = new ServiceParameters(changedData);

        // when
        Executable executable = () -> service.processChangedCompanyProfile(serviceParameters);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, executable);
        assertEquals(DESERIALISE_FAILED_MESSAGE, exception.getMessage());
        verifyNoInteractions(appointmentsClient);
    }

    @Test
    @DisplayName("Should not process a changed company profile when event type is deleted")
    void processChangedCompanyProfileDeleted() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setEvent(new EventRecord("", "deleted", emptyList()));
        ServiceParameters serviceParameters = new ServiceParameters(changedData);

        // when
        service.processChangedCompanyProfile(serviceParameters);

        // then
        verifyNoInteractions(appointmentsClient);
    }
}