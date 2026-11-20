package sk.kasv.fx.security;

import sk.kasv.fx.entity.User;
import sk.kasv.fx.exception.ApiException;

/** Actions that involve other people (ordering, offering services) need a verified e-mail. */
public final class Verified {
    public static final String MESSAGE = "Verify your e-mail address first";
    private Verified() {}
    public static User require(User user) {
        if (!user.isEmailVerified()) throw new ApiException(403, MESSAGE);
        return user;
    }
}
