package com.share.rental.rental.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.share.rental.rental.service.ResilientRemoteCallService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RentalSentinelRuleConfigTest {

    @AfterEach
    void clearRules() {
        DegradeRuleManager.loadRules(List.of());
    }

    @Test
    void loadsItemAndWalletSlowCallRulesFromEnvironment() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("resilience.item.slow-call-rt-ms", "800")
                .withProperty("resilience.item.slow-ratio-threshold", "0.6")
                .withProperty("resilience.item.min-request-amount", "4")
                .withProperty("resilience.item.stat-interval-ms", "30000")
                .withProperty("resilience.item.time-window-seconds", "8")
                .withProperty("resilience.wallet.slow-call-rt-ms", "700")
                .withProperty("resilience.wallet.slow-ratio-threshold", "0.7")
                .withProperty("resilience.wallet.min-request-amount", "3")
                .withProperty("resilience.wallet.stat-interval-ms", "20000")
                .withProperty("resilience.wallet.time-window-seconds", "6");

        new RentalSentinelRuleConfig(environment).loadRules();

        DegradeRule itemRule = onlyRule(ResilientRemoteCallService.ITEM_RESOURCE);
        assertThat(itemRule.getGrade()).isEqualTo(RuleConstant.DEGRADE_GRADE_RT);
        assertThat(itemRule.getCount()).isEqualTo(800.0);
        assertThat(itemRule.getSlowRatioThreshold()).isEqualTo(0.6);
        assertThat(itemRule.getMinRequestAmount()).isEqualTo(4);
        assertThat(itemRule.getStatIntervalMs()).isEqualTo(30000);
        assertThat(itemRule.getTimeWindow()).isEqualTo(8);

        DegradeRule walletRule = onlyRule(ResilientRemoteCallService.WALLET_RESOURCE);
        assertThat(walletRule.getCount()).isEqualTo(700.0);
        assertThat(walletRule.getSlowRatioThreshold()).isEqualTo(0.7);
        assertThat(walletRule.getMinRequestAmount()).isEqualTo(3);
        assertThat(walletRule.getStatIntervalMs()).isEqualTo(20000);
        assertThat(walletRule.getTimeWindow()).isEqualTo(6);
    }

    @Test
    void reloadsOnlyWhenResilienceKeysChange() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("resilience.wallet.slow-call-rt-ms", "1000");
        RentalSentinelRuleConfig config = new RentalSentinelRuleConfig(environment);
        config.loadRules();

        environment.setProperty("resilience.wallet.slow-call-rt-ms", "500");
        config.reload(new EnvironmentChangeEvent(Set.of("message.websocket.online-ttl-seconds")));
        assertThat(onlyRule(ResilientRemoteCallService.WALLET_RESOURCE).getCount()).isEqualTo(1000.0);

        config.reload(new EnvironmentChangeEvent(Set.of("resilience.wallet.slow-call-rt-ms")));
        assertThat(onlyRule(ResilientRemoteCallService.WALLET_RESOURCE).getCount()).isEqualTo(500.0);
    }

    private DegradeRule onlyRule(String resource) {
        Set<DegradeRule> rules = DegradeRuleManager.getRulesOfResource(resource);
        assertThat(rules).hasSize(1);
        return rules.iterator().next();
    }
}
