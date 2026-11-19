package com.farmlink.order;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @EntityGraph(attributePaths = {"items", "retailer", "farmer"})
    Optional<Order> findDetailedById(UUID id);

    @EntityGraph(attributePaths = {"items", "retailer", "farmer"})
    List<Order> findByRetailerIdOrderByCreatedAtDesc(UUID retailerId);

    @EntityGraph(attributePaths = {"items", "retailer", "farmer"})
    List<Order> findByFarmerIdOrderByCreatedAtDesc(UUID farmerId);

    @EntityGraph(attributePaths = {"items", "retailer", "farmer"})
    List<Order> findByRazorpayOrderId(String razorpayOrderId);

    @EntityGraph(attributePaths = {"items", "retailer", "farmer"})
    List<Order> findByIdInAndRetailerId(Collection<UUID> ids, UUID retailerId);

    /** Paged: only join the single-valued parties (fetching the items collection would page in memory). */
    @Override
    @EntityGraph(attributePaths = {"retailer", "farmer"})
    Page<Order> findAll(Pageable pageable);

    long countByFarmerId(UUID farmerId);

    long countByFarmerIdAndStatus(UUID farmerId, OrderStatus status);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.farmer.id = :farmerId and o.status <> com.farmlink.order.OrderStatus.CANCELLED")
    BigDecimal revenueForFarmer(@Param("farmerId") UUID farmerId);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.status <> com.farmlink.order.OrderStatus.CANCELLED")
    BigDecimal totalRevenue();

    @Query("select o.status, count(o) from Order o group by o.status")
    List<Object[]> countByStatus();
}
