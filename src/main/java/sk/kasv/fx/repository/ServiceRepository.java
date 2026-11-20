package sk.kasv.fx.repository;

import sk.kasv.fx.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface ServiceRepository extends JpaRepository<Service, Long>, JpaSpecificationExecutor<Service> {
    Page<Service> findByProviderId(Long providerId, Pageable pageable);
    Page<Service> findByProviderIdAndDeletedFalse(Long providerId, Pageable pageable);
    boolean existsByCategoryId(Long categoryId);
    java.util.List<Service> findAllByProviderIdAndDeletedFalseOrderByIdAsc(Long providerId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update Service s set s.deleted = true, s.active = false where s.provider.id = :providerId")
    int deleteAllForProvider(@org.springframework.data.repository.query.Param("providerId") Long providerId);
}
