package sk.kasv.fx.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import sk.kasv.fx.service.CatalogService;
import java.util.List;
import static sk.kasv.fx.dto.ApiDtos.*;

@RestController
@Validated
@RequestMapping("/api/categories")
public class CategoryController {
    private final CatalogService catalog;
    public CategoryController(CatalogService catalog) { this.catalog = catalog; }
    @GetMapping public List<CategoryResponse> list() { return catalog.categories(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) { return catalog.createCategory(request); }
    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable @Positive Long id, @Valid @RequestBody CategoryRequest request) {
        return catalog.updateCategory(id, request);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long id) { catalog.deleteCategory(id); }
}
