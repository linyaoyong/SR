package com.share.rental.wallet.service;

import com.share.rental.wallet.dto.CancelOrderPaymentRequest;
import com.share.rental.wallet.dto.CompleteSettlementRequest;
import com.share.rental.wallet.dto.SettlementResponse;
import com.share.rental.wallet.dto.WalletOperationResponse;
import com.share.rental.wallet.entity.OrderSettlement;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletSettlementServiceTest {

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
    void cancelOrderPayment_refundsPaidRentAndReleasesDeposit() {
        WalletAccount renterAccount = new WalletAccount();
        renterAccount.setId(1L);
        renterAccount.setUserId(10L);
        renterAccount.setBalance(new BigDecimal("0.00"));
        renterAccount.setFrozenAmount(new BigDecimal("50.00"));
        renterAccount.setStatus(0);
        when(accountMapper.selectOne(any())).thenReturn(renterAccount);
        lenient().when(transactionMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(depositFreezeMapper.selectList(any())).thenReturn(Collections.emptyList());

        CancelOrderPaymentRequest request = new CancelOrderPaymentRequest();
        request.setRenterId(10L);
        request.setOwnerId(20L);
        request.setOrderId(100L);
        request.setPaidRentAmount(new BigDecimal("100.00"));
        request.setFrozenDepositAmount(new BigDecimal("50.00"));

        when(accountMapper.updateById(renterAccount)).thenReturn(1);
        WalletOperationResponse response = walletService.cancelOrderPayment(request);

        assertThat(response.getOrderId()).isEqualTo(100L);
        // balance 增加 100 (退租金) + 50 (释放押金) = 150
        assertThat(renterAccount.getBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
        // frozen 减少 50
        assertThat(renterAccount.getFrozenAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
        // 两笔 transaction: CANCEL_REFUND 和 RELEASE_DEPOSIT
        verify(transactionMapper, org.mockito.Mockito.times(2)).insert(any(WalletTransaction.class));
        verify(accountMapper, org.mockito.Mockito.times(2)).updateById(renterAccount);
    }

    @Test
    void cancelOrderPayment_noPayment_refundsNothing() {
        WalletAccount renterAccount = new WalletAccount();
        renterAccount.setId(1L);
        renterAccount.setUserId(10L);
        renterAccount.setBalance(new BigDecimal("100.00"));
        renterAccount.setFrozenAmount(BigDecimal.ZERO);
        renterAccount.setStatus(0);
        when(accountMapper.selectOne(any())).thenReturn(renterAccount);
        lenient().when(transactionMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(depositFreezeMapper.selectList(any())).thenReturn(Collections.emptyList());

        CancelOrderPaymentRequest request = new CancelOrderPaymentRequest();
        request.setRenterId(10L);
        request.setOwnerId(20L);
        request.setOrderId(100L);
        request.setPaidRentAmount(BigDecimal.ZERO);
        request.setFrozenDepositAmount(BigDecimal.ZERO);

        WalletOperationResponse response = walletService.cancelOrderPayment(request);

        assertThat(response.getOrderId()).isEqualTo(100L);
        assertThat(renterAccount.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(renterAccount.getFrozenAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(transactionMapper, never()).insert(any(WalletTransaction.class));
        verify(accountMapper, never()).updateById(any(WalletAccount.class));
    }

    @Test
    void completeSettlement_transfersRentToOwnerAndReleasesDeposit() {
        WalletAccount renterAccount = new WalletAccount();
        renterAccount.setId(1L);
        renterAccount.setUserId(10L);
        renterAccount.setBalance(new BigDecimal("1000.00"));
        renterAccount.setFrozenAmount(new BigDecimal("100.00"));
        renterAccount.setStatus(0);

        WalletAccount ownerAccount = new WalletAccount();
        ownerAccount.setId(2L);
        ownerAccount.setUserId(20L);
        ownerAccount.setBalance(new BigDecimal("0.00"));
        ownerAccount.setFrozenAmount(BigDecimal.ZERO);
        ownerAccount.setStatus(0);

        when(orderSettlementMapper.selectOne(any())).thenReturn(null);
        when(accountMapper.selectOne(any())).thenReturn(renterAccount, ownerAccount);
        lenient().when(transactionMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(depositFreezeMapper.selectList(any())).thenReturn(Collections.emptyList());

        CompleteSettlementRequest request = new CompleteSettlementRequest();
        request.setRenterId(10L);
        request.setOwnerId(20L);
        request.setOrderId(100L);
        request.setRentAmount(new BigDecimal("200.00"));
        request.setDepositAmount(new BigDecimal("100.00"));
        request.setFrozenDepositAmount(new BigDecimal("100.00"));
        request.setPaidRentAmount(new BigDecimal("200.00"));
        request.setRentEndTime(LocalDateTime.now().plusDays(1));
        request.setDailyPrice(new BigDecimal("10.00"));

        when(accountMapper.updateById(any(WalletAccount.class))).thenReturn(1);

        SettlementResponse response = walletService.completeSettlement(request);

        assertThat(response.getOrderId()).isEqualTo(100L);
        assertThat(response.getRentSettled()).isTrue();
        assertThat(response.getDepositReleased()).isTrue();
        // 租金转入 owner
        assertThat(ownerAccount.getBalance()).isEqualByComparingTo(new BigDecimal("200.00"));
        // 逾期为 0
        assertThat(response.getOverdueFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        // 剩余押金释放回 renter，renter frozen 减少
        assertThat(renterAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1100.00"));
        assertThat(renterAccount.getFrozenAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getDepositDeductedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(orderSettlementMapper).insert(any(OrderSettlement.class));
    }

    @Test
    void completeSettlement_withOverdue_deductsFromDepositFirst() {
        WalletAccount renterAccount = new WalletAccount();
        renterAccount.setId(1L);
        renterAccount.setUserId(10L);
        renterAccount.setBalance(new BigDecimal("1000.00"));
        renterAccount.setFrozenAmount(new BigDecimal("100.00"));
        renterAccount.setStatus(0);

        WalletAccount ownerAccount = new WalletAccount();
        ownerAccount.setId(2L);
        ownerAccount.setUserId(20L);
        ownerAccount.setBalance(new BigDecimal("0.00"));
        ownerAccount.setFrozenAmount(BigDecimal.ZERO);
        ownerAccount.setStatus(0);

        when(orderSettlementMapper.selectOne(any())).thenReturn(null);
        when(accountMapper.selectOne(any())).thenReturn(renterAccount, ownerAccount);
        lenient().when(transactionMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(depositFreezeMapper.selectList(any())).thenReturn(Collections.emptyList());

        CompleteSettlementRequest request = new CompleteSettlementRequest();
        request.setRenterId(10L);
        request.setOwnerId(20L);
        request.setOrderId(100L);
        request.setRentAmount(new BigDecimal("200.00"));
        request.setDepositAmount(new BigDecimal("100.00"));
        request.setFrozenDepositAmount(new BigDecimal("100.00"));
        request.setPaidRentAmount(new BigDecimal("200.00"));
        // 12 小时前结束 -> overdueFee = 1.5 * 10 * (720/1440) = 7.50
        request.setRentEndTime(LocalDateTime.now().minusHours(12));
        request.setDailyPrice(new BigDecimal("10.00"));

        when(accountMapper.updateById(any(WalletAccount.class))).thenReturn(1);

        SettlementResponse response = walletService.completeSettlement(request);

        assertThat(response.getOrderId()).isEqualTo(100L);
        // overdueFee 应接近 7.50 (容忍 ±0.10 时间漂移)
        BigDecimal overdueFee = response.getOverdueFeeAmount();
        assertThat(overdueFee).isNotNull();
        assertThat(overdueFee.subtract(new BigDecimal("7.50")).abs())
                .isLessThanOrEqualTo(new BigDecimal("0.10"));
        // 押金足以覆盖，从押金扣除
        assertThat(response.getDepositDeductedAmount()).isEqualByComparingTo(overdueFee);
        // renter frozen 应该被清零 (100 - overdueFee 释放回 renter)
        assertThat(renterAccount.getFrozenAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        // owner 应收到 租金 + 逾期费
        assertThat(ownerAccount.getBalance())
                .isEqualByComparingTo(new BigDecimal("200.00").add(overdueFee));
        // renter 应得到剩余押金 (100 - overdueFee)
        assertThat(renterAccount.getBalance())
                .isEqualByComparingTo(new BigDecimal("1000.00").add(new BigDecimal("100.00").subtract(overdueFee)));
        verify(orderSettlementMapper).insert(any(OrderSettlement.class));
    }

    @Test
    void completeSettlement_withOverdue_depositNotEnough_deductsFromWallet() {
        WalletAccount renterAccount = new WalletAccount();
        renterAccount.setId(1L);
        renterAccount.setUserId(10L);
        renterAccount.setBalance(new BigDecimal("1000.00"));
        renterAccount.setFrozenAmount(new BigDecimal("5.00"));
        renterAccount.setStatus(0);

        WalletAccount ownerAccount = new WalletAccount();
        ownerAccount.setId(2L);
        ownerAccount.setUserId(20L);
        ownerAccount.setBalance(new BigDecimal("0.00"));
        ownerAccount.setFrozenAmount(BigDecimal.ZERO);
        ownerAccount.setStatus(0);

        when(orderSettlementMapper.selectOne(any())).thenReturn(null);
        when(accountMapper.selectOne(any())).thenReturn(renterAccount, ownerAccount);
        lenient().when(transactionMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(depositFreezeMapper.selectList(any())).thenReturn(Collections.emptyList());

        CompleteSettlementRequest request = new CompleteSettlementRequest();
        request.setRenterId(10L);
        request.setOwnerId(20L);
        request.setOrderId(100L);
        request.setRentAmount(new BigDecimal("200.00"));
        request.setDepositAmount(new BigDecimal("5.00"));
        request.setFrozenDepositAmount(new BigDecimal("5.00"));
        request.setPaidRentAmount(new BigDecimal("200.00"));
        // 12 小时前结束 -> overdueFee ≈ 7.50，押金 5 不足
        request.setRentEndTime(LocalDateTime.now().minusHours(12));
        request.setDailyPrice(new BigDecimal("10.00"));

        when(accountMapper.updateById(any(WalletAccount.class))).thenReturn(1);

        SettlementResponse response = walletService.completeSettlement(request);

        assertThat(response.getOrderId()).isEqualTo(100L);
        BigDecimal overdueFee = response.getOverdueFeeAmount();
        assertThat(overdueFee).isNotNull();
        assertThat(overdueFee.subtract(new BigDecimal("7.50")).abs())
                .isLessThanOrEqualTo(new BigDecimal("0.10"));
        // 押金全部被扣除
        assertThat(response.getDepositDeductedAmount()).isEqualByComparingTo(new BigDecimal("5.00"));
        // renter frozen 清零
        assertThat(renterAccount.getFrozenAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        // renter balance 扣除剩余逾期费 (overdueFee - 5)
        BigDecimal remaining = overdueFee.subtract(new BigDecimal("5.00"));
        assertThat(renterAccount.getBalance())
                .isEqualByComparingTo(new BigDecimal("1000.00").subtract(remaining));
        // owner 应收到 租金 + 完整逾期费 (5 来自押金 + remaining 来自 renter balance)
        assertThat(ownerAccount.getBalance())
                .isEqualByComparingTo(new BigDecimal("200.00").add(overdueFee));
        verify(orderSettlementMapper).insert(any(OrderSettlement.class));
    }
}
