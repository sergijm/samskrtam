package sm.selflearn.samskrtam.user.config;

import lombok.Getter;
import lombok.Setter;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "keycloak")
@Getter
@Setter
public class KeycloakAdminClientConfig {

    private String url;
    private String realm;
    private String clientIdAdmin;
    private String clientSecretAdmin;
    // Removed adminUser and adminPassword as they are not used for client_credentials grant

    @Bean
    public Keycloak keycloakAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(url)
                .realm(realm)
                .clientId(clientIdAdmin)
                .clientSecret(clientSecretAdmin)
                .grantType("client_credentials") // Correctly using client_credentials grant
                .build();
    }
}
