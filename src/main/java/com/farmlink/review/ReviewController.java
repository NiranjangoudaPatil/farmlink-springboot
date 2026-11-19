package com.farmlink.review;

import com.farmlink.common.ApiResponse;
import com.farmlink.common.PageResponse;
import com.farmlink.security.AuthUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reviews")
public class ReviewController {

    private final ReviewService service;

    @PostMapping("/api/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RETAILER')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<ReviewDto> create(@AuthenticationPrincipal AuthUser me,
                                         @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.ok(service.create(me.id(), request));
    }

    @GetMapping("/api/farmers/{farmerId}/reviews")
    public ApiResponse<PageResponse<ReviewDto>> forFarmer(
            @PathVariable UUID farmerId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(service.forFarmer(farmerId, pageable));
    }

    @GetMapping("/api/farmers/{farmerId}/rating")
    public ApiResponse<RatingSummary> rating(@PathVariable UUID farmerId) {
        return ApiResponse.ok(service.rating(farmerId));
    }
}
