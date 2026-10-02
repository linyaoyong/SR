package com.share.rental.wallet;

import com.share.rental.wallet.entity.WalletAccount;
import com.share.rental.wallet.enums.DepositStatusEnum;
import com.share.rental.wallet.enums.SettlementStatusEnum;
import com.share.rental.wallet.enums.WalletTransactionTypeEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class WalletEntityCompileTest {

    @Test
    void enumsExposeExpectedCodes() {
        assertThat(WalletTransactionTypeEnum.RECHARGE.code()).isEqualTo(1);
        assertThat(WalletTransactionTypeEnum.DEPOSIT_DEDUCTION.code()).isEqualTo(9);
        assertThat(DepositStatusEnum.FROZEN.code()).isEqualTo(0);
        assertThat(SettlementStatusEnum.SETTLED.code()).isEqualTo(2);
    }

    @Test
    void entitiesCanInstantiate() {
        WalletAccount account = new WalletAccount();
        account.setUserId(1L);
        account.setBalance(new BigDecimal("100.00"));
        assertThat(account.getUserId()).isEqualTo(1L);
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
    }
}
