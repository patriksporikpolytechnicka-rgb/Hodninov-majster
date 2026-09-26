package sk.kasv.fx.dto;

import sk.kasv.fx.entity.*;
import static sk.kasv.fx.dto.ApiDtos.*;

public final class ApiMapper {
    private ApiMapper() {}
    public static UserResponse user(User u) { return new UserResponse(u.getId(), u.getEmail(), u.getRole()); }
    public static CategoryResponse category(Category c) { return new CategoryResponse(c.getId(), c.getName()); }
    public static ProviderResponse provider(ProviderProfile p) {
        return new ProviderResponse(p.getId(), p.getName(), p.getDescription(), p.getLocation(),
                p.getPhone(), p.getProfileImage(), p.isAvailability());
    }
    public static ServiceResponse service(Service s) {
        return new ServiceResponse(s.getId(), provider(s.getProvider()), category(s.getCategory()),
                s.getName(), s.getDescription(), s.getPrice(), s.getPriceUnit(), s.isActive());
    }
    public static OrderResponse order(Order o) {
        return new OrderResponse(o.getId(), o.getCustomer().getId(), o.getProvider().getId(),
                o.getService().getId(), o.getServiceName(), o.getScheduledAt(), o.getStatus(),
                o.getUnitPrice(), o.getPriceUnit(), o.getQuantity(), o.getTotalPrice(),
                o.getAddress(), o.getNote(), o.getCreatedAt());
    }
}
