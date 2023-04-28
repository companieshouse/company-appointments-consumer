package uk.gov.companieshouse.appointments.subdelta;

import java.util.concurrent.CountDownLatch;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ConsumerAspect {

    private final CountDownLatch latch;

    public ConsumerAspect(CountDownLatch latch) {
        this.latch = latch;
    }

    @After("execution(* Consumer.consumeOfficersStream(..))")
    void afterConsume(JoinPoint joinPoint) {
        latch.countDown();
    }
}
