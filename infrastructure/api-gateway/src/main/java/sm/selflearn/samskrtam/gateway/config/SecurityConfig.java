package sm.selflearn.samskrtam.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * Правила авторизации по путям.
 *
 * <p>Публичные (без JWT):
 * <ul>
 *   <li>/actuator/health/** — health probes для k8s
 *   <li>OPTIONS /** — CORS preflight
 *   <li>/api/v1/auth/** — логин, регистрация, восстановление пароля
 * </ul>
 *
 * <p>Только ADMIN:
 * <ul>
 *   <li>/api/v1/content/** (за исключением /content/public/**)
 * </ul>
 *
 * <p>Любой аутентифицированный:
 * <ul>
 *   <li>/api/v1/content/public/**
 *   <li>/api/v1/content/quizzes — список квизов
 *   <li>/api/** (квизы, словарь, статистика)
 * </ul>
 *
 * <p>Ошибки 401 и 403 возвращаются как JSON статус-коды, не HTML.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable) // Отключаем HTTP Basic Authentication
                .authorizeExchange(auth -> auth
                        // k8s health probes — без JWT
                        .pathMatchers("/actuator/health/**").permitAll()
                        // CORS preflight — без JWT
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // --- PUBLIC ENDPOINTS (permitAll) ---
                        // Auth endpoints — без JWT (логин, регистрация, OAuth2 flow)
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        // OAuth2 flow — без JWT (инициация + callback)
                        .pathMatchers("/api/v1/auth/oauth2/**").permitAll()
                        // Handle potential double slash in path for OAuth2
                        .pathMatchers("/api/v1/auth/oauth2/**").permitAll()

                        // --- AUTHENTICATED ENDPOINTS (authenticated) ---
                        // Публичный контент — только аутентифицированные
                        .pathMatchers("/api/v1/content/public/**").authenticated()
                        // Список квизов — любой аутентифицированный
                        .pathMatchers("/api/v1/content/quizzes").authenticated()
                        // Группы - любой аутентифицированный пользователь
                        .pathMatchers("/api/v1/groups/**").authenticated() // Added for authenticated access to groups
                        // Квизы, словарь, статистика — любой аутентифицированный
                        .pathMatchers("/api/**").authenticated()

                        // --- ADMIN ENDPOINTS (hasRole) ---
                        // Управление контентом — только ADMIN
                        .pathMatchers("/api/v1/content/**").hasRole("ADMIN")
                        
                        // --- DENY ALL OTHER REQUESTS ---
                        .anyExchange().denyAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {}) // JWKS URI берётся из application.yml
                )
                // 401 — JSON, не HTML редирект
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(
                                new HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN))
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
