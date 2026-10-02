package com.share.rental.rental.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.response.ApiResponse;
import com.share.rental.rental.client.ItemRentalClient;
import com.share.rental.rental.client.MessageRentalClient;
import com.share.rental.rental.client.WalletRentalClient;
import com.share.rental.rental.client.AuthRentalClient;
import com.share.rental.rental.dto.CancelOrderRequest;
import com.share.rental.rental.dto.CancelPaymentFeignRequest;
import com.share.rental.rental.dto.CompleteSettlementFeignRequest;
import com.share.rental.rental.dto.CreditScoreAdjustFeignRequest;
import com.share.rental.rental.dto.FreezeDepositFeignRequest;
import com.share.rental.rental.dto.ItemInfoFeignResponse;
import com.share.rental.rental.dto.ItemSnapshotFeignResponse;
import com.share.rental.rental.dto.PrepayRentFeignRequest;
import com.share.rental.rental.dto.SystemNotificationRequest;
import com.share.rental.rental.dto.RentalOrderResponse;
import com.share.rental.rental.dto.ReturnOrderRequest;
import com.share.rental.rental.dto.SettlementResponse;
import com.share.rental.rental.dto.ShipOrderRequest;
import com.share.rental.rental.dto.WalletOperationResponse;
import com.share.rental.rental.entity.OrderStatusHistory;
import com.share.rental.rental.entity.RentalApplication;
import com.share.rental.rental.entity.RentalOrder;
import com.share.rental.rental.entity.RentalProposal;
import com.share.rental.rental.enums.ApplicationStatusEnum;
import com.share.rental.rental.enums.DeliveryTypeEnum;
import com.share.rental.rental.enums.OrderStatusEnum;
import com.share.rental.rental.mapper.OrderStatusHistoryMapper;
import com.share.rental.rental.mapper.RentalApplicationMapper;
import com.share.rental.rental.mapper.RentalOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RentalOrderService {

    private final RentalOrderMapper orderMapper;
    private final OrderStatusHistoryMapper historyMapper;
    private final RentalApplicationMapper applicationMapper;
    private final ItemRentalClient itemClient;
    private final RentalTimeLockService timeLockService;
    private final OrderNumberGenerator orderNumberGenerator;
    private final WalletRentalClient walletClient;
    private final MessageRentalClient messageClient;
    private final AuthRentalClient authClient;
    private final PaymentTimeoutProducer paymentTimeoutProducer;

    @Autowired
    public RentalOrderService(RentalOrderMapper orderMapper,
                              OrderStatusHistoryMapper historyMapper,
                              RentalApplicationMapper applicationMapper,
                              ItemRentalClient itemClient,
                              RentalTimeLockService timeLockService,
                              OrderNumberGenerator orderNumberGenerator,
                              WalletRentalClient walletClient,
                              MessageRentalClient messageClient,
                              AuthRentalClient authClient,
                              PaymentTimeoutProducer paymentTimeoutProducer) {
        this.orderMapper = orderMapper;
        this.historyMapper = historyMapper;
        this.applicationMapper = applicationMapper;
        this.itemClient = itemClient;
        this.timeLockService = timeLockService;
        this.orderNumberGenerator = orderNumberGenerator;
        this.walletClient = walletClient;
        this.messageClient = messageClient;
        this.authClient = authClient;
        this.paymentTimeoutProducer = paymentTimeoutProducer;
    }

    @Transactional(rollbackFor = Exception.class)
    public void createOrder(RentalApplication application, RentalProposal proposal) {
        ItemInfoFeignResponse item = requireItem(application.getItemId());
        if (item.getQuantity() == null) {
            throw new BusinessException(ErrorCode.RENTAL_ITEM_UNAVAILABLE);
        }
        Long snapshotId = requireSnapshot(application.getItemId());
        String orderNo = orderNumberGenerator.next();

        RentalOrder order = new RentalOrder();
        order.setOrderNo(orderNo);
        order.setApplicationId(application.getId());
        order.setProposalId(proposal.getId());
        order.setItemId(application.getItemId());
        order.setItemSnapshotId(snapshotId);
        order.setOwnerId(application.getOwnerId());
        order.setRenterId(application.getRenterId());
        order.setQuantity(proposal.getQuantity());
        order.setDeliveryType(proposal.getDeliveryType());
        order.setRentStartTime(proposal.getRentStartTime());
        order.setRentEndTime(proposal.getRentEndTime());
        order.setDailyPrice(item.getDailyPrice());
        order.setRentAmount(proposal.getRentAmount());
        order.setDepositAmount(proposal.getDepositAmount());
        order.setPaidRentAmount(application.getPrepaidRentAmount());
        order.setFrozenDepositAmount(application.getPreFrozenDepositAmount());
        order.setStatus(OrderStatusEnum.PENDING_PAYMENT.code());
        order.setOverdueSettled(0);
        orderMapper.insert(order);

        timeLockService.createLockIfAvailable(order.getId(), application.getItemId(), proposal.getQuantity(),
                proposal.getRentStartTime(), proposal.getRentEndTime(), item.getQuantity());

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(order.getId());
        history.setOldStatus(null);
        history.setNewStatus(OrderStatusEnum.PENDING_PAYMENT.code());
        history.setOperatorId(proposal.getOperatorId());
        historyMapper.insert(history);

        application.setStatus(ApplicationStatusEnum.CONVERTED.code());
        applicationMapper.updateById(application);
        reserveItemStock(application.getItemId(), proposal.getQuantity());
        paymentTimeoutProducer.publish(order.getId());
    }

    public List<RentalOrderResponse> listOrders(Long userId) {
        QueryWrapper<RentalOrder> wrapper = new QueryWrapper<RentalOrder>()
                .eq("owner_id", userId)
                .or()
                .eq("renter_id", userId)
                .orderByDesc("create_time");
        List<RentalOrder> orders = orderMapper.selectList(wrapper);
        return orders.stream().map(order -> toResponse(order, false)).collect(Collectors.toList());
    }

    public List<RentalOrderResponse> listPublicCompletedOrders(Long userId) {
        QueryWrapper<RentalOrder> wrapper = new QueryWrapper<RentalOrder>()
                .eq("status", OrderStatusEnum.COMPLETED.code())
                .and(w -> w.eq("owner_id", userId).or().eq("renter_id", userId))
                .orderByDesc("completed_time")
                .orderByDesc("create_time")
                .last("LIMIT 20");
        List<RentalOrder> orders = orderMapper.selectList(wrapper);
        return orders.stream().map(order -> toResponse(order, true)).collect(Collectors.toList());
    }

    public RentalOrderResponse getOrder(Long orderId, Long userId) {
        RentalOrder order = requireOrder(orderId);
        requireParticipant(order, userId);
        return toResponse(order, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public RentalOrderResponse payOrder(Long orderId, Long renterId, BigDecimal amount) {
        RentalOrder order = requireOrder(orderId);
        requireRenter(order, renterId);
        if (order.getStatus() == null
                || order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.code()) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_STATUS_INVALID);
        }
        PrepayRentFeignRequest request = new PrepayRentFeignRequest(renterId, orderId, amount,
                order.getApplicationId(), order.getProposalId());
        ApiResponse<WalletOperationResponse> resp = ResilientRemoteCallService.callWallet(
                () -> walletClient.prepayRent(orderId, request));
        if (resp == null || resp.data() == null) {
            throw new BusinessException(ErrorCode.REMOTE_CALL_FAILED);
        }
        order.setPaidRentAmount(resp.data().getPaidRentAmount());
        checkAndAdvanceToPaid(order, renterId);
        orderMapper.updateById(order);
        return toResponse(order, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public RentalOrderResponse freezeDeposit(Long orderId, Long renterId, BigDecimal amount) {
        RentalOrder order = requireOrder(orderId);
        requireRenter(order, renterId);
        if (order.getStatus() == null
                || order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.code()) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_STATUS_INVALID);
        }
        FreezeDepositFeignRequest request = new FreezeDepositFeignRequest(renterId, orderId, amount,
                order.getApplicationId(), order.getProposalId());
        ApiResponse<WalletOperationResponse> resp = ResilientRemoteCallService.callWallet(
                () -> walletClient.freezeDeposit(orderId, request));
        if (resp == null || resp.data() == null) {
            throw new BusinessException(ErrorCode.REMOTE_CALL_FAILED);
        }
        order.setFrozenDepositAmount(resp.data().getFrozenDepositAmount());
        checkAndAdvanceToPaid(order, renterId);
        orderMapper.updateById(order);
        return toResponse(order, false);
    }

    private void checkAndAdvanceToPaid(RentalOrder order, Long operatorId) {
        if (order.getStatus() == null
                || order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.code()) {
            return;
        }
        BigDecimal paid = order.getPaidRentAmount() == null ? BigDecimal.ZERO : order.getPaidRentAmount();
        BigDecimal frozen = order.getFrozenDepositAmount() == null ? BigDecimal.ZERO : order.getFrozenDepositAmount();
        BigDecimal rent = order.getRentAmount() == null ? BigDecimal.ZERO : order.getRentAmount();
        BigDecimal deposit = order.getDepositAmount() == null ? BigDecimal.ZERO : order.getDepositAmount();
        if (paid.compareTo(rent) >= 0
                && frozen.compareTo(deposit) >= 0
                && order.getStatus() == OrderStatusEnum.PENDING_PAYMENT.code()) {
            Integer oldStatus = order.getStatus();
            order.setStatus(OrderStatusEnum.PAID_PENDING_DELIVERY.code());
            writeHistory(order.getId(), oldStatus, OrderStatusEnum.PAID_PENDING_DELIVERY.code(), operatorId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, Long userId, CancelOrderRequest request) {
        RentalOrder order = requireOrder(orderId);
        requireParticipant(order, userId);
        if (order.getStatus() == null
                || (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.code()
                        && order.getStatus() != OrderStatusEnum.PAID_PENDING_DELIVERY.code())) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_STATUS_INVALID);
        }
        cancelOrder(order, userId, request == null ? null : request.getCancelReason());
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean cancelPendingPaymentBySystem(Long orderId, String reason) {
        RentalOrder order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() == null
                || order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.code()) {
            return false;
        }
        cancelOrder(order, 0L, reason);
        return true;
    }

    private void cancelOrder(RentalOrder order, Long operatorId, String reason) {
        Integer oldStatus = order.getStatus();
        order.setStatus(OrderStatusEnum.CANCELLED.code());
        order.setCancelReason(reason);
        orderMapper.updateById(order);
        timeLockService.releaseLocks(order.getId());
        releaseItemStock(order.getItemId(), order.getQuantity());
        BigDecimal paid = order.getPaidRentAmount() == null ? BigDecimal.ZERO : order.getPaidRentAmount();
        BigDecimal frozen = order.getFrozenDepositAmount() == null ? BigDecimal.ZERO : order.getFrozenDepositAmount();
        if (paid.compareTo(BigDecimal.ZERO) > 0 || frozen.compareTo(BigDecimal.ZERO) > 0) {
            CancelPaymentFeignRequest cancelReq = new CancelPaymentFeignRequest(
                    order.getRenterId(), order.getOwnerId(), order.getId(), paid, frozen);
            ResilientRemoteCallService.callWallet(
                    () -> walletClient.cancelOrderPayment(order.getId(), cancelReq));
        }
        writeHistory(order.getId(), oldStatus, OrderStatusEnum.CANCELLED.code(), operatorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void ship(Long orderId, Long ownerId, ShipOrderRequest request) {
        RentalOrder order = requireOrder(orderId);
        requireOwner(order, ownerId);
        if (order.getStatus() == null
                || order.getStatus() != OrderStatusEnum.PAID_PENDING_DELIVERY.code()) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_STATUS_INVALID);
        }
        if (order.getDeliveryType() == null
                || order.getDeliveryType() != DeliveryTypeEnum.EXPRESS.code()) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_STATUS_INVALID);
        }
        Integer oldStatus = order.getStatus();
        order.setShipCompany(request.getShipCompany());
        order.setShipTrackingNo(request.getShipTrackingNo());
        order.setStatus(OrderStatusEnum.SHIPPED.code());
        orderMapper.updateById(order);
        writeHistory(orderId, oldStatus, OrderStatusEnum.SHIPPED.code(), ownerId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void receive(Long orderId, Long renterId) {
        RentalOrder order = requireOrder(orderId);
        requireRenter(order, renterId);
        Integer status = order.getStatus();
        Integer deliveryType = order.getDeliveryType();
        if (status == null || deliveryType == null) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_STATUS_INVALID);
        }
        if (deliveryType == DeliveryTypeEnum.EXPRESS.code()) {
            if (status != OrderStatusEnum.SHIPPED.code()) {
                throw new BusinessException(ErrorCode.RENTAL_ORDER_STATUS_INVALID);
            }
        } else if (deliveryType == DeliveryTypeEnum.MEETUP.code()) {
            if (status != OrderStatusEnum.PAID_PENDING_DELIVERY.code()) {
                throw new BusinessException(ErrorCode.RENTAL_ORDER_STATUS_INVALID);
            }
        } else {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_STATUS_INVALID);
        }
        Integer oldStatus = status;
        order.setStatus(OrderStatusEnum.RENTING.code());
        order.setReceivedTime(LocalDateTime.now());
        orderMapper.updateById(order);
        writeHistory(orderId, oldStatus, OrderStatusEnum.RENTING.code(), renterId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void returnOrder(Long orderId, Long renterId, ReturnOrderRequest request) {
        RentalOrder order = requireOrder(orderId);
        requireRenter(order, renterId);
        if (order.getStatus() == null || order.getStatus() != OrderStatusEnum.RENTING.code()) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_STATUS_INVALID);
        }
        Integer oldStatus = order.getStatus();
        if (request != null) {
            order.setReturnCompany(request.getReturnCompany());
            order.setReturnTrackingNo(request.getReturnTrackingNo());
        }
        order.setStatus(OrderStatusEnum.PENDING_RETURN_CONFIRM.code());
        orderMapper.updateById(order);
        writeHistory(orderId, oldStatus, OrderStatusEnum.PENDING_RETURN_CONFIRM.code(), renterId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void complete(Long orderId, Long ownerId) {
        RentalOrder order = requireOrder(orderId);
        requireOwner(order, ownerId);
        if (order.getStatus() == null
                || order.getStatus() != OrderStatusEnum.PENDING_RETURN_CONFIRM.code()) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_STATUS_INVALID);
        }
        Integer oldStatus = order.getStatus();
        order.setStatus(OrderStatusEnum.COMPLETED.code());
        order.setCompletedTime(LocalDateTime.now());
        orderMapper.updateById(order);
        timeLockService.releaseLocks(orderId);
        releaseItemStock(order.getItemId(), order.getQuantity());
        CompleteSettlementFeignRequest request = new CompleteSettlementFeignRequest(
                order.getRenterId(), order.getOwnerId(), orderId,
                order.getRentAmount(), order.getDepositAmount(),
                order.getFrozenDepositAmount(), order.getPaidRentAmount(),
                order.getRentEndTime(), order.getDailyPrice());
        ApiResponse<SettlementResponse> resp = ResilientRemoteCallService.callWallet(
                () -> walletClient.completeSettlement(orderId, request));
        if (resp != null && resp.data() != null) {
            SettlementResponse settlement = resp.data();
            if (settlement.getOverdueFeeAmount() != null) {
                order.setOverdueFeeAmount(settlement.getOverdueFeeAmount());
            }
            order.setOverdueSettled(1);
            orderMapper.updateById(order);
        }
        writeHistory(orderId, oldStatus, OrderStatusEnum.COMPLETED.code(), ownerId);
        adjustCreditSafely(order.getRenterId(), orderId, 1, "ORDER_COMPLETED", "完成订单");
        adjustCreditSafely(order.getOwnerId(), orderId, 1, "ORDER_COMPLETED", "完成订单");
        BigDecimal overdueFee = order.getOverdueFeeAmount() == null ? BigDecimal.ZERO : order.getOverdueFeeAmount();
        if (overdueFee.compareTo(BigDecimal.ZERO) > 0) {
            adjustCreditSafely(order.getRenterId(), orderId, -5, "OVERDUE_RETURN", "超时归还");
        }
        // Send review reminder to both parties
        try {
            messageClient.sendSystemNotification(new SystemNotificationRequest(
                    order.getRenterId(), "订单已完成，请评价对方", 4));
            messageClient.sendSystemNotification(new SystemNotificationRequest(
                    order.getOwnerId(), "订单已完成，请评价对方", 4));
        } catch (Exception e) {
            // Notification failure should not block order completion
        }
    }

    private void adjustCreditSafely(Long userId, Long orderId, int changeValue, String reasonType, String reason) {
        try {
            authClient.adjustCreditScore(userId,
                    new CreditScoreAdjustFeignRequest(orderId, changeValue, reasonType, reason));
        } catch (Exception ignored) {
            // 信用分调整失败不阻断订单主状态流转，后续可通过信用分记录补偿。
        }
    }

    private void reserveItemStock(Long itemId, Integer quantity) {
        ApiResponse<Void> resp = ResilientRemoteCallService.callItem(
                () -> itemClient.reserveRentedCount(itemId, quantity));
        if (resp == null) {
            throw new BusinessException(ErrorCode.REMOTE_CALL_FAILED);
        }
    }

    private void releaseItemStock(Long itemId, Integer quantity) {
        ApiResponse<Void> resp = ResilientRemoteCallService.callItem(
                () -> itemClient.releaseRentedCount(itemId, quantity));
        if (resp == null) {
            throw new BusinessException(ErrorCode.REMOTE_CALL_FAILED);
        }
    }

    private RentalOrder requireOrder(Long orderId) {
        RentalOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_NOT_FOUND);
        }
        return order;
    }

    private void requireParticipant(RentalOrder order, Long userId) {
        if (!userId.equals(order.getOwnerId()) && !userId.equals(order.getRenterId())) {
            throw new BusinessException(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED);
        }
    }

    private void requireOwner(RentalOrder order, Long userId) {
        if (!userId.equals(order.getOwnerId())) {
            throw new BusinessException(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED);
        }
    }

    private void requireRenter(RentalOrder order, Long userId) {
        if (!userId.equals(order.getRenterId())) {
            throw new BusinessException(ErrorCode.RENTAL_APPLICATION_PERMISSION_DENIED);
        }
    }

    private void writeHistory(Long orderId, Integer oldStatus, Integer newStatus, Long operatorId) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setOperatorId(operatorId);
        historyMapper.insert(history);
    }

    private RentalOrderResponse toResponse(RentalOrder order) {
        return toResponse(order, false);
    }

    private RentalOrderResponse toResponse(RentalOrder order, boolean includeSnapshot) {
        RentalOrderResponse resp = new RentalOrderResponse();
        resp.setId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setApplicationId(order.getApplicationId());
        resp.setProposalId(order.getProposalId());
        resp.setItemId(order.getItemId());
        resp.setItemSnapshotId(order.getItemSnapshotId());
        resp.setOwnerId(order.getOwnerId());
        resp.setRenterId(order.getRenterId());
        resp.setQuantity(order.getQuantity());
        resp.setDeliveryType(order.getDeliveryType());
        resp.setRentStartTime(order.getRentStartTime());
        resp.setRentEndTime(order.getRentEndTime());
        resp.setDailyPrice(order.getDailyPrice());
        resp.setRentAmount(order.getRentAmount());
        resp.setDepositAmount(order.getDepositAmount());
        resp.setPaidRentAmount(order.getPaidRentAmount());
        resp.setFrozenDepositAmount(order.getFrozenDepositAmount());
        resp.setStatus(order.getStatus());
        resp.setShipCompany(order.getShipCompany());
        resp.setShipTrackingNo(order.getShipTrackingNo());
        resp.setReturnCompany(order.getReturnCompany());
        resp.setReturnTrackingNo(order.getReturnTrackingNo());
        resp.setReceivedTime(order.getReceivedTime());
        resp.setReturnedTime(order.getReturnedTime());
        resp.setCompletedTime(order.getCompletedTime());
        resp.setOverdueMinutes(order.getOverdueMinutes());
        resp.setOverdueFeeAmount(order.getOverdueFeeAmount());
        resp.setOverdueSettled(order.getOverdueSettled());
        resp.setCancelReason(order.getCancelReason());
        resp.setCreateTime(order.getCreateTime());
        if (includeSnapshot) {
            enrichSnapshot(resp, order.getItemSnapshotId());
        }
        return resp;
    }

    private void enrichSnapshot(RentalOrderResponse resp, Long snapshotId) {
        if (snapshotId == null) {
            return;
        }
        try {
            ApiResponse<ItemSnapshotFeignResponse> snapshotResp = ResilientRemoteCallService.callItem(
                    () -> itemClient.getSnapshot(snapshotId));
            if (snapshotResp == null || snapshotResp.data() == null) {
                return;
            }
            ItemSnapshotFeignResponse snapshot = snapshotResp.data();
            resp.setItemSnapshotTitle(snapshot.getTitle());
            resp.setItemSnapshotDescription(snapshot.getDescription());
            resp.setItemSnapshotCategoryName(snapshot.getCategoryName());
            resp.setItemSnapshotImageUrls(snapshot.getImageUrls());
        } catch (RuntimeException ignored) {
            // 快照增强失败不影响订单主流程，前端仍可展示订单基础字段。
        }
    }

    private ItemInfoFeignResponse requireItem(Long itemId) {
        ApiResponse<ItemInfoFeignResponse> resp = ResilientRemoteCallService.callItem(
                () -> itemClient.getItemInfo(itemId));
        if (resp == null || resp.data() == null) {
            throw new BusinessException(ErrorCode.RENTAL_ITEM_UNAVAILABLE);
        }
        return resp.data();
    }

    private Long requireSnapshot(Long itemId) {
        ApiResponse<Long> resp = ResilientRemoteCallService.callItem(
                () -> itemClient.createSnapshot(itemId));
        if (resp == null || resp.data() == null) {
            throw new BusinessException(ErrorCode.REMOTE_CALL_FAILED);
        }
        return resp.data();
    }
}
