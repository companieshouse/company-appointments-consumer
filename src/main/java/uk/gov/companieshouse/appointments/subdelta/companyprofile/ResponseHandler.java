package uk.gov.companieshouse.appointments.subdelta.companyprofile;

import static uk.gov.companieshouse.appointments.subdelta.Application.NAMESPACE;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.appointments.subdelta.exception.NonRetryableException;
import uk.gov.companieshouse.appointments.subdelta.exception.RetryableException;
import uk.gov.companieshouse.appointments.subdelta.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class ResponseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    public void handle(String message, URIValidationException ex) {
        LOGGER.error(message, DataMapHolder.getLogMap());
        throw new NonRetryableException(message, ex);
    }

    public void handle(String message, IllegalArgumentException ex) {
        String causeMessage = ex.getCause() != null
                ? String.format("; %s", ex.getCause().getMessage()) : "";
        LOGGER.info(message + causeMessage, DataMapHolder.getLogMap());
        throw new RetryableException(message, ex);
    }

    public void handle(String message, ApiErrorResponseException ex) {
        if (HttpStatus.valueOf(ex.getStatusCode()).is5xxServerError()) {
            LOGGER.info(message, DataMapHolder.getLogMap());
            throw new RetryableException(message, ex);
        } else if (HttpStatus.valueOf(ex.getStatusCode()) == HttpStatus.NOT_FOUND) {
            LOGGER.info("HTTP response code 404 returned, appointments not yet inserted for company in context.",
                    DataMapHolder.getLogMap());
        } else {
            LOGGER.error(message, DataMapHolder.getLogMap());
            throw new NonRetryableException(message, ex);
        }
    }
}
