package sk.kasv.fx.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sk.kasv.fx.entity.Review;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByOrderId(Long orderId);
    Optional<Review> findByOrderId(Long orderId);
    Page<Review> findByProviderId(Long providerId, Pageable pageable);
    long countByProviderId(Long providerId);
    @Query("select avg(r.rating) from Review r where r.provider.id = :providerId")
    Double averageRating(@Param("providerId") Long providerId);
}
