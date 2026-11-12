package com.salesforecast.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Size(max = 100, message = "Username must be at most 100 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(max = 72, message = "Password must be at most 72 characters")
    private String password;
}