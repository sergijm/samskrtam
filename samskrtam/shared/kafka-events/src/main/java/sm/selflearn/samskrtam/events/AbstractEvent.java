package sm.selflearn.samskrtam.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "eventType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AnswerSubmitted.class, name = "AnswerSubmitted"),
        @JsonSubTypes.Type(value = SessionCompleted.class, name = "SessionCompleted")
})
@Getter // Только геттеры
@NoArgsConstructor // Для Jackson десериализации
@AllArgsConstructor // Для Jackson десериализации со всеми полями (если нужно)
public abstract class AbstractEvent {
    protected final UUID eventId;
    protected final Instant occurredAt;
    protected final String eventType;

    // Конструктор для вызова подклассами, инициализирует eventId и occurredAt
    protected AbstractEvent(String eventType) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.eventType = eventType;
    }
}
