package com.tradeops.model.response;

import com.tradeops.model.entity.Role;

import java.time.LocalDateTime;

public record TraderUserResponse(
        Long id,
        TraderResponse trader,
        Role role,
        String name,
        String email,
        LocalDateTime lastLoginAt
) {}
