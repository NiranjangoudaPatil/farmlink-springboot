package com.farmlink.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public final class PaymentDtos {

    private PaymentDtos() {}

    public record CreatePaymentRequest(@NotEmpty List<UUID> orderIds) {}

    /** Everything the Razorpay checkout widget needs on the client. */
    public record CreatePaymentResponse(String razorpayKeyId, String razorpayOrderId, long amount, String currency) {}

    public record VerifyPaymentRequest(@NotBlank String razorpayOrderId, @NotBlank String razorpayPaymentId,
                                       @NotBlank String razorpaySignature) {}
}
