package sk.kasv.fx.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sk.kasv.fx.entity.WorkingHours;
import java.util.List;

public interface WorkingHoursRepository extends JpaRepository<WorkingHours, Long> {
    List<WorkingHours> findByProviderIdOrderByDayOfWeekAsc(Long providerId);
    /** Bulk delete runs at once, so new rows for the same days can be inserted in the same transaction. */
    @Modifying
    @Query("delete from WorkingHours w where w.provider.id = :providerId")
    void deleteAllForProvider(@Param("providerId") Long providerId);
}
