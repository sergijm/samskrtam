package sm.selflearn.samskrtam.statistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration; // Import FlywayAutoConfiguration

@SpringBootApplication(exclude = {FlywayAutoConfiguration.class}) // Исключаем автоконфигурацию Flyway
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
