package com.farmlink.review;

import com.farmlink.user.UserSummary;
import java.time.Instant;
import java.util.UUID;

public record ReviewDto(UUID id, UUID orderId, UserSummary reviewer, UserSummary farmer, int rating, String comment,
                        Instant createdAt) {

    public static ReviewDto from(Review r) {
        return new ReviewDto(r.getId(), r.getOrder().getId(), UserSummary.from(r.getReviewer()),
                UserSummary.from(r.getFarmer()), r.getRating(), r.getComment(), r.getCreatedAt());
    }
}
