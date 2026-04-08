package com.tradeops.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record EditProductRequest(
        @NotBlank(message = "Product name is required")
        String productName,
        @NotNull(message = "Product category is required")
        Long categoryId,
        @NotNull(message = "Product price is required")
        BigDecimal price
) {
}
