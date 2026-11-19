package com.farmlink.order;

import com.farmlink.user.UserSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDto(
        UUID id,
        OrderStatus status,
        PaymentStatus paymentStatus,
        BigDecimal totalAmount,
        String notes,
        String deliveryAddress,
        Double deliveryLatitude,
        Double deliveryLongitude,
        String razorpayPaymentId,
        UserSummary retailer,
        UserSummary farmer,
        List<Item> items,
        Instant createdAt,
        Instant updatedAt) {

    public record Item(UUID productId, String productName, BigDecimal quantity, BigDecimal pricePerUnit,
                       BigDecimal totalPrice) {}

    public static OrderDto from(Order o) {
        List<Item> items = o.getItems().stream()
                .map(i -> new Item(i.getProduct().getId(), i.getProductName(), i.getQuantity(), i.getPricePerUnit(),
                        i.getTotalPrice()))
                .toList();
        return new OrderDto(o.getId(), o.getStatus(), o.getPaymentStatus(), o.getTotalAmount(), o.getNotes(),
                o.getDeliveryAddress(), o.getDeliveryLatitude(), o.getDeliveryLongitude(), o.getRazorpayPaymentId(),
                UserSummary.from(o.getRetailer()), UserSummary.from(o.getFarmer()), items, o.getCreatedAt(),
                o.getUpdatedAt());
    }
}
