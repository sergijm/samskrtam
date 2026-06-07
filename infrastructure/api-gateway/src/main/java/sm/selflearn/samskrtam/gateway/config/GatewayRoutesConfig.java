package sm.selflearn.samskrtam.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Маршрутизация объявлена через Java DSL — не в application.yml.
 *
 * <p>Порядок маршрутов важен: более специфичные пути (/content/public/**)
 * должны стоять перед общими (/content/**).
 *
 * <p>Если маршрут не проксируется — проверяй этот файл и SecurityConfig.
 */
@Configuration
public class GatewayRoutesConfig {

    @Value("${USER_SERVICE_URL:http://user-service:8087}")
    private String userServiceUrl;

    @Value("${CONTENT_SERVICE_URL:http://content-service:8081}")
    private String contentServiceUrl;

    @Value("${QUIZ_SERVICE_URL:http://quiz-service:8082}")
    private String quizServiceUrl;

    @Value("${DICTIONARY_SERVICE_URL:http://dictionary-service:8085}")
    private String dictionaryServiceUrl;

    @Value("${STATISTICS_SERVICE_URL:http://statistics-service:8086}")
    private String statisticsServiceUrl;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                // --- New routes for user-service auth endpoints with path rewriting ---
                .route("user-service-register-route", r -> r
                        .path("/api/v1/auth/register")
                        .filters(f -> f.rewritePath("/api/v1/auth/register", "/api/v1/users/register"))
                        .uri(userServiceUrl))
                .route("user-service-forgot-password-route", r -> r
                        .path("/api/v1/auth/forgot-password")
                        .filters(f -> f.rewritePath("/api/v1/auth/forgot-password", "/api/v1/users/forgot-password"))
                        .uri(userServiceUrl))
                // --- End new routes ---

                // ── Auth (публичный) — исключая OAuth2 пути которые обрабатывает Gateway ──
                // Логин/пароль, регистрация, refresh, logout — проксируем в user-service.
                // /api/v1/auth/oauth2/** НЕ проксируется — обрабатывается OAuthController.
                // /api/v1/auth/login НЕ проксируется — обрабатывается AuthController.
                // Now also exclude register and forgot-password as they are handled by specific routes above
                .route("auth", r -> r
                        .path("/api/v1/auth/**")
                        .and().not(p -> p.path("/api/v1/auth/oauth2/**")
                                .or().path("/api/v1/auth/login")
                                .or().path("/api/v1/auth/register") // Exclude specific register path
                                .or().path("/api/v1/auth/forgot-password")) // Exclude specific forgot-password path
                        .uri(userServiceUrl))

                // ── Admin Users (требует JWT и ADMIN роль) ──────────────────────────────────────────
                .route("admin-users", r -> r
                        .path("/api/v1/admin/users/**")
                        .uri(userServiceUrl))

                // ── Users (требует JWT) ──────────────────────────────────────────
                .route("users", r -> r
                        .path("/api/v1/users/**")
                        .uri(userServiceUrl))

                // ── Groups (требует JWT) ─────────────────────────────────────────
                .route("groups", r -> r
                        .path("/api/v1/groups/**")
                        .uri(userServiceUrl))

                // ── Content public (STUDENT) — до /content/** ────────────────────
                // Порядок важен: этот маршрут должен стоять перед /content/**
                .route("content-public", r -> r
                        .path("/api/v1/content/public/**")
                        .uri(contentServiceUrl))

                // ── Content admin (ADMIN) ────────────────────────────────────────
                .route("content", r -> r
                        .path("/api/v1/content/**")
                        .uri(contentServiceUrl))

                // ── Quiz Service ─────────────────────────────────────────────────
                .route("quiz", r -> r
                        .path("/api/v1/quiz/**")
                        .uri(quizServiceUrl))

                // --- New route for user quiz sessions ---
                .route("user-quiz-sessions", r -> r
                        .path("/api/v1/quiz-sessions") // Изменено
                        .uri(quizServiceUrl))
                // --- End new route ---

                // ── Dictionary ───────────────────────────────────────────────────
                .route("dictionary", r -> r
                        .path("/api/v1/dictionary/**")
                        .uri(dictionaryServiceUrl))

                // ── Statistics & Leaderboard ─────────────────────────────────────
                .route("statistics", r -> r
                        .path("/api/v1/statistics/**", "/api/v1/leaderboard/**")
                        .uri(statisticsServiceUrl))

                .build();
    }

    /**
     * Key resolver для Redis Rate Limiter.
     *
     * <p>Аутентифицированные запросы — ключ = userId из JWT subject.
     * Публичные запросы (/api/v1/auth/**) — ключ = "anonymous".
     *
     * <p>На этот бин ссылается application.yml через Spring EL:
     * {@code key-resolver: "#{@userKeyResolver}"}
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .map(auth -> auth.getToken().getSubject())
                .onErrorReturn("anonymous")
                .defaultIfEmpty("anonymous");
    }
}
