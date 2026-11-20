package sk.kasv.fx.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Generated API docs: /swagger-ui.html (try requests in the browser) and /v3/api-docs (JSON).
 * "Authorize" takes the accessToken from /api/auth/login (without the word Bearer).
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "Hodinový majster API", version = "1"), security = @SecurityRequirement(name = "bearer"))
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {}
