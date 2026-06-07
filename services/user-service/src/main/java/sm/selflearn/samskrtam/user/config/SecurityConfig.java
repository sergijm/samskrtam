package sm.selflearn.samskrtam.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Отключаем CSRF для API
            .authorizeHttpRequests(authorize -> authorize
                // Public endpoints that should always be accessible
                .requestMatchers(HttpMethod.POST, "/api/v1/users/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/users/forgot-password").permitAll()
                // Endpoint for OAuth2 profile synchronization, called by API Gateway
                .requestMatchers(HttpMethod.POST, "/api/v1/users/oauth2/sync").permitAll()
                // All other requests are permitted, as authentication is handled by API Gateway
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {})); // Enable JWT decoding for internal use

        return http.build();
    }
}
