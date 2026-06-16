package sm.selflearn.samskrtam.quiz.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import reactor.kafka.sender.SenderOptions;
import sm.selflearn.samskrtam.quiz.event.StatisticEvent;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class KafkaConfig {

    @Bean
    public JsonSerializer<StatisticEvent> statisticEventJsonSerializer(ObjectMapper objectMapper) {
        JsonSerializer<StatisticEvent> serializer = new JsonSerializer<>(objectMapper);
        serializer.setAddTypeInfo(true);
        return serializer;
    }

    @Bean
    @Primary
    public ReactiveKafkaProducerTemplate<String, StatisticEvent> reactiveKafkaProducerTemplate(
            KafkaProperties properties,
            JsonSerializer<StatisticEvent> statisticEventJsonSerializer) {
        Map<String, Object> props = new HashMap<>(properties.buildProducerProperties()); // Use buildProducerProperties for compatibility
        props.put("security.protocol", "PLAINTEXT");
        props.put("listener.name", "EXTERNAL");

        SenderOptions<String, StatisticEvent> senderOptions = SenderOptions.<String, StatisticEvent>create(props)
                .withValueSerializer(statisticEventJsonSerializer);

        return new ReactiveKafkaProducerTemplate<>(senderOptions);
    }

    @Bean
    @Qualifier("outboxKafkaProducer")
    public ReactiveKafkaProducerTemplate<String, Object> outboxKafkaProducer(
            KafkaProperties properties,
            ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>(properties.buildProducerProperties()); // Use buildProducerProperties for compatibility
        props.put("security.protocol", "PLAINTEXT");
        props.put("listener.name", "EXTERNAL");

        JsonSerializer<Object> serializer = new JsonSerializer<>(objectMapper);
        serializer.setAddTypeInfo(true);

        SenderOptions<String, Object> senderOptions = SenderOptions.<String, Object>create(props)
                .withValueSerializer(serializer);

        return new ReactiveKafkaProducerTemplate<>(senderOptions);
    }

    @Bean
    public ApplicationRunner kafkaInitializer(ReactiveKafkaProducerTemplate<String, StatisticEvent> kafkaTemplate) {
        return args -> kafkaTemplate.doOnProducer(producer -> {
                    log.info("Successfully initialized Kafka producer.");
                    return producer;
                })
                .then()
                .block(Duration.ofSeconds(10));
    }
}
