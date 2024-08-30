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
        if (HttpStatus.BAD_REQUEST.value() == ex.getStatusCode() || HttpStatus.CONFLICT.value() == ex.getStatusCode()){
            LOGGER.error(message, DataMapHolder.getLogMap());
            throw new NonRetryableException(message, ex);
        }
        else {
            LOGGER.info(message, DataMapHolder.getLogMap());
            throw new RetryableException(message, ex);
        }
    }
}
