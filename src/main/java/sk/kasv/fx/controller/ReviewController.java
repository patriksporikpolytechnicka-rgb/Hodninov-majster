package sk.kasv.fx.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import sk.kasv.fx.service.ReviewService;
import static sk.kasv.fx.dto.ApiDtos.*;

@RestController
@Validated
public class ReviewController {
    private final ReviewService reviews;
    public ReviewController(ReviewService reviews) { this.reviews = reviews; }

    @PostMapping("/api/orders/{id}/review") @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@PathVariable @Positive Long id, @Valid @RequestBody ReviewRequest request) {
        return reviews.create(id, request);
    }
    @GetMapping("/api/orders/{id}/review")
    public ReviewResponse forOrder(@PathVariable @Positive Long id) { return reviews.forOrder(id); }

    @GetMapping("/api/providers/{id}/reviews")
    public PageResponse<ReviewResponse> forProvider(@PathVariable @Positive Long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return reviews.forProvider(id, page, size);
    }
    @GetMapping("/api/providers/{id}/rating")
    public RatingResponse rating(@PathVariable @Positive Long id) { return reviews.rating(id); }
}
