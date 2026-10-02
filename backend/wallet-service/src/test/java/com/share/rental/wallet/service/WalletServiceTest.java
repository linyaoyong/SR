package com.share.rental.wallet.service;

import com.share.rental.wallet.dto.WalletMeResponse;
import com.share.rental.wallet.dto.WalletUsableResponse;
import com.share.rental.wallet.entity.WalletAccount;
import com.share.rental.wallet.entity.WalletTransaction;
import com.share.rental.wallet.mapper.DepositFreezeMapper;
import com.share.rental.wallet.mapper.OrderSettlementMapper;
import com.share.rental.wallet.mapper.WalletAccountMapper;
import com.share.rental.wallet.mapper.WalletTransactionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletAccountMapper accountMapper;
    @Mock
    private WalletTransactionMapper transactionMapper;
    @Mock
    private DepositFreezeMapper depositFreezeMapper;
    @Mock
    private OrderSettlementMapper orderSettlementMapper;

    @InjectMocks
    private WalletService walletService;

    @Test
    void getOrCreateWallet_existing_returnsIt() {
        WalletAccount existing = new WalletAccount();
        existing.setId(1L);
        existing.setUserId(10L);
        existing.setBalance(new BigDecimal("100.00"));
        when(accountMapper.selectOne(any())).thenReturn(existing);

        WalletMeResponse resp = walletService.getMyWallet(10L);

        assertEquals(10L, resp.getUserId());
        assertEquals(new BigDecimal("100.00"), resp.getBalance());
        verify(accountMapper, never()).insert(any(WalletAccount.class));
    }

    @Test
    void getOrCreateWallet_missing_createsNew() {
        when(accountMapper.selectOne(any())).thenReturn(null);

        WalletMeResponse resp = walletService.getMyWallet(10L);

        assertEquals(10L, resp.getUserId());
        assertEquals(BigDecimal.ZERO, resp.getBalance());
        verify(accountMapper).insert(any(WalletAccount.class));
    }

    @Test
    void recharge_success_updatesBalanceAndWritesTransaction() {
        WalletAccount account = new WalletAccount();
        account.setId(1L);
        account.setUserId(10L);
        account.setBalance(new BigDecimal("50.00"));
        account.setFrozenAmount(BigDecimal.ZERO);
        when(accountMapper.selectOne(any())).thenReturn(account);

        when(accountMapper.updateById(account)).thenReturn(1);

        walletService.recharge(10L, new BigDecimal("100.00"), "test");

        assertEquals(new BigDecimal("150.00"), account.getBalance());
        verify(accountMapper).updateById(account);
        verify(transactionMapper).insert(any(WalletTransaction.class));
    }

    @Test
    void recharge_updateConflictDoesNotWriteTransaction() {
        WalletAccount account = new WalletAccount();
        account.setId(1L);
        account.setUserId(10L);
        account.setBalance(new BigDecimal("50"));
        account.setFrozenAmount(BigDecimal.ZERO);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(accountMapper.updateById(account)).thenReturn(0);
        com.share.rental.common.exception.BusinessException ex = assertThrows(
                com.share.rental.common.exception.BusinessException.class,
                () -> walletService.recharge(10L, new BigDecimal("100"), "test"));
        assertEquals(40412, ex.errorCode().code());
        verifyNoInteractions(transactionMapper, depositFreezeMapper, orderSettlementMapper);
    }

    @Test
    void isUsable_balancePositive_returnsTrue() {
        WalletAccount account = new WalletAccount();
        account.setBalance(new BigDecimal("0.01"));
        when(accountMapper.selectOne(any())).thenReturn(account);

        assertTrue(walletService.isUsable(10L));
    }

    @Test
    void isUsable_balanceZero_returnsFalse() {
        WalletAccount account = new WalletAccount();
        account.setBalance(new BigDecimal("0.00"));
        when(accountMapper.selectOne(any())).thenReturn(account);

        assertFalse(walletService.isUsable(10L));
    }

    @Test
    void isUsable_balanceNegative_returnsFalse() {
        WalletAccount account = new WalletAccount();
        account.setBalance(new BigDecimal("-10.00"));
        when(accountMapper.selectOne(any())).thenReturn(account);

        assertFalse(walletService.isUsable(10L));
    }

    @Test
    void isUsable_walletMissing_returnsFalse() {
        when(accountMapper.selectOne(any())).thenReturn(null);

        assertFalse(walletService.isUsable(99L));
    }

    @Test
    void getUsable_positiveBalance_returnsTrueWithActualBalance() {
        WalletAccount account = new WalletAccount();
        account.setUserId(10L);
        account.setBalance(new BigDecimal("88.50"));
        when(accountMapper.selectOne(any())).thenReturn(account);

        WalletUsableResponse response = walletService.getUsable(10L);

        assertEquals(10L, response.getUserId());
        assertTrue(response.getUsable());
        assertEquals(new BigDecimal("88.50"), response.getBalance());
    }

    @Test
    void getUsable_withDemoDelay_waitsBeforeReturning() {
        ReflectionTestUtils.setField(walletService, "walletDelayMs", 5L);
        WalletAccount account = new WalletAccount();
        account.setUserId(10L);
        account.setBalance(new BigDecimal("88.50"));
        when(accountMapper.selectOne(any())).thenReturn(account);

        long start = System.nanoTime();
        walletService.getUsable(10L);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertTrue(elapsedMillis >= 5L);
    }

    @Test
    void getUsable_missingWallet_returnsFalseWithNullBalance() {
        when(accountMapper.selectOne(any())).thenReturn(null);

        WalletUsableResponse response = walletService.getUsable(99L);

        assertEquals(99L, response.getUserId());
        assertFalse(response.getUsable());
        assertNull(response.getBalance());
    }
}
