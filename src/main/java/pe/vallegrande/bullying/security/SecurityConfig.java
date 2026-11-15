package pe.vallegrande.bullying.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pe.vallegrande.bullying.users.AccountRepository;
import pe.vallegrande.bullying.users.AccountService;
import java.util.List;

@Configuration
@ConditionalOnWebApplication
public class SecurityConfig {
    @Bean
    UserDetailsService userDetailsService(AccountRepository repository) {
        return email -> {
            var account = repository.byEmail(AccountService.normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas."));
            return User.withUsername(account.correo()).password(account.passwordHash())
                .roles(account.rol()).disabled(!account.activo()).build();
        };
    }
    @Bean
    SecurityFilterChain security(HttpSecurity http, AccountRepository repository,
                                 @Value("${app.allowed-origin}") String origin) throws Exception {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(origin));
        cors.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
        cors.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        http.cors(c -> c.configurationSource(source));
        // CSRF permanece habilitado: el cliente obtiene el token en /api/auth/csrf.
        http.authorizeHttpRequests(a -> a
            .requestMatchers("/api/auth/csrf", "/api/auth/login").permitAll()
            .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/students/all").permitAll()
            .requestMatchers("/api/users", "/api/users/**").hasRole("ADMINISTRADOR")
            .requestMatchers(HttpMethod.POST,"/api/students").hasAnyRole("ADMINISTRADOR","CONVIVENCIA")
            .requestMatchers(HttpMethod.PUT,"/api/students/*").hasAnyRole("ADMINISTRADOR","CONVIVENCIA")
            .requestMatchers(HttpMethod.PATCH,"/api/students/*/status").hasAnyRole("ADMINISTRADOR","CONVIVENCIA")
            .requestMatchers(HttpMethod.GET,"/api/students/**").hasAnyRole("ADMINISTRADOR","CONVIVENCIA","DOCENTE_TUTOR")
            .requestMatchers("/api/cases", "/api/cases/**", "/api/students", "/api/alerts", "/api/reports")
                .hasAnyRole("ADMINISTRADOR", "CONVIVENCIA", "DOCENTE_TUTOR")
            .requestMatchers("/api/auth/me").authenticated()
            .anyRequest().denyAll());
        http.requestCache(c -> c.disable());
        http.formLogin(f -> f.loginProcessingUrl("/api/auth/login").usernameParameter("email")
            .successHandler((req, res, auth) -> {
                res.setStatus(200); res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"message\":\"Sesión iniciada.\"}");
            })
            .failureHandler((req, res, error) -> {
                res.setStatus(401); res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"error\":\"Correo o contraseña incorrectos, o cuenta inactiva.\"}");
            }));
        http.logout(l -> l.logoutUrl("/api/auth/logout").deleteCookies("JSESSIONID")
            .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)));
        http.exceptionHandling(e -> e
            .authenticationEntryPoint((req, res, error) -> {
                res.setStatus(401); res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"error\":\"Inicia sesión para continuar.\"}");
            })
            .accessDeniedHandler((req, res, error) -> {
                res.setStatus(403); res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"error\":\"Acceso denegado o token CSRF no válido.\"}");
            }));
        http.addFilterBefore(new LoginRateFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new ActiveAccountFilter(repository), AuthorizationFilter.class);
        return http.build();
    }
}
