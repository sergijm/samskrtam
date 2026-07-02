package sm.selflearn.samskrtam.sangraha.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class LlmConfig {

    @Bean
    public RestClient llmRestClient(LlmProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Getter
    @Setter
    @ConfigurationProperties(prefix = "sangraha.llm")
    public static class LlmProperties {
        private String baseUrl;
        private String apiKey;
        private String model;
    }
}