package com.farmlink.review;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByOrderIdAndReviewerId(UUID orderId, UUID reviewerId);

    @EntityGraph(attributePaths = {"reviewer", "farmer"})
    Page<Review> findByFarmerId(UUID farmerId, Pageable pageable);

    @EntityGraph(attributePaths = {"reviewer", "farmer"})
    List<Review> findTop5ByFarmerIdOrderByCreatedAtDesc(UUID farmerId);

    @Override
    @EntityGraph(attributePaths = {"reviewer", "farmer"})
    Page<Review> findAll(Pageable pageable);

    long countByFarmerId(UUID farmerId);

    @Query("select avg(r.rating) from Review r where r.farmer.id = :farmerId")
    Double averageRatingForFarmer(@Param("farmerId") UUID farmerId);
}
