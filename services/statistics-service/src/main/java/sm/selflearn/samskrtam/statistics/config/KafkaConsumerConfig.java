package sm.selflearn.samskrtam.statistics.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import sm.selflearn.samskrtam.kafka.serialization.SamskrtamJsonDeserializer; // Import your custom deserializer

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public SamskrtamJsonDeserializer<Object> samskrtamJsonDeserializer(ObjectMapper objectMapper) {
        SamskrtamJsonDeserializer<Object> deserializer = new SamskrtamJsonDeserializer<>(objectMapper);
        deserializer.setUseTypeHeaders(true);
        deserializer.addTrustedPackages("sm.selflearn.samskrtam.events");
        // Если нужны явные маппинги типов для @JsonTypeInfo, их можно добавить здесь
        // deserializer.setTypeMappings(Map.of("AnswerSubmitted", sm.selflearn.samskrtam.events.AnswerSubmitted.class));
        return deserializer;
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory(
            KafkaProperties kafkaProperties,
            SamskrtamJsonDeserializer<Object> samskrtamJsonDeserializer) { // Инжектируем наш бин десериализатора
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        // Вместо указания класса, передаем экземпляр десериализатора
        return new DefaultKafkaConsumerFactory<>(props, new org.apache.kafka.common.serialization.StringDeserializer(), samskrtamJsonDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
