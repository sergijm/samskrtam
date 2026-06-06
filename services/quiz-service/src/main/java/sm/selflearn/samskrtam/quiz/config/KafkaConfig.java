package sm.selflearn.samskrtam.quiz.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.sender.SenderOptions;
import sm.selflearn.samskrtam.kafka.serialization.SamskrtamJsonSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@Slf4j
public class KafkaConfig {

    @Bean
    public ReactiveKafkaProducerTemplate<String, Object> reactiveKafkaProducerTemplate(KafkaProperties properties) {
        Map<String, Object> props = properties.buildProducerProperties();
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, SamskrtamJsonSerializer.class);
        props.put("security.protocol", "PLAINTEXT");
        props.put("listener.name", "EXTERNAL");

        SenderOptions<String, Object> senderOptions = SenderOptions.create(props);
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
