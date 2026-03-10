package com.tradeops.model.response;

import com.tradeops.model.entity.DeliveryStatus;

import java.time.LocalDateTime;

public record DeliveryAssignmentResponse(
        Long id,
        OrderResponse order,
        CourierUserResponse courier,
        LocalDateTime acceptedAt,
        DeliveryStatus status

) {}
