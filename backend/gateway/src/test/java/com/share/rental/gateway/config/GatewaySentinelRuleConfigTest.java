package com.share.rental.gateway.config;

import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.share.rental.gateway.filter.GatewaySentinelFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class GatewaySentinelRuleConfigTest {

    @AfterEach
    void clearRules() {
        FlowRuleManager.loadRules(List.of());
    }

    @Test
    void loadRulesUsesConfiguredQpsValues() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("sentinel.item-list.qps", "3")
                .withProperty("rental.application.qps", "2");

        new GatewaySentinelRuleConfig(environment).loadRules();

        Map<String, FlowRule> rules = rulesByResource();
        assertThat(rules.get(GatewaySentinelFilter.ITEM_LIST_RESOURCE).getCount()).isEqualTo(3.0);
        assertThat(rules.get(GatewaySentinelFilter.APPLICATION_CREATE_RESOURCE).getCount()).isEqualTo(2.0);
    }

    @Test
    void reloadOnlyRespondsToGatewaySentinelKeys() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("sentinel.item-list.qps", "1")
                .withProperty("rental.application.qps", "1");
        GatewaySentinelRuleConfig config = new GatewaySentinelRuleConfig(environment);
        config.loadRules();

        environment.setProperty("sentinel.item-list.qps", "4");
        config.reload(new EnvironmentChangeEvent(Set.of("sentinel.item-list.qps")));

        assertThat(rulesByResource().get(GatewaySentinelFilter.ITEM_LIST_RESOURCE).getCount()).isEqualTo(4.0);
    }

    private Map<String, FlowRule> rulesByResource() {
        return FlowRuleManager.getRules().stream()
                .collect(Collectors.toMap(FlowRule::getResource, Function.identity()));
    }
}
