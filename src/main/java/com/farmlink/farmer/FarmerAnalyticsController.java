package com.farmlink.farmer;

import com.farmlink.common.ApiResponse;
import com.farmlink.order.OrderRepository;
import com.farmlink.order.OrderItemRepository;
import com.farmlink.order.OrderStatus;
import com.farmlink.review.ReviewDto;
import com.farmlink.review.ReviewRepository;
import com.farmlink.security.AuthUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Data behind the farmer dashboard's analytics card. */
@RestController
@RequestMapping("/api/farmers/me")
@RequiredArgsConstructor
@Tag(name = "Farmer analytics")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('FARMER')")
public class FarmerAnalyticsController {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final ReviewRepository reviews;

    public record TopProduct(UUID productId, String name, BigDecimal quantitySold) {}

    public record FarmerAnalytics(long totalOrders, long pendingOrders, BigDecimal revenue, double averageRating,
                                  long reviewCount, List<TopProduct> topProducts, List<ReviewDto> recentReviews) {}

    @GetMapping("/analytics")
    @Transactional(readOnly = true)
    public ApiResponse<FarmerAnalytics> analytics(@AuthenticationPrincipal AuthUser me) {
        UUID id = me.id();
        Double avg = reviews.averageRatingForFarmer(id);
        List<TopProduct> top = orderItems.topProductsForFarmer(id, PageRequest.of(0, 5)).stream()
                .map(r -> new TopProduct((UUID) r[0], (String) r[1], (BigDecimal) r[2]))
                .toList();
        List<ReviewDto> recent = reviews.findTop5ByFarmerIdOrderByCreatedAtDesc(id).stream()
                .map(ReviewDto::from).toList();
        return ApiResponse.ok(new FarmerAnalytics(
                orders.countByFarmerId(id),
                orders.countByFarmerIdAndStatus(id, OrderStatus.PENDING),
                orders.revenueForFarmer(id),
                avg == null ? 0 : Math.round(avg * 10) / 10.0,
                reviews.countByFarmerId(id),
                top, recent));
    }
}
