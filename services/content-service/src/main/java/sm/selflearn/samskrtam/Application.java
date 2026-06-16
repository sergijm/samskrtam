package sm.selflearn.samskrtam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(scanBasePackages = {
        "sm.selflearn.samskrtam"}
)
@EnableJpaRepositories(basePackages = "sm.selflearn.samskrtam")
@EntityScan(basePackages = "sm.selflearn.samskrtam")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
