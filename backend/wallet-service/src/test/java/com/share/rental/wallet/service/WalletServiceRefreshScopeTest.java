package com.share.rental.wallet.service;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import static org.assertj.core.api.Assertions.assertThat;

class WalletServiceRefreshScopeTest {

    @Test
    void walletServiceCanRefreshDemoDelayFromNacos() {
        assertThat(WalletService.class.isAnnotationPresent(RefreshScope.class)).isTrue();
    }
}
