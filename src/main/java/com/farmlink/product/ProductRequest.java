package com.farmlink.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 5000) String description,
        ProductCategory category,
        @NotNull @DecimalMin("0.01") BigDecimal pricePerUnit,
        @Size(max = 20) String unit,
        @NotNull @DecimalMin("0.00") BigDecimal quantityAvailable,
        @Size(max = 500) String imageUrl,
        Boolean available,
        @Size(max = 255) String location,
        @Min(1) Integer shelfLifeHours,
        Double latitude,
        Double longitude) {}
