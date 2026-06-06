package sm.selflearn.samskrtam.statistics.config;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import sm.selflearn.samskrtam.kafka.serialization.SamskrtamJsonDeserializer; // Import your custom deserializer

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, Object> consumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        // Create an instance of your custom SamskrtamJsonDeserializer
        SamskrtamJsonDeserializer<Object> jsonDeserializer = new SamskrtamJsonDeserializer<>();
        
        // Explicitly configure the deserializer properties
        jsonDeserializer.setUseTypeHeaders(true);
        jsonDeserializer.addTrustedPackages("sm.selflearn.samskrtam.events");
        
        // If you need specific type mappings for @JsonTypeInfo, you can set them here.
        // However, with @JsonTypeInfo and @JsonSubTypes on AbstractEvent,
        // the deserializer should automatically handle it if type headers are present.
        // For robustness, you might still want to add explicit type mappings if the header
        // doesn't contain the full class name or if you use aliases.
        // For example:
        // Map<String, Class<?>> typeMappings = new HashMap<>();
        // typeMappings.put("AnswerSubmitted", sm.selflearn.samskrtam.events.AnswerSubmitted.class);
        // typeMappings.put("SessionCompleted", sm.selflearn.samskrtam.events.SessionCompleted.class);
        // jsonDeserializer.setTypeMappings(typeMappings);


        return new DefaultKafkaConsumerFactory<>(props, new org.apache.kafka.common.serialization.StringDeserializer(), jsonDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
