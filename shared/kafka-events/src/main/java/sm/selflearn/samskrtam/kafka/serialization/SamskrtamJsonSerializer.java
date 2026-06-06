package sm.selflearn.samskrtam.kafka.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Slf4j
public class SamskrtamJsonSerializer extends JsonSerializer<Object> {

    // Конструктор, принимающий ObjectMapper
    public SamskrtamJsonSerializer(ObjectMapper objectMapper) {
        super(objectMapper);
        // setAddTypeInfo(true) будет установлено в KafkaConfig
    }

    @Override
    public byte[] serialize(String topic, Object data) {
        try {
            return super.serialize(topic, data);
        } catch (Exception e) {
            log.error("Error serializing message for topic {}: {}", topic, data, e);
            // Возвращаем null, чтобы не блокировать отправку, но ошибка будет залогирована
            return null;
        }
    }
}
