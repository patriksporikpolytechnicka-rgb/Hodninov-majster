package sk.kasv.fx.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import sk.kasv.fx.service.AvailabilityService;
import java.time.LocalDate;
import static sk.kasv.fx.dto.ApiDtos.*;

@RestController
@Validated
@RequestMapping("/api/providers")
public class AvailabilityController {
    private final AvailabilityService availability;
    public AvailabilityController(AvailabilityService availability) { this.availability = availability; }

    @GetMapping("/{id}/availability")
    public AvailabilityResponse get(@PathVariable @Positive Long id) { return availability.get(id); }

    /** Replaces the whole week; an empty list removes the limits. */
    @PutMapping("/me/working-hours")
    public AvailabilityResponse setWorkingHours(@Valid @RequestBody WorkingHoursRequest request) {
        return availability.setWorkingHours(request);
    }
    @PostMapping("/me/days-off")
    public AvailabilityResponse addDayOff(@Valid @RequestBody DayOffRequest request) { return availability.addDayOff(request); }
    @DeleteMapping("/me/days-off/{date}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeDayOff(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        availability.removeDayOff(date);
    }
}
