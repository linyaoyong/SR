package com.share.rental.rental;

import com.share.rental.rental.entity.RentalOrder;
import com.share.rental.rental.enums.DeliveryTypeEnum;
import com.share.rental.rental.enums.OrderStatusEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RentalEntityCompileTest {

    @Test
    void enumsExposeExpectedCodes() {
        assertThat(OrderStatusEnum.PENDING_PAYMENT.code()).isEqualTo(0);
        assertThat(OrderStatusEnum.COMPLETED.code()).isEqualTo(5);
        assertThat(OrderStatusEnum.CANCELLED.code()).isEqualTo(6);
        assertThat(DeliveryTypeEnum.MEETUP.code()).isEqualTo(0);
        assertThat(DeliveryTypeEnum.EXPRESS.code()).isEqualTo(1);
    }

    @Test
    void entitiesCanInstantiate() {
        RentalOrder order = new RentalOrder();
        order.setOrderNo("ORD20260627001");
        order.setStatus(OrderStatusEnum.PENDING_PAYMENT.code());
        order.setRentAmount(new BigDecimal("100.00"));
        assertThat(order.getOrderNo()).isEqualTo("ORD20260627001");
        assertThat(order.getStatus()).isEqualTo(0);
    }
}
