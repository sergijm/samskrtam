package sm.selflearn.samskrtam.quiz.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import sm.selflearn.samskrtam.content.dto.LessonType;

import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = QuizAnsweredEvent.class, name = "QuizAnsweredEvent"),
        @JsonSubTypes.Type(value = QuizSessionStatusChangedEvent.class, name = "QuizSessionStatusChangedEvent")
})
public interface StatisticEvent {
    UUID userId();
    UUID lessonId();
    LessonType lessonType();
    Instant timestamp();
}

