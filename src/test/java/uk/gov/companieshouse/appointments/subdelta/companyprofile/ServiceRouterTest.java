package uk.gov.companieshouse.appointments.subdelta.companyprofile;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

@ExtendWith(MockitoExtension.class)
class ServiceRouterTest {

    @Mock
    private CompanyProfileChangedService companyProfileChangedService;

    private ServiceRouter router;

    @BeforeEach
    void setUp() {
        router = new ServiceRouter( companyProfileChangedService);
    }

    @Test
    @DisplayName("Should call company profile changed service when event type changed")
    void routeChangedCompanyProfile() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setEvent(new EventRecord("", "changed", emptyList()));

        // when
        router.route(changedData);

        // then
        verify(companyProfileChangedService).processMessage(changedData);
    }

    @Test
    @DisplayName("Should not call profile service when event type is deleted")
    void routeDeletedMessage() {
        // given
        ResourceChangedData changedData = new ResourceChangedData();
        changedData.setEvent(new EventRecord("", "deleted", emptyList()));

        // when
        router.route(changedData);

        // then
        verifyNoInteractions(companyProfileChangedService);
    }
}