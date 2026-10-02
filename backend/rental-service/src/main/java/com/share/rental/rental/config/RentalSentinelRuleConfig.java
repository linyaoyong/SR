package com.share.rental.rental.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.share.rental.rental.service.ResilientRemoteCallService;
import jakarta.annotation.PostConstruct;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RentalSentinelRuleConfig {

    private final Environment environment;

    public RentalSentinelRuleConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void loadRules() {
        DegradeRuleManager.loadRules(List.of(
                slowCallRule(ResilientRemoteCallService.ITEM_RESOURCE, "resilience.item"),
                slowCallRule(ResilientRemoteCallService.WALLET_RESOURCE, "resilience.wallet")
        ));
    }

    @EventListener(EnvironmentChangeEvent.class)
    public void reload(EnvironmentChangeEvent event) {
        if (event.getKeys().stream().anyMatch(this::isResilienceKey)) {
            loadRules();
        }
    }

    private boolean isResilienceKey(String key) {
        return key.startsWith("resilience.item.") || key.startsWith("resilience.wallet.");
    }

    private DegradeRule slowCallRule(String resource, String prefix) {
        DegradeRule rule = new DegradeRule(resource);
        rule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        rule.setCount(readDouble(prefix + ".slow-call-rt-ms", 1000.0));
        rule.setSlowRatioThreshold(readDouble(prefix + ".slow-ratio-threshold", 0.5));
        rule.setMinRequestAmount(readInt(prefix + ".min-request-amount", 5));
        rule.setStatIntervalMs(readInt(prefix + ".stat-interval-ms", 60000));
        rule.setTimeWindow(readInt(prefix + ".time-window-seconds", 10));
        return rule;
    }

    private double readDouble(String key, double defaultValue) {
        return environment.getProperty(key, Double.class, defaultValue);
    }

    private int readInt(String key, int defaultValue) {
        return environment.getProperty(key, Integer.class, defaultValue);
    }
}
