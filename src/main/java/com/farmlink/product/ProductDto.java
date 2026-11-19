package com.farmlink.product;

import com.farmlink.user.UserSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductDto(
        UUID id,
        UserSummary farmer,
        String name,
        String description,
        ProductCategory category,
        BigDecimal pricePerUnit,
        String unit,
        BigDecimal quantityAvailable,
        String imageUrl,
        boolean available,
        String location,
        int shelfLifeHours,
        Double latitude,
        Double longitude,
        Instant createdAt) {

    public static ProductDto from(Product p) {
        return new ProductDto(p.getId(), UserSummary.from(p.getFarmer()), p.getName(), p.getDescription(),
                p.getCategory(), p.getPricePerUnit(), p.getUnit(), p.getQuantityAvailable(), p.getImageUrl(),
                p.isAvailable(), p.getLocation(), p.getShelfLifeHours(), p.getLatitude(), p.getLongitude(),
                p.getCreatedAt());
    }
}
