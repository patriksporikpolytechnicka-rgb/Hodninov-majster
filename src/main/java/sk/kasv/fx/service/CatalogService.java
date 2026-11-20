package sk.kasv.fx.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import sk.kasv.fx.dto.ApiMapper;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.*;
import sk.kasv.fx.security.CurrentUser;
import sk.kasv.fx.security.Verified;
import java.util.List;
import java.util.Locale;
import static sk.kasv.fx.dto.ApiDtos.*;

@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class CatalogService {
    private final CategoryRepository categories;
    private final ServiceRepository services;
    private final ProviderProfileRepository providers;
    private final OrderRepository orders;
    private final CurrentUser current;
    @org.springframework.beans.factory.annotation.Autowired
    private java.time.Clock clock = java.time.Clock.systemUTC();
    public CatalogService(CategoryRepository categories, ServiceRepository services,
                          ProviderProfileRepository providers, OrderRepository orders, CurrentUser current) {
        this.categories = categories; this.services = services; this.providers = providers; this.orders = orders; this.current = current;
    }
    public List<CategoryResponse> categories() {
        return categories.findAll(Sort.by("name")).stream().map(ApiMapper::category).toList();
    }
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        requireRole(Role.ADMIN);
        String name = request.name().strip();
        if (categories.existsByNameIgnoreCase(name)) throw new ApiException(409, "Category already exists");
        Category category = new Category(); category.setName(name);
        return ApiMapper.category(categories.saveAndFlush(category));
    }
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        requireRole(Role.ADMIN);
        Category category = categories.findById(id).orElseThrow(() -> new ApiException(404, "Category not found"));
        String name = request.name().strip();
        if (categories.existsByNameIgnoreCaseAndIdNot(name, id)) throw new ApiException(409, "Category already exists");
        category.setName(name);
        return ApiMapper.category(categories.saveAndFlush(category));
    }
    /** A category can be deleted only when no service (not even a deleted one) uses it. */
    @Transactional
    public void deleteCategory(Long id) {
        requireRole(Role.ADMIN);
        Category category = categories.findById(id).orElseThrow(() -> new ApiException(404, "Category not found"));
        if (services.existsByCategoryId(id)) throw new ApiException(409, "Category is used by services; move them first");
        categories.delete(category);
    }
    public ProviderResponse provider(Long id) {
        ProviderProfile profile = findProvider(id);
        if (profile.getUser().isBlocked()) throw new ApiException(404, "Provider not found");
        return ApiMapper.provider(profile);
    }
    public ProviderResponse myProfile() { return ApiMapper.provider(myProvider()); }
    @Transactional
    public ProviderResponse saveProfile(ProviderRequest request) {
        User user = Verified.require(requireRole(Role.PROVIDER));
        ProviderProfile profile = providers.findByUserId(user.getId()).orElseGet(ProviderProfile::new);
        profile.setUser(user); profile.setName(request.name().strip());
        profile.setDescription(request.description().strip()); profile.setLocation(request.location().strip());
        // profileImage is no longer taken from the request; it is set by the photo upload (PhotoService).
        profile.setPhone(request.phone().strip());
        profile.setAvailability(request.availability()); profile.startTrial(clock.instant());
        return ApiMapper.provider(providers.saveAndFlush(profile));
    }
    /** sort: null (oldest first), newest, price_asc, price_desc or rating (best rated handymen first). */
    public PageResponse<ServiceResponse> services(Long categoryId, Long providerId, String q, String location,
                                                  String sort, int page, int size) {
        Specification<Service> spec = (root, query, cb) -> cb.and(cb.isTrue(root.get("active")),
                cb.isFalse(root.get("provider").get("user").get("blocked")));
        if (categoryId != null) spec = spec.and((r, query, cb) -> cb.equal(r.get("category").get("id"), categoryId));
        if (providerId != null) spec = spec.and((r, query, cb) -> cb.equal(r.get("provider").get("id"), providerId));
        if (q != null && !q.isBlank()) spec = spec.and((r, query, cb) -> cb.like(cb.lower(r.get("name")), pattern(q), '!'));
        if (location != null && !location.isBlank()) spec = spec.and((r, query, cb) -> cb.like(cb.lower(r.get("provider").get("location")), pattern(location), '!'));
        return PageResponse.from(services.findAll(spec, PageRequest.of(page, size, sortBy(sort))).map(ApiMapper::service));
    }
    static Sort sortBy(String sort) {
        Sort byId = Sort.by("id");
        if (sort == null) return byId;
        return switch (sort) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "id");
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price").and(byId);
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price").and(byId);
            case "rating" -> Sort.by(Sort.Direction.DESC, "provider.ratingAverage", "provider.ratingCount").and(byId);
            default -> throw new ApiException(400, "Unknown sort");
        };
    }
    public PageResponse<ServiceResponse> myServices(int page, int size) {
        return PageResponse.from(services.findByProviderIdAndDeletedFalse(myProvider().getId(),
                PageRequest.of(page, size, Sort.by("id"))).map(ApiMapper::service));
    }
    public ServiceResponse service(Long id) {
        Service service = findService(id);
        if (!service.isActive() || service.getProvider().getUser().isBlocked()) throw new ApiException(404, "Service not found");
        return ApiMapper.service(service);
    }
    @Transactional
    public ServiceResponse createService(ServiceRequest request) {
        Verified.require(current.require());
        Service service = new Service(); service.setProvider(myProvider());
        apply(service, request);
        return ApiMapper.service(services.saveAndFlush(service));
    }
    @Transactional
    public ServiceResponse updateService(Long id, ServiceRequest request) {
        User user = requireRole(Role.PROVIDER);
        Service service = findService(id);
        if (service.isDeleted()) throw new ApiException(404, "Service not found");
        if (!service.getProvider().getUser().getId().equals(user.getId())) throw new ApiException(403, "Service belongs to another provider");
        apply(service, request);
        return ApiMapper.service(services.saveAndFlush(service));
    }
    /**
     * The owner or an admin deletes a service. Open orders must be finished or cancelled first.
     * A service with past orders is only marked deleted, so order history stays intact.
     */
    @Transactional
    public void deleteService(Long id) {
        User user = current.require();
        Service service = findService(id);
        if (service.isDeleted()) throw new ApiException(404, "Service not found");
        boolean owner = user.getRole() == Role.PROVIDER && service.getProvider().getUser().getId().equals(user.getId());
        if (!owner && user.getRole() != Role.ADMIN) throw new ApiException(403, "Service belongs to another provider");
        if (orders.existsByServiceIdAndStatusIn(id, List.of(OrderStatus.PENDING, OrderStatus.ACCEPTED, OrderStatus.IN_PROGRESS)))
            throw new ApiException(409, "Service has open orders; finish or cancel them first");
        if (orders.existsByServiceId(id)) {
            service.setDeleted(true); service.setActive(false);
            services.saveAndFlush(service);
        } else {
            services.delete(service);
        }
    }
    private void apply(Service service, ServiceRequest request) {
        service.setCategory(categories.findById(request.categoryId()).orElseThrow(() -> new ApiException(404, "Category not found")));
        service.setName(request.name().strip()); service.setDescription(request.description().strip());
        service.setPrice(request.price()); service.setPriceUnit(request.priceUnit()); service.setActive(request.active());
    }
    private String pattern(String value) {
        return "%" + value.strip().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
    private ProviderProfile myProvider() {
        User user = requireRole(Role.PROVIDER);
        return providers.findByUserId(user.getId()).orElseThrow(() -> new ApiException(404, "Create your provider profile first"));
    }
    private User requireRole(Role role) {
        User user = current.require();
        if (user.getRole() != role) throw new ApiException(403, "Role " + role + " required");
        return user;
    }
    private ProviderProfile findProvider(Long id) {
        return providers.findById(id).orElseThrow(() -> new ApiException(404, "Provider not found"));
    }
    private Service findService(Long id) {
        return services.findById(id).orElseThrow(() -> new ApiException(404, "Service not found"));
    }
}
