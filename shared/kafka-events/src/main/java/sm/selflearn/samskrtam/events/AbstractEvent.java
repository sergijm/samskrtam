package sm.selflearn.samskrtam.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder; // Import Builder
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

// Базовый класс или интерфейс
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,          // Используем имя типа
        include = JsonTypeInfo.As.PROPERTY,  // Как часть JSON-объекта
        property = "eventType"               // Поле, по которому определяем тип
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AnswerSubmitted.class, name = "AnswerSubmitted"),
        @JsonSubTypes.Type(value = SessionCompleted.class, name = "SessionCompleted")
})
@Data
@SuperBuilder
@NoArgsConstructor // Lombok will generate a no-args constructor
public abstract class AbstractEvent {
    @Builder.Default // Ensure default initialization when using builder
    protected UUID eventId = UUID.randomUUID();
    @Builder.Default // Ensure default initialization when using builder
    protected Instant occurredAt = Instant.now();

    // Геттер для eventType, который будет использоваться Jackson для сериализации.
    // Он не является полем класса, а вычисляется динамически.
    @JsonProperty("eventType")
    public String getEventType() {
        return this.getClass().getSimpleName();
    }

    // Сеттер для eventType, который будет использоваться Jackson для десериализации.
    // Тело пустое, так как значение не хранится в поле, а выводится из типа.
    @JsonProperty("eventType")
    public void setEventType(String eventType) {
        // Do nothing, as eventType is derived from the class name
    }
}
