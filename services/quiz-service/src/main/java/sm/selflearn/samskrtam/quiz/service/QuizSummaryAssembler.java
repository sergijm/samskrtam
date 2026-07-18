package sm.selflearn.samskrtam.quiz.service;

import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.content.dto.LessonItemResponse;
import sm.selflearn.samskrtam.quiz.dto.QuizSummaryDto;
import sm.selflearn.samskrtam.quiz.model.QuizSession;

import java.time.Duration;

/**
 * Собирает {@link QuizSummaryDto} из данных сессии, информации об уроке
 * и количества правильных ответов.
 */
@Component
public class QuizSummaryAssembler {

    /**
     * Собирает сводку по одной сессии.
     *
     * @param session           сессия квиза
     * @param lessonItem        данные урока из content-service (может быть null)
     * @param correctAnswers    количество правильных ответов
     * @param combinationsCount количество фильтр-комбинаций (0 если без фильтра)
     * @return заполненный QuizSummaryDto
     */
    public QuizSummaryDto assemble(QuizSession session, LessonItemResponse lessonItem,
                                   long correctAnswers, int combinationsCount) {
        Long durationMs = null;
        if (session.getCompletedAt() != null) {
            durationMs = Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis();
        }
        return QuizSummaryDto.builder()
                .sessionId(session.getId())
                .lessonId(session.getLessonId())
                .lessonTitle(lessonItem != null ? lessonItem.getTitleEn() : "Unknown Lesson")
                .lessonTitleRu(lessonItem != null ? lessonItem.getTitleRu() : "Unknown Lesson")
                .lessonTitleEn(lessonItem != null ? lessonItem.getTitleEn() : "Unknown Lesson")
                .slug(lessonItem != null ? lessonItem.getSlug() : "")
                .lessonType(session.getLessonType())
                .score(session.getScore())
                .totalQuestions(session.getTotalQuestions())
                .answeredQuestions(session.getAnsweredQuestions())
                .correctAnswers((int) correctAnswers)
                .combinationsCount(combinationsCount)
                .status(session.getStatus())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .durationMs(durationMs)
                .build();
    }
}
