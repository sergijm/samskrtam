package sm.selflearn.samskrtam.statistics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.events.AnswerSubmitted;
import sm.selflearn.samskrtam.events.SessionCompleted;
import sm.selflearn.samskrtam.statistics.model.UserQuizSessionStatistic;
import sm.selflearn.samskrtam.statistics.repository.UserQuizSessionStatisticRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticService {

    private final UserQuizSessionStatisticRepository userQuizSessionStatisticRepository;
    private final ObjectMapper objectMapper; // Inject ObjectMapper

    @Transactional
    public void processSessionCompleted(SessionCompleted event) {
        log.info("Processing SessionCompleted event: {}", event);

        String answerHistoryJson = null;
        try {
            answerHistoryJson = objectMapper.writeValueAsString(event.getAnswers());
        } catch (JsonProcessingException e) {
            log.error("Error serializing answer history for quiz {}: {}", event.getQuizId(), e.getMessage()); // Changed to event.getQuizId()
            // Decide how to handle this error: throw, return, or proceed without history
        }

        final String finalAnswerHistoryJson = answerHistoryJson; // For use in lambda

        userQuizSessionStatisticRepository.findByUserIdAndQuizId(event.getUserId(), event.getQuizId())
                .ifPresentOrElse(
                        statistic -> {
                            // Update existing statistic
                            statistic.setTotalSessions(statistic.getTotalSessions() + 1);
                            statistic.setTotalQuestionsAnswered(statistic.getTotalQuestionsAnswered() + event.getTotalQuestions());
                            statistic.setTotalCorrectAnswers(statistic.getTotalCorrectAnswers() + event.getScore());
                            statistic.setTotalScore(statistic.getTotalScore() + event.getScore());
                            statistic.setLastCompletedAt(Instant.now());
                            statistic.setAverageScore((double) statistic.getTotalCorrectAnswers() / statistic.getTotalQuestionsAnswered());
                            statistic.setAnswerHistoryJson(finalAnswerHistoryJson); // Set the new field
                            userQuizSessionStatisticRepository.save(statistic);
                            log.debug("Updated statistic for user {} and quiz {}: {}", event.getUserId(), event.getQuizId(), statistic);
                        },
                        () -> {
                            // Create new statistic (should not happen if AnswerSubmitted is processed first, but as fallback)
                            UserQuizSessionStatistic newStatistic = UserQuizSessionStatistic.builder()
                                    .userId(event.getUserId())
                                    .quizId(event.getQuizId())
                                    .quizType(event.getQuizType())
                                    .totalSessions(1)
                                    .totalQuestionsAnswered(event.getTotalQuestions())
                                    .totalCorrectAnswers(event.getScore())
                                    .totalScore(event.getScore())
                                    .averageScore((double) event.getScore() / event.getTotalQuestions())
                                    .lastCompletedAt(Instant.now())
                                    .answerHistoryJson(finalAnswerHistoryJson) // Set the new field
                                    .build();
                            userQuizSessionStatisticRepository.save(newStatistic);
                            log.debug("Created new statistic for user {} and quiz {}: {}", event.getUserId(), event.getQuizId(), newStatistic);
                        }
                );
    }

    @Transactional
    public void processAnswerSubmitted(AnswerSubmitted event) {
        log.info("Processing AnswerSubmitted event: {}", event);

        userQuizSessionStatisticRepository.findByUserIdAndQuizId(event.getUserId(), event.getQuizId())
                .ifPresentOrElse(
                        statistic -> {
                            // Update existing statistic
                            statistic.setTotalQuestionsAnswered(statistic.getTotalQuestionsAnswered() + 1);
                            if (event.getIsCorrect()) {
                                statistic.setTotalCorrectAnswers(statistic.getTotalCorrectAnswers() + 1);
                                statistic.setTotalScore(statistic.getTotalScore() + 1); // Assuming 1 point per correct answer
                            }
                            // Recalculate average score
                            statistic.setAverageScore((double) statistic.getTotalCorrectAnswers() / statistic.getTotalQuestionsAnswered());
                            userQuizSessionStatisticRepository.save(statistic);
                            log.debug("Updated statistic for user {} and quiz {}: {}", event.getUserId(), event.getQuizId(), statistic);
                        },
                        () -> {
                            // Create new statistic for the first answer in a session
                            UserQuizSessionStatistic newStatistic = UserQuizSessionStatistic.builder()
                                    .userId(event.getUserId())
                                    .quizId(event.getQuizId())
                                    .quizType(event.getQuizType())
                                    .totalSessions(0) // Session not completed yet
                                    .totalQuestionsAnswered(1)
                                    .totalCorrectAnswers(event.getIsCorrect() ? 1 : 0)
                                    .totalScore(event.getIsCorrect() ? 1 : 0)
                                    .averageScore(event.getIsCorrect() ? 1.0 : 0.0)
                                    .lastCompletedAt(Instant.now()) // Update on first answer
                                    .build();
                            userQuizSessionStatisticRepository.save(newStatistic);
                            log.debug("Created new statistic for user {} and quiz {}: {}", event.getUserId(), event.getQuizId(), newStatistic);
                        }
                );
    }

    @Transactional(readOnly = true)
    public Page<UserQuizSessionStatistic> getUserQuizStatistics(UUID userId, Pageable pageable) {
        return userQuizSessionStatisticRepository.findByUserId(userId, pageable);
    }
}
