package com.sbpl.OPD.Auth.utils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class AuthMetrics {

    private final Counter successfulLogins;
    private final Counter failedLogins;
    private final Timer loginTimer;
    private final MeterRegistry meterRegistry;

    public AuthMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.successfulLogins = Counter.builder("auth.login.success")
                .description("Number of successful logins")
                .register(meterRegistry);

        this.failedLogins = Counter.builder("auth.login.failure")
                .description("Number of failed logins")
                .register(meterRegistry);

        this.loginTimer = Timer.builder("auth.login.duration")
                .description("Time taken to process login requests")
                .register(meterRegistry);
    }

    public void incrementSuccessfulLogin() {
        successfulLogins.increment();
    }

    public void incrementFailedLogin() {
        failedLogins.increment();
    }

    public Timer.Sample startLoginTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordLoginTime(Timer.Sample timerSample) {
        if (timerSample != null) {
            timerSample.stop(loginTimer);
        }
    }
}
