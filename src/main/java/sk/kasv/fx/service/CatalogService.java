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
import java.util.List;
import java.util.Locale;
import static sk.kasv.fx.dto.ApiDtos.*;

@org.springframework.stereotype.Service
@Transactional(readOnly = true)
public class CatalogService {
    private final CategoryRepository categories;
    private final ServiceRepository services;
    private final ProviderProfileRepository providers;
    private final CurrentUser current;
    @org.springframework.beans.factory.annotation.Autowired
    private java.time.Clock clock = java.time.Clock.systemUTC();
    public CatalogService(CategoryRepository categories, ServiceRepository services,
                          ProviderProfileRepository providers, CurrentUser current) {
        this.categories = categories; this.services = services; this.providers = providers; this.current = current;
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
    public ProviderResponse provider(Long id) { return ApiMapper.provider(findProvider(id)); }
    public ProviderResponse myProfile() { return ApiMapper.provider(myProvider()); }
    @Transactional
    public ProviderResponse saveProfile(ProviderRequest request) {
        User user = requireRole(Role.PROVIDER);
        ProviderProfile profile = providers.findByUserId(user.getId()).orElseGet(ProviderProfile::new);
        profile.setUser(user); profile.setName(request.name().strip());
        profile.setDescription(request.description().strip()); profile.setLocation(request.location().strip());
        profile.setPhone(request.phone().strip()); profile.setProfileImage(request.profileImage());
        profile.setAvailability(request.availability()); profile.startTrial(clock.instant());
        return ApiMapper.provider(providers.saveAndFlush(profile));
    }
    public PageResponse<ServiceResponse> services(Long categoryId, Long providerId, String q, String location, int page, int size) {
        Specification<Service> spec = (root, query, cb) -> cb.isTrue(root.get("active"));
        if (categoryId != null) spec = spec.and((r, query, cb) -> cb.equal(r.get("category").get("id"), categoryId));
        if (providerId != null) spec = spec.and((r, query, cb) -> cb.equal(r.get("provider").get("id"), providerId));
        if (q != null && !q.isBlank()) spec = spec.and((r, query, cb) -> cb.like(cb.lower(r.get("name")), pattern(q), '!'));
        if (location != null && !location.isBlank()) spec = spec.and((r, query, cb) -> cb.like(cb.lower(r.get("provider").get("location")), pattern(location), '!'));
        return PageResponse.from(services.findAll(spec, PageRequest.of(page, size, Sort.by("id"))).map(ApiMapper::service));
    }
    public PageResponse<ServiceResponse> myServices(int page, int size) {
        return PageResponse.from(services.findByProviderId(myProvider().getId(),
                PageRequest.of(page, size, Sort.by("id"))).map(ApiMapper::service));
    }
    public ServiceResponse service(Long id) {
        Service service = findService(id);
        if (!service.isActive()) throw new ApiException(404, "Service not found");
        return ApiMapper.service(service);
    }
    @Transactional
    public ServiceResponse createService(ServiceRequest request) {
        Service service = new Service(); service.setProvider(myProvider());
        apply(service, request);
        return ApiMapper.service(services.saveAndFlush(service));
    }
    @Transactional
    public ServiceResponse updateService(Long id, ServiceRequest request) {
        User user = requireRole(Role.PROVIDER);
        Service service = findService(id);
        if (!service.getProvider().getUser().getId().equals(user.getId())) throw new ApiException(403, "Service belongs to another provider");
        apply(service, request);
        return ApiMapper.service(services.saveAndFlush(service));
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
