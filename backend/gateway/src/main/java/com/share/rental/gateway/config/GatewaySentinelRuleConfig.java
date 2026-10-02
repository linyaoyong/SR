package com.share.rental.gateway.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.share.rental.gateway.filter.GatewaySentinelFilter;
import jakarta.annotation.PostConstruct;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GatewaySentinelRuleConfig {

    private final Environment environment;

    public GatewaySentinelRuleConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void init() {
        loadRules();
    }

    @EventListener(EnvironmentChangeEvent.class)
    void reload(EnvironmentChangeEvent event) {
        if (event.getKeys().stream().anyMatch(this::isSentinelKey)) {
            loadRules();
        }
    }

    void loadRules() {
        FlowRuleManager.loadRules(List.of(
                qpsRule(GatewaySentinelFilter.ITEM_LIST_RESOURCE,
                        readQps("sentinel.item-list.qps", 20.0)),
                qpsRule(GatewaySentinelFilter.APPLICATION_CREATE_RESOURCE,
                        readQps("rental.application.qps", 5.0))
        ));
    }

    private boolean isSentinelKey(String key) {
        return "sentinel.item-list.qps".equals(key) || "rental.application.qps".equals(key);
    }

    private double readQps(String key, double defaultValue) {
        Double value = environment.getProperty(key, Double.class);
        return value == null ? defaultValue : value;
    }

    private FlowRule qpsRule(String resource, double qps) {
        FlowRule rule = new FlowRule(resource);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(qps);
        return rule;
    }
}
