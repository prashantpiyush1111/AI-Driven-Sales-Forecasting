package com.salesforecast.dto.response;

import com.salesforecast.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        String role,
        LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getRole(), user.getCreatedAt());
    }
}
