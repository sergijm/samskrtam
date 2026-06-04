package sm.selflearn.samskrtam.quiz.config;

import org.flywaydb.core.Flyway; // Added import
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Bean(initMethod = "migrate")
    public Flyway flyway() {
        return Flyway.configure()
                .dataSource(datasourceUrl, datasourceUsername, datasourcePassword)
                .schemas("quiz")
                .baselineOnMigrate(true)
                .load();
    }
}
