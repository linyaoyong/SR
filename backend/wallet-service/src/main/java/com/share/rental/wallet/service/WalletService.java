package com.share.rental.wallet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.wallet.dto.CancelOrderPaymentRequest;
import com.share.rental.wallet.dto.CompleteSettlementRequest;
import com.share.rental.wallet.dto.FreezeDepositRequest;
import com.share.rental.wallet.dto.PrepayRequest;
import com.share.rental.wallet.dto.SettlementResponse;
import com.share.rental.wallet.dto.WalletMeResponse;
import com.share.rental.wallet.dto.WalletOperationResponse;
import com.share.rental.wallet.dto.WalletTransactionResponse;
import com.share.rental.wallet.dto.WalletUsableResponse;
import com.share.rental.wallet.entity.DepositFreeze;
import com.share.rental.wallet.entity.OrderSettlement;
import com.share.rental.wallet.entity.WalletAccount;
import com.share.rental.wallet.entity.WalletTransaction;
import com.share.rental.wallet.enums.DepositStatusEnum;
import com.share.rental.wallet.enums.SettlementStatusEnum;
import com.share.rental.wallet.enums.WalletTransactionTypeEnum;
import com.share.rental.wallet.mapper.DepositFreezeMapper;
import com.share.rental.wallet.mapper.OrderSettlementMapper;
import com.share.rental.wallet.mapper.WalletAccountMapper;
import com.share.rental.wallet.mapper.WalletTransactionMapper;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RefreshScope
public class WalletService {

    private final WalletAccountMapper accountMapper;
    private final WalletTransactionMapper transactionMapper;
    private final DepositFreezeMapper depositFreezeMapper;
    private final OrderSettlementMapper orderSettlementMapper;

    @Value("${resilience.demo.wallet-delay-ms:0}")
    private long walletDelayMs;

    @Autowired
    public WalletService(WalletAccountMapper accountMapper,
                         WalletTransactionMapper transactionMapper,
                         DepositFreezeMapper depositFreezeMapper,
                         OrderSettlementMapper orderSettlementMapper) {
        this.accountMapper = accountMapper;
        this.transactionMapper = transactionMapper;
        this.depositFreezeMapper = depositFreezeMapper;
        this.orderSettlementMapper = orderSettlementMapper;
    }

    private void updateAccount(WalletAccount account) {
        if (accountMapper.updateById(account) != 1) {
            throw new BusinessException(ErrorCode.WALLET_UPDATE_CONFLICT);
        }
    }

    public WalletMeResponse getMyWallet(Long userId) {
        WalletAccount account = getOrCreateWallet(userId);
        return new WalletMeResponse(
                account.getUserId(),
                account.getBalance(),
                account.getFrozenAmount(),
                account.getStatus()
        );
    }

    @Transactional
    public void recharge(Long userId, BigDecimal amount, String remark) {
        WalletAccount account = getOrCreateWallet(userId);
        BigDecimal balanceBefore = account.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);
        account.setBalance(balanceAfter);
        updateAccount(account);

        WalletTransaction tx = new WalletTransaction();
        tx.setTransactionNo("RC" + System.currentTimeMillis() + userId);
        tx.setUserId(userId);
        tx.setType(1);
        tx.setDirection(1);
        tx.setAmount(amount);
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(balanceAfter);
        tx.setFrozenBefore(account.getFrozenAmount());
        tx.setFrozenAfter(account.getFrozenAmount());
        tx.setRemark(remark != null ? remark : "模拟充值");
        transactionMapper.insert(tx);
    }

    public List<WalletTransactionResponse> listTransactions(Long userId, Integer page, Integer size) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        int offset = (pageNo - 1) * pageSize;
        List<WalletTransaction> records = transactionMapper.selectList(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getUserId, userId)
                .orderByDesc(WalletTransaction::getCreateTime)
                .last("LIMIT " + pageSize + " OFFSET " + offset));
        return records.stream().map(tx -> new WalletTransactionResponse(
                tx.getId(),
                tx.getTransactionNo(),
                tx.getType(),
                tx.getDirection(),
                tx.getAmount(),
                tx.getBalanceBefore(),
                tx.getBalanceAfter(),
                tx.getRemark(),
                tx.getCreateTime()
        )).collect(Collectors.toList());
    }

    public boolean isUsable(Long userId) {
        return getUsable(userId).getUsable();
    }

    public WalletUsableResponse getUsable(Long userId) {
        applyDemoDelay();
        WalletAccount account = accountMapper.selectOne(new LambdaQueryWrapper<WalletAccount>()
                .eq(WalletAccount::getUserId, userId));
        if (account == null) {
            return new WalletUsableResponse(userId, false, null);
        }
        BigDecimal balance = account.getBalance();
        boolean usable = balance != null && balance.compareTo(BigDecimal.ZERO) > 0;
        return new WalletUsableResponse(userId, usable, balance);
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletOperationResponse prepayRent(PrepayRequest request) {
        applyDemoDelay();
        Long renterId = request.getRenterId();
        Long orderId = request.getOrderId();
        BigDecimal amount = request.getAmount();

        WalletAccount account = getOrCreateWallet(renterId);
        if (account.getBalance() == null || account.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
        }

        BigDecimal balanceBefore = account.getBalance();
        BigDecimal frozenAmount = account.getFrozenAmount();
        BigDecimal balanceAfter = balanceBefore.subtract(amount);
        account.setBalance(balanceAfter);
        updateAccount(account);

        writeTransaction(
                "PR" + System.currentTimeMillis() + renterId,
                renterId,
                null,
                request.getApplicationId(),
                request.getProposalId(),
                orderId,
                WalletTransactionTypeEnum.PAY_RENT.code(),
                2,
                amount,
                balanceBefore,
                balanceAfter,
                frozenAmount,
                frozenAmount,
                "支付租金"
        );

        BigDecimal totalPaid = getPaidRentTotal(orderId);
        BigDecimal frozenTotal = getFrozenDepositTotal(orderId);
        return new WalletOperationResponse(orderId, totalPaid, frozenTotal, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletOperationResponse freezeDeposit(FreezeDepositRequest request) {
        applyDemoDelay();
        Long renterId = request.getRenterId();
        Long orderId = request.getOrderId();
        BigDecimal amount = request.getAmount();

        WalletAccount account = getOrCreateWallet(renterId);
        if (account.getBalance() == null || account.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
        }

        BigDecimal balanceBefore = account.getBalance();
        BigDecimal frozenBefore = account.getFrozenAmount();
        BigDecimal balanceAfter = balanceBefore.subtract(amount);
        BigDecimal frozenAfter = frozenBefore.add(amount);
        account.setBalance(balanceAfter);
        account.setFrozenAmount(frozenAfter);
        updateAccount(account);

        writeTransaction(
                "FD" + System.currentTimeMillis() + renterId,
                renterId,
                null,
                request.getApplicationId(),
                request.getProposalId(),
                orderId,
                WalletTransactionTypeEnum.FREEZE_DEPOSIT.code(),
                3,
                amount,
                balanceBefore,
                balanceAfter,
                frozenBefore,
                frozenAfter,
                "冻结押金"
        );

        DepositFreeze freeze = new DepositFreeze();
        freeze.setApplicationId(request.getApplicationId());
        freeze.setProposalId(request.getProposalId());
        freeze.setOrderId(orderId);
        freeze.setUserId(renterId);
        freeze.setAmount(amount);
        freeze.setDeductedAmount(BigDecimal.ZERO);
        freeze.setStatus(DepositStatusEnum.FROZEN.code());
        freeze.setFreezeTime(LocalDateTime.now());
        depositFreezeMapper.insert(freeze);

        BigDecimal totalPaid = getPaidRentTotal(orderId);
        BigDecimal frozenTotal = getFrozenDepositTotal(orderId);
        return new WalletOperationResponse(orderId, totalPaid, frozenTotal, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletOperationResponse cancelOrderPayment(CancelOrderPaymentRequest request) {
        Long renterId = request.getRenterId();
        Long orderId = request.getOrderId();
        BigDecimal paidRent = request.getPaidRentAmount() == null
                ? BigDecimal.ZERO : request.getPaidRentAmount();
        BigDecimal frozenDeposit = request.getFrozenDepositAmount() == null
                ? BigDecimal.ZERO : request.getFrozenDepositAmount();

        WalletAccount account = getOrCreateWallet(renterId);

        if (paidRent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal balanceBefore = account.getBalance();
            BigDecimal frozenAmount = account.getFrozenAmount();
            BigDecimal balanceAfter = balanceBefore.add(paidRent);
            account.setBalance(balanceAfter);
            updateAccount(account);

            writeTransaction(
                    "CR" + System.currentTimeMillis() + renterId,
                    renterId,
                    null,
                    null,
                    null,
                    orderId,
                    WalletTransactionTypeEnum.CANCEL_REFUND.code(),
                    1,
                    paidRent,
                    balanceBefore,
                    balanceAfter,
                    frozenAmount,
                    frozenAmount,
                    "取消订单退回租金"
            );
        }

        if (frozenDeposit.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal balanceBefore = account.getBalance();
            BigDecimal frozenBefore = account.getFrozenAmount();
            BigDecimal balanceAfter = balanceBefore.add(frozenDeposit);
            BigDecimal frozenAfter = frozenBefore.subtract(frozenDeposit);
            account.setBalance(balanceAfter);
            account.setFrozenAmount(frozenAfter);
            updateAccount(account);

            writeTransaction(
                    "RD" + System.currentTimeMillis() + renterId,
                    renterId,
                    null,
                    null,
                    null,
                    orderId,
                    WalletTransactionTypeEnum.RELEASE_DEPOSIT.code(),
                    4,
                    frozenDeposit,
                    balanceBefore,
                    balanceAfter,
                    frozenBefore,
                    frozenAfter,
                    "取消订单释放押金"
            );
        }

        List<DepositFreeze> freezes = depositFreezeMapper.selectList(new LambdaQueryWrapper<DepositFreeze>()
                .eq(DepositFreeze::getOrderId, orderId)
                .eq(DepositFreeze::getStatus, DepositStatusEnum.FROZEN.code()));
        LocalDateTime now = LocalDateTime.now();
        for (DepositFreeze f : freezes) {
            f.setStatus(DepositStatusEnum.CANCELLED.code());
            f.setCancelTime(now);
            depositFreezeMapper.updateById(f);
        }

        BigDecimal totalPaid = getPaidRentTotal(orderId);
        BigDecimal frozenTotal = getFrozenDepositTotal(orderId);
        return new WalletOperationResponse(orderId, totalPaid, frozenTotal, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public SettlementResponse completeSettlement(CompleteSettlementRequest request) {
        Long orderId = request.getOrderId();
        Long renterId = request.getRenterId();
        Long ownerId = request.getOwnerId();

        OrderSettlement existing = orderSettlementMapper.selectOne(new LambdaQueryWrapper<OrderSettlement>()
                .eq(OrderSettlement::getOrderId, orderId));
        if (existing != null && existing.getStatus() != null
                && existing.getStatus() == SettlementStatusEnum.SETTLED.code()) {
            return toSettlementResponse(existing);
        }

        WalletAccount renterAccount = getOrCreateWallet(renterId);
        WalletAccount ownerAccount = getOrCreateWallet(ownerId);

        BigDecimal paidRent = request.getPaidRentAmount() == null
                ? BigDecimal.ZERO : request.getPaidRentAmount();
        BigDecimal frozenDeposit = request.getFrozenDepositAmount() == null
                ? BigDecimal.ZERO : request.getFrozenDepositAmount();
        BigDecimal depositAmount = request.getDepositAmount() == null
                ? BigDecimal.ZERO : request.getDepositAmount();
        BigDecimal dailyPrice = request.getDailyPrice() == null
                ? BigDecimal.ZERO : request.getDailyPrice();

        BigDecimal overdueFee = calculateOverdueFee(request.getRentEndTime(), dailyPrice);

        // 1) 租金转入 owner
        if (paidRent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ownerBalanceBefore = ownerAccount.getBalance();
            BigDecimal ownerFrozen = ownerAccount.getFrozenAmount();
            BigDecimal ownerBalanceAfter = ownerBalanceBefore.add(paidRent);
            ownerAccount.setBalance(ownerBalanceAfter);
            updateAccount(ownerAccount);

            writeTransaction(
                    "RI" + System.currentTimeMillis() + ownerId,
                    ownerId,
                    renterId,
                    null,
                    null,
                    orderId,
                    WalletTransactionTypeEnum.RENT_INCOME.code(),
                    1,
                    paidRent,
                    ownerBalanceBefore,
                    ownerBalanceAfter,
                    ownerFrozen,
                    ownerFrozen,
                    "租金收入"
            );
        }

        // 2) 逾期费用处理
        BigDecimal depositDeductedAmount = BigDecimal.ZERO;
        if (overdueFee.compareTo(BigDecimal.ZERO) > 0) {
            if (frozenDeposit.compareTo(overdueFee) >= 0) {
                // 押金足以覆盖逾期费用：从押金扣除转给 owner
                BigDecimal renterFrozenBefore = renterAccount.getFrozenAmount();
                BigDecimal renterFrozenAfter = renterFrozenBefore.subtract(overdueFee);
                renterAccount.setFrozenAmount(renterFrozenAfter);
                updateAccount(renterAccount);

                BigDecimal ownerBalanceBefore = ownerAccount.getBalance();
                BigDecimal ownerFrozen = ownerAccount.getFrozenAmount();
                BigDecimal ownerBalanceAfter = ownerBalanceBefore.add(overdueFee);
                ownerAccount.setBalance(ownerBalanceAfter);
                updateAccount(ownerAccount);

                writeTransaction(
                        "OI" + System.currentTimeMillis() + ownerId,
                        ownerId,
                        renterId,
                        null,
                        null,
                        orderId,
                        WalletTransactionTypeEnum.OVERDUE_INCOME.code(),
                        1,
                        overdueFee,
                        ownerBalanceBefore,
                        ownerBalanceAfter,
                        ownerFrozen,
                        ownerFrozen,
                        "逾期收入"
                );
                depositDeductedAmount = overdueFee;
            } else {
                // 押金不足：押金全部扣除转 owner，剩余从 renter balance 扣除
                if (frozenDeposit.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal renterFrozenBefore = renterAccount.getFrozenAmount();
                    BigDecimal renterFrozenAfter = renterFrozenBefore.subtract(frozenDeposit);
                    renterAccount.setFrozenAmount(renterFrozenAfter);
                    updateAccount(renterAccount);

                    BigDecimal ownerBalanceBefore = ownerAccount.getBalance();
                    BigDecimal ownerFrozen = ownerAccount.getFrozenAmount();
                    BigDecimal ownerBalanceAfter = ownerBalanceBefore.add(frozenDeposit);
                    ownerAccount.setBalance(ownerBalanceAfter);
                    updateAccount(ownerAccount);

                    writeTransaction(
                            "OI" + System.currentTimeMillis() + ownerId,
                            ownerId,
                            renterId,
                            null,
                            null,
                            orderId,
                            WalletTransactionTypeEnum.OVERDUE_INCOME.code(),
                            1,
                            frozenDeposit,
                            ownerBalanceBefore,
                            ownerBalanceAfter,
                            ownerFrozen,
                            ownerFrozen,
                            "逾期收入（押金部分）"
                    );
                }
                BigDecimal remaining = overdueFee.subtract(frozenDeposit);
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal renterBalanceBefore = renterAccount.getBalance();
                    BigDecimal renterFrozen = renterAccount.getFrozenAmount();
                    BigDecimal renterBalanceAfter = renterBalanceBefore.subtract(remaining);
                    renterAccount.setBalance(renterBalanceAfter);
                    updateAccount(renterAccount);

                    writeTransaction(
                            "OE" + System.currentTimeMillis() + renterId,
                            renterId,
                            ownerId,
                            null,
                            null,
                            orderId,
                            WalletTransactionTypeEnum.OVERDUE_EXPENSE.code(),
                            2,
                            remaining,
                            renterBalanceBefore,
                            renterBalanceAfter,
                            renterFrozen,
                            renterFrozen,
                            "逾期支出"
                    );

                    // 剩余逾期费也转给 owner
                    BigDecimal ownerBalanceBefore = ownerAccount.getBalance();
                    BigDecimal ownerFrozen = ownerAccount.getFrozenAmount();
                    BigDecimal ownerBalanceAfter = ownerBalanceBefore.add(remaining);
                    ownerAccount.setBalance(ownerBalanceAfter);
                    updateAccount(ownerAccount);

                    writeTransaction(
                            "OI" + System.currentTimeMillis() + ownerId,
                            ownerId,
                            renterId,
                            null,
                            null,
                            orderId,
                            WalletTransactionTypeEnum.OVERDUE_INCOME.code(),
                            1,
                            remaining,
                            ownerBalanceBefore,
                            ownerBalanceAfter,
                            ownerFrozen,
                            ownerFrozen,
                            "逾期收入（余额部分）"
                    );
                }
                depositDeductedAmount = frozenDeposit;
            }
        }

        // 3) 剩余押金释放回 renter
        BigDecimal remainingFrozen = frozenDeposit.subtract(depositDeductedAmount);
        if (remainingFrozen.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal renterBalanceBefore = renterAccount.getBalance();
            BigDecimal renterFrozenBefore = renterAccount.getFrozenAmount();
            BigDecimal renterBalanceAfter = renterBalanceBefore.add(remainingFrozen);
            BigDecimal renterFrozenAfter = renterFrozenBefore.subtract(remainingFrozen);
            renterAccount.setBalance(renterBalanceAfter);
            renterAccount.setFrozenAmount(renterFrozenAfter);
            updateAccount(renterAccount);

            writeTransaction(
                    "RL" + System.currentTimeMillis() + renterId,
                    renterId,
                    null,
                    null,
                    null,
                    orderId,
                    WalletTransactionTypeEnum.RELEASE_DEPOSIT.code(),
                    4,
                    remainingFrozen,
                    renterBalanceBefore,
                    renterBalanceAfter,
                    renterFrozenBefore,
                    renterFrozenAfter,
                    "结算释放剩余押金"
            );
        }

        // 4) 更新 deposit_freezes 状态
        List<DepositFreeze> freezes = depositFreezeMapper.selectList(new LambdaQueryWrapper<DepositFreeze>()
                .eq(DepositFreeze::getOrderId, orderId)
                .eq(DepositFreeze::getStatus, DepositStatusEnum.FROZEN.code()));
        LocalDateTime now = LocalDateTime.now();
        for (DepositFreeze f : freezes) {
            if (depositDeductedAmount.compareTo(BigDecimal.ZERO) > 0
                    && remainingFrozen.compareTo(BigDecimal.ZERO) > 0) {
                f.setStatus(DepositStatusEnum.PARTIAL_DEDUCTED_RELEASED.code());
            } else if (depositDeductedAmount.compareTo(BigDecimal.ZERO) > 0) {
                f.setStatus(DepositStatusEnum.DEDUCTED.code());
            } else {
                f.setStatus(DepositStatusEnum.RELEASED.code());
            }
            f.setReleaseTime(now);
            depositFreezeMapper.updateById(f);
        }

        // 5) 创建 OrderSettlement 记录
        OrderSettlement settlement = new OrderSettlement();
        settlement.setOrderId(orderId);
        settlement.setRenterId(renterId);
        settlement.setOwnerId(ownerId);
        settlement.setRentAmount(paidRent);
        settlement.setDepositAmount(depositAmount);
        settlement.setOverdueFeeAmount(overdueFee);
        settlement.setDepositDeductedAmount(depositDeductedAmount);
        settlement.setRentSettled(1);
        settlement.setDepositReleased(1);
        settlement.setOverdueFeeSettled(1);
        settlement.setStatus(SettlementStatusEnum.SETTLED.code());
        settlement.setSettleTime(now);
        orderSettlementMapper.insert(settlement);

        return toSettlementResponse(settlement);
    }

    public SettlementResponse getSettlement(Long orderId) {
        OrderSettlement settlement = orderSettlementMapper.selectOne(new LambdaQueryWrapper<OrderSettlement>()
                .eq(OrderSettlement::getOrderId, orderId));
        if (settlement == null) {
            throw new BusinessException(ErrorCode.WALLET_SETTLEMENT_NOT_FOUND);
        }
        return toSettlementResponse(settlement);
    }

    private BigDecimal getPaidRentTotal(Long orderId) {
        List<WalletTransaction> list = transactionMapper.selectList(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getOrderId, orderId)
                .eq(WalletTransaction::getType, WalletTransactionTypeEnum.PAY_RENT.code()));
        return list.stream()
                .map(WalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getFrozenDepositTotal(Long orderId) {
        List<WalletTransaction> list = transactionMapper.selectList(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getOrderId, orderId)
                .eq(WalletTransaction::getType, WalletTransactionTypeEnum.FREEZE_DEPOSIT.code()));
        return list.stream()
                .map(WalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void writeTransaction(String transactionNo, Long userId, Long counterpartyUserId,
                                  Long applicationId, Long proposalId, Long orderId,
                                  int type, int direction, BigDecimal amount,
                                  BigDecimal balanceBefore, BigDecimal balanceAfter,
                                  BigDecimal frozenBefore, BigDecimal frozenAfter,
                                  String remark) {
        WalletTransaction tx = new WalletTransaction();
        tx.setTransactionNo(transactionNo);
        tx.setUserId(userId);
        tx.setCounterpartyUserId(counterpartyUserId);
        tx.setApplicationId(applicationId);
        tx.setProposalId(proposalId);
        tx.setOrderId(orderId);
        tx.setType(type);
        tx.setDirection(direction);
        tx.setAmount(amount);
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(balanceAfter);
        tx.setFrozenBefore(frozenBefore);
        tx.setFrozenAfter(frozenAfter);
        tx.setRemark(remark);
        transactionMapper.insert(tx);
    }

    private BigDecimal calculateOverdueFee(LocalDateTime rentEndTime, BigDecimal dailyPrice) {
        if (rentEndTime == null || dailyPrice == null || dailyPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        LocalDateTime now = LocalDateTime.now();
        if (!now.isAfter(rentEndTime)) {
            return BigDecimal.ZERO;
        }
        long minutes = Duration.between(rentEndTime, now).toMinutes();
        if (minutes <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal minutesBd = new BigDecimal(minutes);
        BigDecimal dayMinutes = new BigDecimal(1440);
        if (minutes < 1440L) {
            BigDecimal ratio = minutesBd.divide(dayMinutes, 10, RoundingMode.HALF_UP);
            return dailyPrice.multiply(new BigDecimal("1.5"))
                    .multiply(ratio)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal firstDay = dailyPrice.multiply(new BigDecimal("1.5"));
        long extraMinutes = minutes - 1440L;
        BigDecimal extraDays = new BigDecimal(extraMinutes).divide(dayMinutes, 10, RoundingMode.HALF_UP);
        BigDecimal extra = dailyPrice.multiply(new BigDecimal("2")).multiply(extraDays);
        return firstDay.add(extra).setScale(2, RoundingMode.HALF_UP);
    }

    private SettlementResponse toSettlementResponse(OrderSettlement s) {
        return new SettlementResponse(
                s.getOrderId(),
                s.getRentSettled() != null && s.getRentSettled() == 1,
                s.getDepositReleased() != null && s.getDepositReleased() == 1,
                s.getOverdueFeeAmount(),
                s.getDepositDeductedAmount()
        );
    }

    private WalletAccount getOrCreateWallet(Long userId) {
        WalletAccount account = accountMapper.selectOne(new LambdaQueryWrapper<WalletAccount>()
                .eq(WalletAccount::getUserId, userId));
        if (account == null) {
            account = new WalletAccount();
            account.setUserId(userId);
            account.setBalance(BigDecimal.ZERO);
            account.setFrozenAmount(BigDecimal.ZERO);
            account.setStatus(0);
            accountMapper.insert(account);
        }
        return account;
    }

    private void applyDemoDelay() {
        if (walletDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(walletDelayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SERVICE_BUSY);
        }
    }
}
