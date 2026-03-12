package com.tradeops.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record OrdersRequest(
        @NotNull(message = "Trader id is required")
        @PositiveOrZero
        Long traderId
) {
}
