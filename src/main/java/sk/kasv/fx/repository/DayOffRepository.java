package sk.kasv.fx.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sk.kasv.fx.entity.DayOff;
import java.time.LocalDate;
import java.util.List;

public interface DayOffRepository extends JpaRepository<DayOff, Long> {
    boolean existsByProviderIdAndDay(Long providerId, LocalDate day);
    List<DayOff> findByProviderIdAndDayGreaterThanEqualOrderByDayAsc(Long providerId, LocalDate from);
    @Modifying
    @Query("delete from DayOff d where d.provider.id = :providerId and d.day = :day")
    int deleteDay(@Param("providerId") Long providerId, @Param("day") LocalDate day);
}
