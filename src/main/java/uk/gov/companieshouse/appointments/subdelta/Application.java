package uk.gov.companieshouse.appointments.subdelta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@SpringBootApplication
public class Application {

    public static final Logger LOGGER = LoggerFactory.getLogger("company-appointments-consumer");

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
