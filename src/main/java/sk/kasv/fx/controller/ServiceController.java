package sk.kasv.fx.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import sk.kasv.fx.service.CatalogService;
import static sk.kasv.fx.dto.ApiDtos.*;

@RestController
@Validated
@RequestMapping("/api/services")
public class ServiceController {
    private final CatalogService catalog;
    public ServiceController(CatalogService catalog) { this.catalog = catalog; }
    @GetMapping
    public PageResponse<ServiceResponse> list(@RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false) @Positive Long providerId,
            @RequestParam(required = false) @Size(max = 150) String q,
            @RequestParam(required = false) @Size(max = 150) String location,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return catalog.services(categoryId, providerId, q, location, page, size);
    }
    @GetMapping("/mine")
    public PageResponse<ServiceResponse> mine(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) { return catalog.myServices(page, size); }
    @GetMapping("/{id}") public ServiceResponse get(@PathVariable @Positive Long id) { return catalog.service(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse create(@Valid @RequestBody ServiceRequest request) { return catalog.createService(request); }
    @PutMapping("/{id}")
    public ServiceResponse update(@PathVariable @Positive Long id, @Valid @RequestBody ServiceRequest request) {
        return catalog.updateService(id, request);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long id) { catalog.deleteService(id); }
}
