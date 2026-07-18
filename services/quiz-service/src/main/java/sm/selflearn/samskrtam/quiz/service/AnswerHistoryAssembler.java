package sm.selflearn.samskrtam.quiz.service;

import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.quiz.dto.AnswerHistoryDto;
import sm.selflearn.samskrtam.quiz.model.QuizAnswer;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Собирает список {@link AnswerHistoryDto} из сгенерированных вопросов и ответов пользователя.
 */
@Component
public class AnswerHistoryAssembler {

    /**
     * Собирает полную историю ответов: для каждого вопроса сессии — был ли ответ,
     * какой вариант выбран, верный ли он.
     *
     * @param generatedQuestions вопросы сессии (отсортированы по id)
     * @param quizAnswers        ответы пользователя
     * @return список AnswerHistoryDto, по одному на каждый вопрос
     */
    public List<AnswerHistoryDto> assemble(
            List<GeneratedQuizQuestionDto> generatedQuestions,
            List<QuizAnswer> quizAnswers) {

        Map<UUID, QuizAnswer> answersByQuestionId = quizAnswers.stream()
                .collect(Collectors.toMap(QuizAnswer::getQuestionId, Function.identity()));

        return generatedQuestions.stream()
                .sorted(Comparator.comparing(GeneratedQuizQuestionDto::getId))
                .map(gq -> {
                    QuizAnswer answer = answersByQuestionId.get(gq.getId());
                    String explanationRu = gq.getExplanationRu() != null ? gq.getExplanationRu() : "";
                    String explanationEn = gq.getExplanationEn() != null ? gq.getExplanationEn() : "";

                    return AnswerHistoryDto.builder()
                            .questionId(gq.getId())
                            .questionNumber(gq.getQuestionNumber())
                            .questionText(gq.getText())
                            .selectedAnswerIast(answer != null ? answer.getSelectedFormIast() : null)
                            .correctOptionIast(gq.getCorrectFormIast())
                            .isCorrect(answer != null ? answer.getIsCorrect() : null)
                            .responseTimeMs(answer != null ? answer.getResponseTimeMs() : null)
                            .answeredAt(answer != null ? answer.getAnsweredAt() : null)
                            .explanationRu(explanationRu)
                            .explanationEn(explanationEn)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
