package com.tradeops.model.request;

import jakarta.validation.constraints.NotNull;

public record DeleteProductRequest(

        @NotNull(message = "Product id is required")
        Long id
) {}
