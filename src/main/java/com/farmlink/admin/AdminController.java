package com.farmlink.admin;

import com.farmlink.common.ApiException;
import com.farmlink.common.ApiResponse;
import com.farmlink.common.PageResponse;
import com.farmlink.order.OrderDto;
import com.farmlink.order.OrderRepository;
import com.farmlink.product.ProductDto;
import com.farmlink.product.ProductService;
import com.farmlink.product.ProductSpecs;
import com.farmlink.product.ProductRepository;
import com.farmlink.review.ReviewDto;
import com.farmlink.review.ReviewRepository;
import com.farmlink.user.Role;
import com.farmlink.user.User;
import com.farmlink.user.UserDto;
import com.farmlink.user.UserRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Replaces the React admin dashboard tabs: users, products, orders, reviews, analytics. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Sort NEWEST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final UserRepository users;
    private final ProductRepository products;
    private final ProductService productService;
    private final OrderRepository orders;
    private final ReviewRepository reviews;

    public record VerifyRequest(boolean verified) {}

    public record AvailabilityRequest(boolean available) {}

    public record Analytics(Map<String, Long> usersByRole, long products, long orders,
                            Map<String, Long> ordersByStatus, BigDecimal revenue, long reviews) {}

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<UserDto>> users(@RequestParam(required = false) Role role,
                                                    @PageableDefault(size = 20, sort = "createdAt",
                                                            direction = Sort.Direction.DESC) Pageable pageable) {
        var page = role == null ? users.findAll(pageable) : users.findByRole(role, pageable);
        return ApiResponse.ok(PageResponse.of(page.map(UserDto::from)));
    }

    /** Verify (or un-verify) a user, e.g. after checking a farmer's license. */
    @PatchMapping("/users/{id}/verification")
    @Transactional
    public ApiResponse<UserDto> verify(@PathVariable UUID id, @RequestBody VerifyRequest request) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        u.setVerified(request.verified());
        return ApiResponse.ok(UserDto.from(u));
    }

    @GetMapping("/products")
    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<ProductDto>> products(@PageableDefault(size = 20, sort = "createdAt",
            direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(PageResponse.of(
                products.findAll(ProductSpecs.filter(null, null, null, null), pageable).map(ProductDto::from)));
    }

    /** Hide or show a product in the marketplace (moderation). */
    @PatchMapping("/products/{id}/availability")
    public ApiResponse<ProductDto> availability(@PathVariable UUID id, @RequestBody AvailabilityRequest request) {
        return ApiResponse.ok(productService.setAvailability(id, request.available()));
    }

    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<OrderDto>> orders(@PageableDefault(size = 20, sort = "createdAt",
            direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(PageResponse.of(orders.findAll(pageable).map(OrderDto::from)));
    }

    @GetMapping("/reviews")
    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<ReviewDto>> reviews(@PageableDefault(size = 20, sort = "createdAt",
            direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(PageResponse.of(reviews.findAll(pageable).map(ReviewDto::from)));
    }

    @GetMapping("/analytics")
    @Transactional(readOnly = true)
    public ApiResponse<Analytics> analytics() {
        Map<String, Long> byRole = new LinkedHashMap<>();
        for (Role r : Role.values()) byRole.put(r.name(), 0L);
        users.countByRole().forEach(row -> byRole.put(((Role) row[0]).name(), (Long) row[1]));

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (var s : com.farmlink.order.OrderStatus.values()) byStatus.put(s.name(), 0L);
        orders.countByStatus().forEach(row -> byStatus.put(((com.farmlink.order.OrderStatus) row[0]).name(),
                (Long) row[1]));

        return ApiResponse.ok(new Analytics(byRole, products.count(), orders.count(), byStatus,
                orders.totalRevenue(), reviews.count()));
    }
}
