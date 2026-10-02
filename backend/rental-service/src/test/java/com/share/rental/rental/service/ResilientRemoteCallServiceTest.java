package com.share.rental.rental.service;

import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResilientRemoteCallServiceTest {

    @BeforeEach
    void resetBeforeEach() {
        clearRules();
    }

    @AfterEach
    void clearRules() {
        FlowRuleManager.loadRules(List.of());
    }

    @Test
    void callItem_successReturnsSupplierValue() {
        String value = ResilientRemoteCallService.callItem(() -> "ok");

        assertThat(value).isEqualTo("ok");
    }

    @Test
    void callItem_blockedThrowsServiceBusy() {
        FlowRule rule = new FlowRule(ResilientRemoteCallService.ITEM_RESOURCE);
        rule.setCount(0);
        rule.setGrade(1);
        FlowRuleManager.loadRules(List.of(rule));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ResilientRemoteCallService.callItem(() -> "blocked"));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.SERVICE_BUSY);
    }

    @Test
    void callWallet_runtimeExceptionThrowsServiceBusy() {
        AtomicInteger calls = new AtomicInteger();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> ResilientRemoteCallService.callWallet(() -> {
                    calls.incrementAndGet();
                    throw new IllegalStateException("wallet down");
                }));

        assertThat(calls).hasValue(1);
        assertThat(ex.errorCode()).isEqualTo(ErrorCode.SERVICE_BUSY);
    }
}
