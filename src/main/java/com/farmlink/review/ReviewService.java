package com.farmlink.review;

import com.farmlink.common.ApiException;
import com.farmlink.common.PageResponse;
import com.farmlink.notification.NotificationService;
import com.farmlink.order.Order;
import com.farmlink.order.OrderRepository;
import com.farmlink.order.OrderStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviews;
    private final OrderRepository orders;
    private final NotificationService notifications;

    @Transactional
    public ReviewDto create(UUID retailerId, ReviewRequest r) {
        Order order = orders.findDetailedById(r.orderId()).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!order.getRetailer().getId().equals(retailerId)) {
            throw ApiException.forbidden("You can only review your own orders");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw ApiException.badRequest("You can review an order only after it is delivered");
        }
        if (reviews.existsByOrderIdAndReviewerId(order.getId(), retailerId)) {
            throw ApiException.conflict("You already reviewed this order");
        }
        Review review = new Review();
        review.setOrder(order);
        review.setReviewer(order.getRetailer());
        review.setFarmer(order.getFarmer());
        review.setRating(r.rating());
        review.setComment(r.comment());
        reviews.save(review);

        notifications.notify(order.getFarmer(), "New Review",
                order.getRetailer().getFullName() + " rated you " + r.rating() + "/5", "REVIEW", order.getId());
        return ReviewDto.from(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> forFarmer(UUID farmerId, Pageable pageable) {
        return PageResponse.of(reviews.findByFarmerId(farmerId, pageable).map(ReviewDto::from));
    }

    @Transactional(readOnly = true)
    public RatingSummary rating(UUID farmerId) {
        Double avg = reviews.averageRatingForFarmer(farmerId);
        return new RatingSummary(avg == null ? 0 : Math.round(avg * 10) / 10.0, reviews.countByFarmerId(farmerId));
    }
}
