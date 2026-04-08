package com.tradeops.model.request;

import jakarta.validation.constraints.NotBlank;

public class EditCategoryRequest {
    @NotBlank(message = "Category name is required")
    public String name;
}
