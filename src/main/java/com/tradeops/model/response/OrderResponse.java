package com.tradeops.model.response;

import com.tradeops.model.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        String orderNumber,
        Long traderId,
        OrderStatus status,
        String deliveryAddress,
        LocalDateTime createdAt,
        BigDecimal totals,
        Long customerLinkId,
        List<Long> orderLinesId
) {
}
