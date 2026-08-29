package sm.selflearn.samskrtam.common.transliteration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Регистрирует {@link TransliterationService} как Spring-бин.
 * Подхватывается во всех сервисах через auto-configuration (META-INF/spring/
 * org.springframework.boot.autoconfigure.AutoConfiguration.imports), поэтому
 * явный @Import в каждом сервисе не требуется.
 */
@Configuration
public class TransliterationAutoConfiguration {

    @Bean
    public TransliterationService transliterationService() {
        return new TransliterationService();
    }
}
