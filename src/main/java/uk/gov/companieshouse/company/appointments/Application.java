package uk.gov.companieshouse.company.appointments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static final String NAMESPACE = "company-appointments-consumer";

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
