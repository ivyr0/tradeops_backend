package com.tradeops.model.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank(message = "Fullname is required")
        String fullName
) {
}
