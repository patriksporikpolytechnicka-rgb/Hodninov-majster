package sk.kasv.fx.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.security.*;
import sk.kasv.fx.service.*;
import java.time.Instant;
import java.util.List;
import static sk.kasv.fx.dto.ApiDtos.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class, CategoryController.class, ProviderController.class,
        ServiceController.class, OrderController.class, ReviewController.class, AdminController.class}, properties = {
        "app.jwt.secret=test-only-key-at-least-thirty-two-bytes-long",
        "app.cors.allowed-origin=http://localhost:4200",
        "spring.jackson.deserialization.fail-on-unknown-properties=true"})
@Import(SecurityConfig.class)
class ApiControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JwtEncoder encoder;
    @MockitoBean AuthService auth;
    @MockitoBean CatalogService catalog;
    @MockitoBean OrderService orders;
    @MockitoBean ReviewService reviews;
    @MockitoBean UserAdminService userAdmin;
    @MockitoBean LoginAttemptService attempts;
    @MockitoBean ProviderStatsService providerStats;
    @MockitoBean CurrentUser current;

    @Test void categoriesArePublic() throws Exception {
        when(catalog.categories()).thenReturn(List.of(new CategoryResponse(1L, "Garden")));
        mvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Garden"));
    }
    @Test void ordersRequireAuthentication() throws Exception {
        mvc.perform(get("/api/orders/my")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        verifyNoInteractions(orders);
    }
    @Test void ownProfileAndServicesAreNotPublic() throws Exception {
        mvc.perform(get("/api/providers/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/services/mine")).andExpect(status().isUnauthorized());
    }
    @Test void invalidServiceFieldsAreRejected() throws Exception {
        mvc.perform(post("/api/services").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("""
                {"categoryId":1,"name":" ","description":"Description","price":-1,"priceUnit":"HOUR","active":true}
                """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.price").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
        verifyNoInteractions(catalog);
    }
    @Test void frontendCannotSupplyCustomerOrPrice() throws Exception {
        mvc.perform(post("/api/orders").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("""
                {"serviceId":1,"scheduledAt":"2027-01-10T10:00:00","quantity":2,"address":"Test","customerId":9,"totalPrice":0}
                """)).andExpect(status().isBadRequest());
        verifyNoInteractions(orders);
    }
    @Test void paginationIsBounded() throws Exception {
        mvc.perform(get("/api/services?size=101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/services?page=-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/services?sort=cheapest")).andExpect(status().isBadRequest());
        verifyNoInteractions(catalog);
    }
    @Test void angularPreflightIsAllowed() throws Exception {
        mvc.perform(options("/api/orders").header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }
    @Test void signedJwtIsAcceptedAndPasswordNeverReturned() throws Exception {
        User user = new User("a@example.invalid", "secret-hash", Role.CUSTOMER); user.setId(7L);
        when(current.require()).thenReturn(user);
        var claims = JwtClaimsSet.builder().issuer(SecurityConfig.ISSUER).subject("7").issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60)).claim("role", "CUSTOMER").build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7)).andExpect(jsonPath("$.password").doesNotExist());
    }
    @Test void providerRatingAndReviewsArePublic() throws Exception {
        when(reviews.rating(3L)).thenReturn(new RatingResponse(3L, 4.5, 2));
        mvc.perform(get("/api/providers/3/rating")).andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(4.5)).andExpect(jsonPath("$.count").value(2));
        when(reviews.forProvider(3L, 0, 10)).thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0));
        mvc.perform(get("/api/providers/3/reviews")).andExpect(status().isOk());
    }
    @Test void writingAReviewNeedsLoginAndValidRating() throws Exception {
        mvc.perform(post("/api/orders/5/review").contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/orders/5/review").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{\"rating\":6}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.rating").exists());
        verifyNoInteractions(reviews);
    }
    @Test void adminAndDeleteEndpointsNeedLogin() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/services/3")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/categories/1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(userAdmin, catalog);
    }
    @Test void deletingServiceReturnsNoContent() throws Exception {
        mvc.perform(delete("/api/services/3").with(jwt())).andExpect(status().isNoContent());
        verify(catalog).deleteService(3L);
    }
    @Test void tooManyLoginAttemptsReturn429() throws Exception {
        doThrow(new sk.kasv.fx.exception.ApiException(429, "Too many attempts; try again in 15 minutes"))
                .when(attempts).check(eq("login"), eq("a@example.invalid"), any());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"A@example.invalid\",\"password\":\"whatever-123\"}"))
                .andExpect(status().isTooManyRequests());
        verifyNoInteractions(auth);
    }
    @Test void refreshAndPasswordResetArePublic() throws Exception {
        mvc.perform(post("/api/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@example.invalid\"}")).andExpect(status().isAccepted());
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"abc\"}")).andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/logout-all")).andExpect(status().isUnauthorized());
    }
    @Test void invalidJwtIsRejected() throws Exception {
        mvc.perform(get("/api/orders/my").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(orders);
    }
}
