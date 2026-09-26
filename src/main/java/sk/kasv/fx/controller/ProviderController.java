package sk.kasv.fx.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import sk.kasv.fx.service.CatalogService;
import static sk.kasv.fx.dto.ApiDtos.*;

@RestController
@Validated
@RequestMapping("/api/providers")
public class ProviderController {
    private final CatalogService catalog;
    public ProviderController(CatalogService catalog) { this.catalog = catalog; }
    @GetMapping("/{id}") public ProviderResponse get(@PathVariable @Positive Long id) { return catalog.provider(id); }
    @GetMapping("/me") public ProviderResponse me() { return catalog.myProfile(); }
    @PutMapping("/me") public ProviderResponse save(@Valid @RequestBody ProviderRequest request) { return catalog.saveProfile(request); }
}
