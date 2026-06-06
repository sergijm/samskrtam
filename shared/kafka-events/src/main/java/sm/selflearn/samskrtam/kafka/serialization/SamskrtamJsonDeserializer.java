package sm.selflearn.samskrtam.kafka.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Slf4j
public class SamskrtamJsonDeserializer<T> extends JsonDeserializer<T> {

    // Конструктор, принимающий ObjectMapper
    public SamskrtamJsonDeserializer(ObjectMapper objectMapper) {
        super(objectMapper);
        // setUseTypeHeaders(true) и addTrustedPackages будут установлены в KafkaConsumerConfig
    }

    @Override
    public T deserialize(String topic, Headers headers, byte[] data) {
        try {
            return super.deserialize(topic, headers, data);
        } catch (Exception e) {
            log.error("Error deserializing message from topic {}: {}", topic, new String(data), e);
            // Возвращаем null, чтобы не блокировать обработку, но ошибка будет залогирована
            return null;
        }
    }
}
