package uk.gov.companieshouse.appointments.subdelta;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

@ExtendWith(MockitoExtension.class)
class ServiceRouterTest {

    private static final String OFFICERS_TOPIC = "officers topic";
    private static final String COMPANY_PROFILE_TOPIC = "company profile topic";
    private static final String INVALID_TOPIC = "invalid topic";

    @Mock
    private CompanyAppointmentChangedService companyAppointmentChangedService;
    @Mock
    private CompanyProfileChangedService companyProfileChangedService;

    private ServiceRouter router;

    @BeforeEach
    void setUp() {
        router = new ServiceRouter(companyAppointmentChangedService, companyProfileChangedService,
                OFFICERS_TOPIC, COMPANY_PROFILE_TOPIC);
    }

    @Test
    @DisplayName("Should call company appointment changed service when officers topic name")
    void routeChangedAppointment() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setEvent(new EventRecord("", "changed", emptyList()));
        RouterParameters parameters = new RouterParameters(changedData, OFFICERS_TOPIC);

        // when
        router.route(parameters);

        // then
        verify(companyAppointmentChangedService).processMessage(changedData);
        verifyNoInteractions(companyProfileChangedService);
    }

    @Test
    @DisplayName("Should call company profile changed service when company profile topic name")
    void routeChangedCompanyProfile() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setEvent(new EventRecord("", "changed", emptyList()));
        RouterParameters parameters = new RouterParameters(changedData, COMPANY_PROFILE_TOPIC);

        // when
        router.route(parameters);

        // then
        verify(companyProfileChangedService).processMessage(changedData);
        verifyNoInteractions(companyAppointmentChangedService);
    }

    @Test
    @DisplayName("Should throw non retryable exception when topic name matchers neither topic")
    void routeInvalidTopicName() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setEvent(new EventRecord("", "changed", emptyList()));
        RouterParameters parameters = new RouterParameters(changedData, INVALID_TOPIC);

        // when
        Executable executable = () -> router.route(parameters);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, executable);
        assertEquals(String.format("Invalid topic name: [%s]", INVALID_TOPIC), exception.getMessage());
        verifyNoInteractions(companyProfileChangedService);
        verifyNoInteractions(companyAppointmentChangedService);
    }

    @Test
    @DisplayName("Should not call either service when event type is deleted")
    void routeDeletedMessage() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setEvent(new EventRecord("", "deleted", emptyList()));
        RouterParameters parameters = new RouterParameters(changedData, OFFICERS_TOPIC);

        // when
        router.route(parameters);

        // then
        verifyNoInteractions(companyAppointmentChangedService);
        verifyNoInteractions(companyProfileChangedService);
    }
}