package uk.gov.companieshouse.appointments.subdelta.companyprofile;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.appointments.subdelta.exception.NonRetryableException;
import uk.gov.companieshouse.stream.ResourceChangedData;

@ExtendWith(MockitoExtension.class)
class CompanyProfileChangedServiceTest {

    private static final String CHANGED_COMPANY_PROFILE_PATCH_URI = "/company/12345678/appointments";
    private static final String CHANGED_COMPANY_PROFILE_RESOURCE_URI = "/company/12345678";
    private static final String DESERIALISE_FAILED_MESSAGE = "Failed to deserialise company profile data";
    private static final String CONTEXT_ID = "context id";
    private static final String COMPANY_NAME = "COMPANY LIMITED";
    private static final String COMPANY_STATUS = "active";

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private AppointmentsClient appointmentsClient;
    @InjectMocks
    private CompanyProfileChangedService service;

    @Test
    @DisplayName("Should process changed company profile successfully with no exceptions")
    void processChangedCompanyProfile() throws IOException {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setResourceUri(CHANGED_COMPANY_PROFILE_RESOURCE_URI);
        changedData.setContextId(CONTEXT_ID);
        String companyProfileData = "{ \"company_name\": \"COMPANY LIMITED\", \"company_status\": \"active\" }";
        changedData.setData(companyProfileData);

        Data data = new Data()
                .companyName("COMPANY LIMITED")
                .companyStatus("active");
        when(objectMapper.readValue(anyString(), eq(Data.class))).thenReturn(data);

        // when
        service.processMessage(changedData);

        // then
        verify(objectMapper).readValue(companyProfileData, Data.class);
        verify(appointmentsClient).patchCompanyNameAndStatusForAllAppointments(CHANGED_COMPANY_PROFILE_PATCH_URI, COMPANY_NAME, COMPANY_STATUS);
    }

    @Test
    @DisplayName("Should throw NonRetryableException when JsonProcessingException is caught")
    void processChangedCompanyProfileThrowsNonRetryableException() throws JsonProcessingException {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setResourceUri(CHANGED_COMPANY_PROFILE_RESOURCE_URI);
        changedData.setContextId(CONTEXT_ID);
        String companyProfileData = "{ \"invalid_field\": \"COMPANY LIMITED\", \"company_status\": \"active\" }";
        changedData.setData(companyProfileData);
        when(objectMapper.readValue(anyString(), eq(Data.class))).thenThrow(JsonProcessingException.class);

        // when
        Executable executable = () -> service.processMessage(changedData);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, executable);
        assertEquals(DESERIALISE_FAILED_MESSAGE, exception.getMessage());
        verifyNoInteractions(appointmentsClient);
    }

}