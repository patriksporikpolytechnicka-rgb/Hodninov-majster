package sk.kasv.fx.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import sk.kasv.fx.service.CatalogService;
import java.util.List;
import static sk.kasv.fx.dto.ApiDtos.*;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CatalogService catalog;
    public CategoryController(CatalogService catalog) { this.catalog = catalog; }
    @GetMapping public List<CategoryResponse> list() { return catalog.categories(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) { return catalog.createCategory(request); }
}
