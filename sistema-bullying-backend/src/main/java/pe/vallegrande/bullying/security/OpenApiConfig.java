package pe.vallegrande.bullying.security;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
class OpenApiConfig {
    private static final String SESSION = "Sesion";
    private static final String CSRF = "CSRF";
    private static final String LOGIN = "/api/auth/login";
    private static final String LOGOUT = "/api/auth/logout";
    private static final String TOKEN = "/api/auth/csrf";
    private static final Set<PathItem.HttpMethod> READ_METHODS =
        Set.of(PathItem.HttpMethod.GET, PathItem.HttpMethod.HEAD, PathItem.HttpMethod.OPTIONS);

    @Bean
    OpenApiCustomizer sessionApiDocumentation() {
        return api -> {
            api.info(new Info().title("API de Convivencia Escolar").version("1.0").description("""
                Para iniciar sesión, ejecuta GET /api/auth/csrf, copia el valor de token
                y pégalo en Authorize → CSRF. Ejecuta POST /api/auth/login con tu correo y contraseña.
                El navegador conserva la cookie de sesión. Para escribir o cerrar sesión,
                vuelve a ejecutar GET /api/auth/csrf después del login y reemplaza el token en Authorize.
                """));
            api.getComponents()
                .addSecuritySchemes(SESSION, new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.COOKIE).name("JSESSIONID")
                    .description("El navegador guarda esta cookie al iniciar sesión."))
                .addSecuritySchemes(CSRF, new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER).name("X-CSRF-TOKEN")
                    .description("Pega el campo token devuelto por GET /api/auth/csrf."));

            // Spring Security implementa estas operaciones con filtros, fuera de MVC.
            api.path(LOGIN, new PathItem().post(login()));
            api.path(LOGOUT, new PathItem().post(new Operation().operationId("cerrarSesion")
                .tags(List.of("Autenticación")).summary("Cerrar la sesión actual")
                .responses(new ApiResponses().addApiResponse("204", response("Sesión cerrada")))));
            api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                if (TOKEN.equals(path)) {
                    operation.tags(List.of("Autenticación")).summary("Obtener token CSRF");
                    return;
                }
                if ("/api/auth/me".equals(path)) {
                    operation.tags(List.of("Autenticación")).summary("Consultar la sesión actual");
                }
                var required = new SecurityRequirement();
                if (!LOGIN.equals(path)) {
                    required.addList(SESSION);
                }
                if (!READ_METHODS.contains(method)) {
                    required.addList(CSRF);
                }
                operation.addSecurityItem(required);
            }));
        };
    }

    private static Operation login() {
        var form = new ObjectSchema();
        form.addProperty("email", new StringSchema().format("email"));
        form.addProperty("password", new StringSchema().format("password"));
        form.setRequired(List.of("email", "password"));
        return new Operation().operationId("iniciarSesion").summary("Iniciar sesión")
            .tags(List.of("Autenticación"))
            .requestBody(new RequestBody().required(true).content(new Content()
                .addMediaType("application/x-www-form-urlencoded", new MediaType().schema(form))))
            .responses(new ApiResponses()
                .addApiResponse("200", response("Sesión iniciada y cookie de sesión emitida"))
                .addApiResponse("401", response("Credenciales incorrectas o cuenta inactiva"))
                .addApiResponse("403", response("Token CSRF no válido"))
                .addApiResponse("429", response("Demasiados intentos de inicio de sesión")));
    }

    private static ApiResponse response(String description) {
        return new ApiResponse().description(description);
    }
}
