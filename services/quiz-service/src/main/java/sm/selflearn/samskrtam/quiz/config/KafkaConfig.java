package sm.selflearn.samskrtam.quiz.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer; // Import Spring's JsonSerializer
import reactor.kafka.sender.SenderOptions;
import sm.selflearn.samskrtam.kafka.serialization.SamskrtamJsonSerializer; // Import your custom serializer

import java.time.Duration;
import java.util.Map;

@Configuration
@Slf4j
public class KafkaConfig {

    @Bean
    public SamskrtamJsonSerializer samskrtamJsonSerializer(ObjectMapper objectMapper) {
        SamskrtamJsonSerializer serializer = new SamskrtamJsonSerializer(objectMapper);
        serializer.setAddTypeInfo(true); // Устанавливаем добавление информации о типе
        return serializer;
    }

    @Bean
    public ReactiveKafkaProducerTemplate<String, Object> reactiveKafkaProducerTemplate(
            KafkaProperties properties,
            SamskrtamJsonSerializer samskrtamJsonSerializer) { // Инжектируем наш бин сериализатора
        Map<String, Object> props = properties.buildProducerProperties();
        // Вместо указания класса, передаем экземпляр сериализатора
        // Spring Kafka автоматически обернет его в ProducerFactory
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class); // Указываем базовый класс
        props.put("security.protocol", "PLAINTEXT");
        props.put("listener.name", "EXTERNAL");

        // Явно указываем типы для SenderOptions
        SenderOptions<String, Object> senderOptions = SenderOptions.<String, Object>create(props)
                .withValueSerializer(samskrtamJsonSerializer); // Передаем наш экземпляр

        return new ReactiveKafkaProducerTemplate<>(senderOptions);
    }

    @Bean
    public ApplicationRunner kafkaInitializer(ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate) {
        return args -> kafkaTemplate.doOnProducer(producer -> {
                    log.info("Successfully initialized Kafka producer.");
                    return producer;
                })
                .then()
                .block(Duration.ofSeconds(10));
    }
}
