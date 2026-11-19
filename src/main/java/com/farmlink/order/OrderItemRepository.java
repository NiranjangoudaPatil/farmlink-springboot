package com.farmlink.order;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    boolean existsByProductId(UUID productId);

    /** [productId, productName, totalQuantitySold] for a farmer's non-cancelled orders, best sellers first. */
    @Query("""
            select i.product.id, i.productName, sum(i.quantity)
            from OrderItem i
            where i.order.farmer.id = :farmerId and i.order.status <> com.farmlink.order.OrderStatus.CANCELLED
            group by i.product.id, i.productName
            order by sum(i.quantity) desc
            """)
    List<Object[]> topProductsForFarmer(@Param("farmerId") UUID farmerId, Pageable pageable);
}
