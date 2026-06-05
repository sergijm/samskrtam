package sm.selflearn.samskrtam.quiz.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.sender.SenderOptions;

import java.time.Duration;
import java.util.Map;

@Configuration
@Slf4j
public class KafkaConfig {

    @Bean
    public ReactiveKafkaProducerTemplate<String, Object> reactiveKafkaProducerTemplate(KafkaProperties properties) {
        Map<String, Object> props = properties.buildProducerProperties();
        // Устанавливаем короткий таймаут для быстрого падения, если Kafka недоступен
        props.put("delivery.timeout.ms", 5000);
        props.put("request.timeout.ms", 4000); // Должен быть меньше delivery.timeout.ms
        SenderOptions<String, Object> senderOptions = SenderOptions.create(props);
        return new ReactiveKafkaProducerTemplate<>(senderOptions);
    }

    /**
     * Принудительно инициализирует Kafka-продюсер при старте приложения.
     * Если Kafka недоступен, приложение упадет с ошибкой.
     *
     * @param kafkaTemplate Reactive-шаблон для Kafka.
     * @return ApplicationRunner, который будет выполнен при старте.
     */
    @Bean
    public ApplicationRunner kafkaInitializer(ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate) {
        return args -> kafkaTemplate.doOnProducer(producer -> {
                    log.info("Successfully initialized Kafka producer.");
                    return producer;
                })
                .then()
                .block(Duration.ofSeconds(10)); // Блокируемся, чтобы дождаться инициализации
    }
}
