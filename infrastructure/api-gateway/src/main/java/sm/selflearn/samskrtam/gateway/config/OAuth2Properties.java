package sm.selflearn.samskrtam.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Типизированный биндинг OAuth2 настроек из application.yml.
 *
 * <p>Все чувствительные значения (client-secret) берутся только из переменных
 * окружения — никогда не хардкодятся и не попадают в логи.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth2.keycloak")
public class OAuth2Properties {

    /** Публичный базовый URL Keycloak для редиректов браузера (например, https://keycloak.sm.dev) */
    private String baseUrl;

    /** Внутренний базовый URL Keycloak для запросов с бэкенда (например, http://keycloak:8080) */
    private String internalBaseUrl;

    /** samskrtam */
    private String realm;

    /** samskrtam-frontend */
    private String clientId;

    /** Из env KEYCLOAK_CLIENT_SECRET — никогда не логировать */
    private String clientSecret;

    /**
     * Redirect URI зарегистрированный в Keycloak клиенте.
     * Пример: http://localhost:8090/api/v1/auth/oauth2/callback
     */
    private String redirectUri;

    /** Строит URL Authorization Endpoint для провайдера (для брокера) */
    public String brokerAuthorizationEndpoint(String providerAlias) {
        return "%s/realms/%s/broker/%s/login".formatted(baseUrl, realm, providerAlias);
    }

    /** Строит URL OpenID Connect Authorization Endpoint Keycloak (для редиректа браузера) */
    public String oidcAuthorizationEndpoint() {
        return "%s/realms/%s/protocol/openid-connect/auth".formatted(baseUrl, realm);
    }

    /** Строит URL Token Endpoint Keycloak (для запросов с бэкенда) */
    public String tokenEndpoint() {
        return "%s/realms/%s/protocol/openid-connect/token".formatted(internalBaseUrl, realm);
    }
}
