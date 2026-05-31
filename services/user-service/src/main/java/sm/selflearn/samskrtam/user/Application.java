package sm.selflearn.samskrtam.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        // Enable virtual threads
        System.setProperty("spring.threads.virtual.enabled", "true");
        SpringApplication.run(Application.class, args);
    }

}
