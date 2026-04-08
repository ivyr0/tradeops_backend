package com.tradeops.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRequest(
        @NotBlank(message = "Name is required")
        String name,
        @NotNull(message = "Parent id is required")
        Long parentId,
        @NotBlank(message = "Slug is required")
        String slug,
        Integer sortOrder
) {}
