package com.share.rental.rental.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "rental.payment")
public class PaymentTimeoutProperties {

    private long timeoutMinutes = 30;
    private Long demoTimeoutSeconds;
    private long timeoutScanMs = 60000;

    public Duration timeoutDuration() {
        if (demoTimeoutSeconds != null && demoTimeoutSeconds > 0) {
            return Duration.ofSeconds(demoTimeoutSeconds);
        }
        return Duration.ofMinutes(timeoutMinutes);
    }

    public long timeoutMillis() {
        return timeoutDuration().toMillis();
    }

    public long getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public void setTimeoutMinutes(long timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    public Long getDemoTimeoutSeconds() {
        return demoTimeoutSeconds;
    }

    public void setDemoTimeoutSeconds(Long demoTimeoutSeconds) {
        this.demoTimeoutSeconds = demoTimeoutSeconds;
    }

    public long getTimeoutScanMs() {
        return timeoutScanMs;
    }

    public void setTimeoutScanMs(long timeoutScanMs) {
        this.timeoutScanMs = timeoutScanMs;
    }
}
