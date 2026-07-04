package sm.selflearn.samskrtam.sangraha.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "sangraha.llm")
public class LlmProperties {
    private String baseUrl;
    private String apiKey;
    private String model;
}