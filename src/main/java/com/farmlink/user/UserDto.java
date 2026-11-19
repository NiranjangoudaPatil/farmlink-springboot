package com.farmlink.user;

import java.time.Instant;
import java.util.UUID;

/** What the API returns for a user. Never includes the password hash. */
public record UserDto(
        UUID id,
        String email,
        String fullName,
        Role role,
        String phone,
        String location,
        String avatarUrl,
        String licenseNumber,
        boolean verified,
        Double latitude,
        Double longitude,
        Instant createdAt) {

    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getPhone(), u.getLocation(),
                u.getAvatarUrl(), u.getLicenseNumber(), u.isVerified(), u.getLatitude(), u.getLongitude(),
                u.getCreatedAt());
    }
}
