package com.farmlink.user;

import jakarta.validation.constraints.Size;

/** All fields optional: only non-null values are applied. */
public record UpdateProfileRequest(
        @Size(min = 1, max = 150) String fullName,
        @Size(max = 30) String phone,
        @Size(max = 255) String location,
        @Size(max = 500) String avatarUrl,
        @Size(max = 100) String licenseNumber,
        Double latitude,
        Double longitude) {}
