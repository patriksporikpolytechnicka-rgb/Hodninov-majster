package sk.kasv.fx.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import sk.kasv.fx.entity.User;
import sk.kasv.fx.exception.ApiException;
import sk.kasv.fx.repository.UserRepository;

@Component
public class CurrentUser {
    private final UserRepository users;
    public CurrentUser(UserRepository users) { this.users = users; }
    public User require() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) throw new ApiException(401, "Authentication required");
        try {
            User user = users.findById(Long.valueOf(jwt.getSubject()))
                    .orElseThrow(() -> new ApiException(401, "Account no longer exists"));
            if (user.isBlocked()) throw new ApiException(403, "Account is blocked");
            Object version = jwt.getClaims().get("tv");
            int tokenVersion = version instanceof Number n ? n.intValue() : 0;
            if (tokenVersion != user.getTokenVersion()) throw new ApiException(401, "Token was revoked");
            return user;
        } catch (NumberFormatException e) { throw new ApiException(401, "Invalid token subject"); }
    }
}
