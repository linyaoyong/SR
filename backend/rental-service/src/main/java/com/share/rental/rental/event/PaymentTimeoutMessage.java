package com.share.rental.rental.event;

import java.io.Serializable;

public record PaymentTimeoutMessage(String eventId, Long orderId) implements Serializable {
}
