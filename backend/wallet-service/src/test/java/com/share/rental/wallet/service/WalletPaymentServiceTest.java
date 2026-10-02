package com.share.rental.wallet.service;

import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.wallet.dto.PrepayRequest;
import com.share.rental.wallet.dto.WalletOperationResponse;
import com.share.rental.wallet.entity.WalletAccount;
import com.share.rental.wallet.entity.WalletTransaction;
import com.share.rental.wallet.mapper.DepositFreezeMapper;
import com.share.rental.wallet.mapper.OrderSettlementMapper;
import com.share.rental.wallet.mapper.WalletAccountMapper;
import com.share.rental.wallet.mapper.WalletTransactionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletPaymentServiceTest {

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
    void prepayRent_deductsBalanceAndWritesTransaction() {
        WalletAccount account = new WalletAccount();
        account.setId(1L);
        account.setUserId(10L);
        account.setBalance(new BigDecimal("500.00"));
        account.setFrozenAmount(BigDecimal.ZERO);
        account.setStatus(0);
        when(accountMapper.selectOne(any())).thenReturn(account);
        lenient().when(transactionMapper.selectList(any())).thenReturn(Collections.emptyList());

        PrepayRequest request = new PrepayRequest();
        request.setRenterId(10L);
        request.setOrderId(100L);
        request.setAmount(new BigDecimal("100.00"));
        request.setApplicationId(50L);
        request.setProposalId(200L);

        when(accountMapper.updateById(account)).thenReturn(1);

        WalletOperationResponse response = walletService.prepayRent(request);

        assertThat(response.getOrderId()).isEqualTo(100L);
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("400.00"));
        verify(accountMapper).updateById(account);

        ArgumentCaptor<WalletTransaction> txCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(transactionMapper).insert(txCaptor.capture());
        WalletTransaction tx = txCaptor.getValue();
        assertThat(tx.getType()).isEqualTo(2);
        assertThat(tx.getDirection()).isEqualTo(2);
        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(tx.getUserId()).isEqualTo(10L);
        assertThat(tx.getOrderId()).isEqualTo(100L);
        assertThat(tx.getApplicationId()).isEqualTo(50L);
        assertThat(tx.getProposalId()).isEqualTo(200L);
        assertThat(tx.getBalanceBefore()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(tx.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(tx.getTransactionNo()).startsWith("PR");
    }

    @Test
    void prepayRent_insufficientBalance_throwsException() {
        WalletAccount account = new WalletAccount();
        account.setId(1L);
        account.setUserId(10L);
        account.setBalance(new BigDecimal("50.00"));
        account.setFrozenAmount(BigDecimal.ZERO);
        account.setStatus(0);
        when(accountMapper.selectOne(any())).thenReturn(account);

        PrepayRequest request = new PrepayRequest();
        request.setRenterId(10L);
        request.setOrderId(100L);
        request.setAmount(new BigDecimal("100.00"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> walletService.prepayRent(request));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
        verify(accountMapper, org.mockito.Mockito.never()).updateById(any(WalletAccount.class));
        verify(transactionMapper, org.mockito.Mockito.never()).insert(any(WalletTransaction.class));
    }
}
