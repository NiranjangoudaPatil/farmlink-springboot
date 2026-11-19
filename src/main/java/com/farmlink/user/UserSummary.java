package com.farmlink.user;

import java.util.UUID;

/** Small public view of a user, embedded in products, orders and reviews. */
public record UserSummary(UUID id, String fullName, String location, boolean verified) {

    public static UserSummary from(User u) {
        return new UserSummary(u.getId(), u.getFullName(), u.getLocation(), u.isVerified());
    }
}
