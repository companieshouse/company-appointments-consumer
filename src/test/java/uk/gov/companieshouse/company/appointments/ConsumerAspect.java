package uk.gov.companieshouse.company.appointments;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Aspect
@Component
public class ConsumerAspect {

    private CountDownLatch latch;

    public ConsumerAspect(CountDownLatch latch) {
        this.latch = latch;
    }

    @After("execution(* uk.gov.companieshouse.company.appointments.Consumer.consume(..))")
    void afterConsume(JoinPoint joinPoint) {
        latch.countDown();
    }

    @After("execution(* uk.gov.companieshouse.company.appointments.ErrorConsumer.consume(..))")
    void afterErrorConsume(JoinPoint joinPoint) {
        latch.countDown();
    }
}
