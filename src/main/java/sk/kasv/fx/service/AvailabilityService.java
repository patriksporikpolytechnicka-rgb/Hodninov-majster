package sk.kasv.fx.service;

import org.springframework.transaction.annotation.Transactional;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.DayOffRepository;
import sk.kasv.fx.repository.ProviderProfileRepository;
import sk.kasv.fx.repository.WorkingHoursRepository;
import sk.kasv.fx.security.CurrentUser;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import static sk.kasv.fx.dto.ApiDtos.*;

/**
 * Working hours per weekday and days off. A handyman without any working hours can be booked
 * at any time (as before); once hours are set, orders must start inside them.
 */
@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class AvailabilityService {
    private final WorkingHoursRepository hours;
    private final DayOffRepository daysOff;
    private final ProviderProfileRepository providers;
    private final CurrentUser current;
    private final Clock clock;
    public AvailabilityService(WorkingHoursRepository hours, DayOffRepository daysOff, ProviderProfileRepository providers,
                               CurrentUser current, Clock clock) {
        this.hours = hours; this.daysOff = daysOff; this.providers = providers; this.current = current; this.clock = clock;
    }

    public AvailabilityResponse get(Long providerId) {
        if (!providers.existsById(providerId)) throw new ApiException(404, "Provider not found");
        return response(providerId);
    }

    @Transactional
    public AvailabilityResponse setWorkingHours(WorkingHoursRequest request) {
        ProviderProfile provider = myProvider();
        var days = new HashSet<Integer>();
        for (WorkingHoursEntry e : request.hours()) {
            if (!days.add(e.dayOfWeek())) throw new ApiException(400, "Each day can be listed only once");
            if (!e.start().isBefore(e.end())) throw new ApiException(400, "Start must be before end");
        }
        hours.deleteAllForProvider(provider.getId());
        for (WorkingHoursEntry e : request.hours())
            hours.save(new WorkingHours(provider, e.dayOfWeek(), e.start(), e.end()));
        hours.flush();
        return response(provider.getId());
    }

    @Transactional
    public AvailabilityResponse addDayOff(DayOffRequest request) {
        ProviderProfile provider = myProvider();
        if (request.date().isBefore(LocalDate.now(clock))) throw new ApiException(400, "Day off must be today or later");
        if (!daysOff.existsByProviderIdAndDay(provider.getId(), request.date()))
            daysOff.saveAndFlush(new DayOff(provider, request.date()));
        return response(provider.getId());
    }

    @Transactional
    public void removeDayOff(LocalDate day) { daysOff.deleteDay(myProvider().getId(), day); }

    /** Called when an order is created; scheduledAt is local time in Europe/Bratislava. */
    public void requireAvailable(ProviderProfile provider, LocalDateTime scheduledAt) {
        if (daysOff.existsByProviderIdAndDay(provider.getId(), scheduledAt.toLocalDate()))
            throw new ApiException(409, "The handyman is not available on that day");
        List<WorkingHours> week = hours.findByProviderIdOrderByDayOfWeekAsc(provider.getId());
        if (week.isEmpty()) return;
        int day = scheduledAt.getDayOfWeek().getValue();
        boolean inside = week.stream().anyMatch(h -> h.getDayOfWeek() == day
                && !scheduledAt.toLocalTime().isBefore(h.getStartTime()) && scheduledAt.toLocalTime().isBefore(h.getEndTime()));
        if (!inside) throw new ApiException(409, "Outside the handyman's working hours");
    }

    private AvailabilityResponse response(Long providerId) {
        return new AvailabilityResponse(
                hours.findByProviderIdOrderByDayOfWeekAsc(providerId).stream()
                        .map(h -> new WorkingHoursEntry(h.getDayOfWeek(), h.getStartTime(), h.getEndTime())).toList(),
                daysOff.findByProviderIdAndDayGreaterThanEqualOrderByDayAsc(providerId, LocalDate.now(clock)).stream()
                        .map(DayOff::getDay).toList());
    }
    private ProviderProfile myProvider() {
        User user = current.require();
        if (user.getRole() != Role.PROVIDER) throw new ApiException(403, "Role PROVIDER required");
        return providers.findByUserId(user.getId()).orElseThrow(() -> new ApiException(404, "Create your provider profile first"));
    }
}
