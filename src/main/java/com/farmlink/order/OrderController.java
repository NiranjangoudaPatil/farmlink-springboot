package com.farmlink.order;

import com.farmlink.common.ApiResponse;
import com.farmlink.security.AuthUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RETAILER')")
    public ApiResponse<List<OrderDto>> place(@AuthenticationPrincipal AuthUser me,
                                             @Valid @RequestBody PlaceOrderRequest request) {
        return ApiResponse.ok(service.place(me.id(), request));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('RETAILER')")
    public ApiResponse<List<OrderDto>> mine(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(service.forRetailer(me.id()));
    }

    @GetMapping("/incoming")
    @PreAuthorize("hasRole('FARMER')")
    public ApiResponse<List<OrderDto>> incoming(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(service.forFarmer(me.id()));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderDto> get(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return ApiResponse.ok(service.get(id, me));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('FARMER','RETAILER')")
    public ApiResponse<OrderDto> updateStatus(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                              @Valid @RequestBody UpdateStatusRequest request) {
        return ApiResponse.ok(service.updateStatus(id, me, request.status()));
    }
}
