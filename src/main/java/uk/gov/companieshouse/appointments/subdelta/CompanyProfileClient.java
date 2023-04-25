package uk.gov.companieshouse.appointments.subdelta;

import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;

@Component
public class CompanyProfileClient {

    private static final String FAILED_MSG = "Failed fetching profile for company %s with context id %s";
    private static final String ERROR_MSG = "Error %s fetching profile for company %s with context id %s";

    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final ResponseHandler responseHandler;

    public CompanyProfileClient(Supplier<InternalApiClient> internalApiClientFactory,
            ResponseHandler responseHandler) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.responseHandler = responseHandler;
    }

    public Optional<Data> fetchCompanyProfile(String companyNumber, String contextId) {
        InternalApiClient client = internalApiClientFactory.get();

        Optional<Data> profileData = Optional.empty();
        try {
            profileData = Optional.ofNullable(client.privateCompanyResourceHandler()
                    .getCompanyFullProfile(String.format("/company/%s", companyNumber))
                    .execute()
                    .getData());
        } catch (ApiErrorResponseException ex) {
            responseHandler.handle(
                    String.format(ERROR_MSG, ex.getStatusCode(), companyNumber, contextId), ex);
        } catch (IllegalArgumentException ex) {
            responseHandler.handle(String.format(FAILED_MSG, companyNumber, contextId), ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(String.format(FAILED_MSG, companyNumber, contextId), ex);
        }

        return profileData;
    }
}
