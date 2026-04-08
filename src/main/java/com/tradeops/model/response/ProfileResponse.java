package com.tradeops.model.response;

import jakarta.validation.constraints.NotBlank;

public record ProfileResponse(
        Long id,
        String fullName,
        String email
) {}
