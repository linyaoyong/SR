package com.share.rental.rental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.rental.dto.DisputeCreateRequest;
import com.share.rental.rental.dto.DisputeResponse;
import com.share.rental.rental.dto.ResolveDisputeFeignRequest;
import com.share.rental.rental.entity.Dispute;
import com.share.rental.rental.entity.RentalOrder;
import com.share.rental.rental.enums.DisputeStatusEnum;
import com.share.rental.rental.enums.OrderStatusEnum;
import com.share.rental.rental.mapper.DisputeMapper;
import com.share.rental.rental.mapper.RentalOrderMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DisputeService {

    private final RentalOrderMapper orderMapper;
    private final DisputeMapper disputeMapper;

    public DisputeService(RentalOrderMapper orderMapper, DisputeMapper disputeMapper) {
        this.orderMapper = orderMapper;
        this.disputeMapper = disputeMapper;
    }

    public DisputeResponse createDispute(Long orderId, Long applicantId, DisputeCreateRequest request) {
        RentalOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_NOT_FOUND);
        }
        if (!applicantId.equals(order.getRenterId()) && !applicantId.equals(order.getOwnerId())) {
            throw new BusinessException(ErrorCode.DISPUTE_PERMISSION_DENIED);
        }
        if (order.getStatus() == null
                || (order.getStatus() != OrderStatusEnum.RENTING.code()
                        && order.getStatus() != OrderStatusEnum.COMPLETED.code())) {
            throw new BusinessException(ErrorCode.DISPUTE_STATUS_INVALID);
        }

        // Check if dispute already exists
        Dispute existing = disputeMapper.selectOne(
                new LambdaQueryWrapper<Dispute>().eq(Dispute::getOrderId, orderId));
        if (existing != null) {
            throw new BusinessException(ErrorCode.DISPUTE_ALREADY_EXISTS);
        }

        Dispute dispute = new Dispute();
        dispute.setOrderId(orderId);
        dispute.setApplicantId(applicantId);
        dispute.setReason(request.getReason());
        dispute.setDescription(request.getDescription());
        dispute.setExpectedDepositDeduction(request.getExpectedDepositDeduction());
        dispute.setImageUrls(request.getImageUrls());
        dispute.setStatus(DisputeStatusEnum.PENDING.code());
        disputeMapper.insert(dispute);

        return toResponse(dispute);
    }

    public List<DisputeResponse> listDisputes(Long orderId, Long userId) {
        RentalOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_NOT_FOUND);
        }
        if (!userId.equals(order.getRenterId()) && !userId.equals(order.getOwnerId())) {
            throw new BusinessException(ErrorCode.DISPUTE_PERMISSION_DENIED);
        }
        List<Dispute> disputes = disputeMapper.selectList(
                new LambdaQueryWrapper<Dispute>().eq(Dispute::getOrderId, orderId));
        return disputes.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<DisputeResponse> listAllDisputes(Integer status) {
        LambdaQueryWrapper<Dispute> wrapper = new LambdaQueryWrapper<Dispute>().orderByDesc(Dispute::getCreateTime);
        if (status != null) {
            wrapper.eq(Dispute::getStatus, status);
        }
        List<Dispute> disputes = disputeMapper.selectList(wrapper);
        return disputes.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public void resolveDispute(Long disputeId, ResolveDisputeFeignRequest request) {
        Dispute dispute = disputeMapper.selectById(disputeId);
        if (dispute == null) {
            throw new BusinessException(ErrorCode.DISPUTE_NOT_FOUND);
        }
        if (dispute.getStatus() != DisputeStatusEnum.PENDING.code()
                && dispute.getStatus() != DisputeStatusEnum.PROCESSING.code()) {
            throw new BusinessException(ErrorCode.DISPUTE_STATUS_INVALID);
        }
        dispute.setStatus(DisputeStatusEnum.RESOLVED.code());
        dispute.setAdminId(request.getAdminId());
        dispute.setAdminRemark(request.getAdminRemark());
        disputeMapper.updateById(dispute);
    }

    private DisputeResponse toResponse(Dispute d) {
        DisputeResponse resp = new DisputeResponse();
        resp.setId(d.getId());
        resp.setOrderId(d.getOrderId());
        resp.setApplicantId(d.getApplicantId());
        resp.setReason(d.getReason());
        resp.setDescription(d.getDescription());
        resp.setExpectedDepositDeduction(d.getExpectedDepositDeduction());
        resp.setImageUrls(d.getImageUrls());
        resp.setStatus(d.getStatus());
        resp.setAdminId(d.getAdminId());
        resp.setAdminRemark(d.getAdminRemark());
        resp.setCreateTime(d.getCreateTime());
        resp.setUpdateTime(d.getUpdateTime());
        return resp;
    }
}
