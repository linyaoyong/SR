package com.share.rental.wallet.service;

import com.share.rental.common.exception.BusinessException;
import com.share.rental.wallet.dto.*;
import com.share.rental.wallet.entity.WalletAccount;
import com.share.rental.wallet.mapper.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletConflictTest {
    @Mock WalletAccountMapper accountMapper;
    @Mock WalletTransactionMapper transactionMapper;
    @Mock DepositFreezeMapper depositFreezeMapper;
    @Mock OrderSettlementMapper orderSettlementMapper;
    @InjectMocks WalletService service;

    // Every account update site is exercised, including later failures after an earlier write.
    @ParameterizedTest
    @CsvSource({"prepay,1,0", "freeze,1,0", "cancel,1,0", "cancel,2,1",
            "settle,1,0", "settle,2,1", "covered,1,0", "covered,2,1",
            "covered,3,1", "covered,4,2", "uncovered,1,0", "uncovered,2,1",
            "uncovered,3,1", "uncovered,4,2", "uncovered,5,3"})
    void updateConflictStopsAllSubsequentWrites(String scenario, int failedUpdate, int earlierTransactions) {
        WalletAccount renter = account(10L);
        WalletAccount owner = account(20L);
        when(accountMapper.selectOne(any())).thenReturn(renter, owner);
        AtomicInteger calls = new AtomicInteger();
        when(accountMapper.updateById(any(WalletAccount.class)))
                .thenAnswer(invocation -> calls.incrementAndGet() == failedUpdate ? 0 : 1);

        BusinessException error = assertThrows(BusinessException.class, () -> {
            switch (scenario) {
                case "prepay" -> {
                    PrepayRequest request = new PrepayRequest();
                    request.setRenterId(10L); request.setOrderId(100L); request.setAmount(BigDecimal.TEN);
                    service.prepayRent(request);
                }
                case "freeze" -> {
                    FreezeDepositRequest request = new FreezeDepositRequest();
                    request.setRenterId(10L); request.setOrderId(100L); request.setAmount(BigDecimal.TEN);
                    service.freezeDeposit(request);
                }
                case "cancel" -> {
                    CancelOrderPaymentRequest request = new CancelOrderPaymentRequest();
                    request.setRenterId(10L); request.setOrderId(100L);
                    request.setPaidRentAmount(BigDecimal.TEN); request.setFrozenDepositAmount(BigDecimal.TEN);
                    service.cancelOrderPayment(request);
                }
                default -> {
                    CompleteSettlementRequest request = new CompleteSettlementRequest();
                    request.setRenterId(10L); request.setOwnerId(20L); request.setOrderId(100L);
                    request.setPaidRentAmount(BigDecimal.TEN);
                    request.setFrozenDepositAmount(scenario.equals("uncovered") ? BigDecimal.ONE : new BigDecimal("100"));
                    request.setDailyPrice(BigDecimal.TEN);
                    request.setRentEndTime(scenario.equals("settle") ? LocalDateTime.now().plusDays(1)
                            : LocalDateTime.now().minusHours(12));
                    service.completeSettlement(request);
                }
            }
        });
        assertEquals(40412, error.errorCode().code());
        assertEquals(failedUpdate, calls.get());
        verify(transactionMapper, times(earlierTransactions)).insert(any(com.share.rental.wallet.entity.WalletTransaction.class));
        verifyNoInteractions(depositFreezeMapper);
        verify(orderSettlementMapper, never()).insert(any(com.share.rental.wallet.entity.OrderSettlement.class));
    }

    @org.junit.jupiter.api.Test
    void lateConflictTriggersSpringTransactionRollbackInsteadOfCommit() {
        WalletAccount renter = account(10L);
        when(accountMapper.selectOne(any())).thenReturn(renter);
        when(accountMapper.updateById(renter)).thenReturn(1, 0);
        org.springframework.transaction.PlatformTransactionManager manager =
                mock(org.springframework.transaction.PlatformTransactionManager.class);
        org.springframework.transaction.TransactionStatus status =
                new org.springframework.transaction.support.SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(status);
        org.springframework.aop.framework.ProxyFactory factory =
                new org.springframework.aop.framework.ProxyFactory(service);
        factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(manager,
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        WalletService transactional = (WalletService) factory.getProxy();
        CancelOrderPaymentRequest request = new CancelOrderPaymentRequest();
        request.setRenterId(10L); request.setOrderId(100L);
        request.setPaidRentAmount(BigDecimal.TEN); request.setFrozenDepositAmount(BigDecimal.TEN);
        assertThrows(BusinessException.class, () -> transactional.cancelOrderPayment(request));
        verify(transactionMapper).insert(any(com.share.rental.wallet.entity.WalletTransaction.class));
        verify(manager).rollback(status);
        verify(manager, never()).commit(any());
        verifyNoInteractions(depositFreezeMapper, orderSettlementMapper);
    }

    private WalletAccount account(Long userId) {
        WalletAccount account = new WalletAccount();
        account.setId(userId); account.setUserId(userId);
        account.setBalance(new BigDecimal("1000")); account.setFrozenAmount(new BigDecimal("100"));
        return account;
    }
}
