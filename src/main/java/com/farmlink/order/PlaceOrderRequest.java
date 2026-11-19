package com.farmlink.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** A cart. Items from different farmers are split into one order per farmer. */
public record PlaceOrderRequest(
        @NotEmpty @Valid List<Line> items,
        @Size(max = 500) String deliveryAddress,
        Double deliveryLatitude,
        Double deliveryLongitude,
        @Size(max = 1000) String notes) {

    public record Line(@NotNull UUID productId, @NotNull @DecimalMin("0.01") BigDecimal quantity) {}
}
