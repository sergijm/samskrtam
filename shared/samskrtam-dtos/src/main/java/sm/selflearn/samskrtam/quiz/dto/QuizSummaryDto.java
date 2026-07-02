package sm.selflearn.samskrtam.quiz.dto;

import lombok.*;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSummaryDto {
    UUID sessionId;
    UUID lessonId;
    String lessonTitle;
    String lessonTitleRu;
    String lessonTitleEn;
    String slug;
    LessonType lessonType;
    int score;
    int totalQuestions;
    SessionStatus status;
    Instant startedAt;
    Instant completedAt;
    Long durationMs;
}