package com.farmlink.payment;

import com.farmlink.common.ApiResponse;
import com.farmlink.order.OrderDto;
import com.farmlink.payment.PaymentDtos.CreatePaymentRequest;
import com.farmlink.payment.PaymentDtos.CreatePaymentResponse;
import com.farmlink.payment.PaymentDtos.VerifyPaymentRequest;
import com.farmlink.security.AuthUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('RETAILER')")
public class PaymentController {

    private final PaymentService service;

    @PostMapping("/create-order")
    public ApiResponse<CreatePaymentResponse> create(@AuthenticationPrincipal AuthUser me,
                                                     @Valid @RequestBody CreatePaymentRequest request) {
        return ApiResponse.ok(service.createPayment(me.id(), request));
    }

    @PostMapping("/verify")
    public ApiResponse<List<OrderDto>> verify(@AuthenticationPrincipal AuthUser me,
                                              @Valid @RequestBody VerifyPaymentRequest request) {
        return ApiResponse.ok(service.verify(me.id(), request), "Payment verified");
    }
}
