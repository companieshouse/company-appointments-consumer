package uk.gov.companieshouse.appointments.subdelta;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.stream.ResourceChangedData;

@ExtendWith(MockitoExtension.class)
class CompanyProfileChangedServiceTest {

    private static final String CHANGED_COMPANY_PROFILE_PATCH_URI = "/company/12345678/appointments";
    private static final String CHANGED_COMPANY_PROFILE_RESOURCE_URI = "/company/12345678";
    private static final String DESERIALISE_FAILED_MESSAGE = String.format("Failed to deserialise company profile data: [%s]", CHANGED_COMPANY_PROFILE_RESOURCE_URI);
    private static final String CONTEXT_ID = "context id";
    private static final String COMPANY_NAME = "COMPANY LIMITED";
    private static final String COMPANY_STATUS = "active";

    @Mock
    private AppointmentsClient appointmentsClient;
    @InjectMocks
    private CompanyProfileChangedService service;

    @Test
    @DisplayName("Should process changed company profile successfully with no exceptions")
    void processChangedCompanyProfile() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setResourceUri(CHANGED_COMPANY_PROFILE_RESOURCE_URI);
        changedData.setContextId(CONTEXT_ID);
        String companyProfileData = "{ \"company_name\": \"COMPANY LIMITED\", \"company_status\": \"active\" }";
        changedData.setData(companyProfileData);

        // when
        service.processMessage(changedData);

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
        String companyProfileData = "{ \"invalid_field\": \"COMPANY LIMITED\", \"company_status\": \"active\" }";
        changedData.setData(companyProfileData);

        // when
        Executable executable = () -> service.processMessage(changedData);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, executable);
        assertEquals(DESERIALISE_FAILED_MESSAGE, exception.getMessage());
        verifyNoInteractions(appointmentsClient);
    }
}