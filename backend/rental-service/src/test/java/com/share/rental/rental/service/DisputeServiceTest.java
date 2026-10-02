package com.share.rental.rental.service;

import com.share.rental.common.exception.BusinessException;
import com.share.rental.rental.dto.DisputeCreateRequest;
import com.share.rental.rental.dto.ResolveDisputeFeignRequest;
import com.share.rental.rental.entity.Dispute;
import com.share.rental.rental.entity.RentalOrder;
import com.share.rental.rental.enums.DisputeStatusEnum;
import com.share.rental.rental.enums.OrderStatusEnum;
import com.share.rental.rental.mapper.DisputeMapper;
import com.share.rental.rental.mapper.RentalOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock RentalOrderMapper orderMapper;
    @Mock DisputeMapper disputeMapper;
    @InjectMocks DisputeService disputeService;

    @Test
    void createDispute_validOrder_insertsDispute() {
        RentalOrder order = new RentalOrder();
        order.setId(1L);
        order.setRenterId(100L);
        order.setOwnerId(200L);
        order.setStatus(OrderStatusEnum.COMPLETED.code());
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(disputeMapper.selectOne(any())).thenReturn(null);

        DisputeCreateRequest request = new DisputeCreateRequest();
        request.setReason("物品损坏");
        request.setDescription("屏幕有划痕");
        request.setExpectedDepositDeduction(new BigDecimal("50.00"));

        disputeService.createDispute(1L, 100L, request);

        ArgumentCaptor<Dispute> captor = ArgumentCaptor.forClass(Dispute.class);
        verify(disputeMapper).insert(captor.capture());
        Dispute dispute = captor.getValue();
        assertThat(dispute.getOrderId()).isEqualTo(1L);
        assertThat(dispute.getApplicantId()).isEqualTo(100L);
        assertThat(dispute.getReason()).isEqualTo("物品损坏");
        assertThat(dispute.getStatus()).isEqualTo(DisputeStatusEnum.PENDING.code());
    }

    @Test
    void createDispute_alreadyExists_throws() {
        RentalOrder order = new RentalOrder();
        order.setStatus(OrderStatusEnum.COMPLETED.code());
        order.setRenterId(100L);
        order.setOwnerId(200L);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(disputeMapper.selectOne(any())).thenReturn(new Dispute());

        DisputeCreateRequest request = new DisputeCreateRequest();
        request.setReason("test");
        assertThatThrownBy(() -> disputeService.createDispute(1L, 100L, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createDispute_invalidOrderStatus_throws() {
        RentalOrder order = new RentalOrder();
        order.setStatus(OrderStatusEnum.PENDING_PAYMENT.code());
        order.setRenterId(100L);
        order.setOwnerId(200L);
        when(orderMapper.selectById(1L)).thenReturn(order);

        DisputeCreateRequest request = new DisputeCreateRequest();
        request.setReason("test");

        assertThatThrownBy(() -> disputeService.createDispute(1L, 100L, request))
                .isInstanceOf(BusinessException.class);

        verify(disputeMapper, never()).insert(any(Dispute.class));
    }

    @Test
    void createDispute_rentingOrder_insertsDispute() {
        RentalOrder order = new RentalOrder();
        order.setId(1L);
        order.setRenterId(100L);
        order.setOwnerId(200L);
        order.setStatus(OrderStatusEnum.RENTING.code());
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(disputeMapper.selectOne(any())).thenReturn(null);

        DisputeCreateRequest request = new DisputeCreateRequest();
        request.setReason("物品损坏");

        disputeService.createDispute(1L, 100L, request);

        verify(disputeMapper).insert(any(Dispute.class));
    }

    @Test
    void listDisputes_returnsAllForOrder() {
        RentalOrder order = new RentalOrder();
        order.setId(1L);
        order.setRenterId(100L);
        order.setOwnerId(200L);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(disputeMapper.selectList(any())).thenReturn(Arrays.asList(new Dispute()));

        var result = disputeService.listDisputes(1L, 100L);
        assertThat(result).hasSize(1);
    }

    @Test
    void listAllDisputes_returnsAll() {
        when(disputeMapper.selectList(any())).thenReturn(Arrays.asList(new Dispute()));
        var result = disputeService.listAllDisputes(null);
        assertThat(result).hasSize(1);
    }

    @Test
    void resolveDispute_updatesStatusAndAdminFields() {
        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setStatus(DisputeStatusEnum.PENDING.code());
        when(disputeMapper.selectById(1L)).thenReturn(dispute);

        disputeService.resolveDispute(1L, new ResolveDisputeFeignRequest(999L, "裁定扣除50"));

        assertThat(dispute.getStatus()).isEqualTo(DisputeStatusEnum.RESOLVED.code());
        assertThat(dispute.getAdminId()).isEqualTo(999L);
        assertThat(dispute.getAdminRemark()).isEqualTo("裁定扣除50");
        verify(disputeMapper).updateById(dispute);
    }
}
