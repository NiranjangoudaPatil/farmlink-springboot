package com.farmlink.auth;

import com.farmlink.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 6, max = 72) String password,
        @NotBlank @Size(max = 150) String fullName,
        @NotNull Role role,                      // FARMER or RETAILER (ADMIN is rejected)
        @Size(max = 30) String phone,
        @Size(max = 255) String location,
        @Size(max = 100) String licenseNumber,   // farmers only
        Double latitude,
        Double longitude) {}
