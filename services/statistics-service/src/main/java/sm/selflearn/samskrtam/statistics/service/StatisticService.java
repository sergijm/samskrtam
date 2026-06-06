package sm.selflearn.samskrtam.statistics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.events.AnswerSubmitted; // Import AnswerSubmitted event
import sm.selflearn.samskrtam.events.SessionCompleted;
import sm.selflearn.samskrtam.statistics.model.UserQuizSessionStatistic;
import sm.selflearn.samskrtam.statistics.repository.UserQuizSessionStatisticRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticService {

    private final UserQuizSessionStatisticRepository userQuizSessionStatisticRepository;

    @Transactional
    public void processSessionCompleted(SessionCompleted event) {
        log.info("Processing SessionCompleted event: {}", event);

        userQuizSessionStatisticRepository.findByUserIdAndQuizId(event.getUserId(), event.getQuizId())
                .ifPresentOrElse(
                        statistic -> {
                            // Update existing statistic
                            statistic.setTotalSessions(statistic.getTotalSessions() + 1);
                            statistic.setTotalQuestionsAnswered(statistic.getTotalQuestionsAnswered() + event.getTotalQuestions());
                            statistic.setTotalCorrectAnswers(statistic.getTotalCorrectAnswers() + event.getScore());
                            statistic.setTotalScore(statistic.getTotalScore() + event.getScore());
                            statistic.setLastCompletedAt(Instant.now());
                            // Recalculate average score
                            statistic.setAverageScore((double) statistic.getTotalCorrectAnswers() / statistic.getTotalQuestionsAnswered());
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
                            if (event.isCorrect()) {
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
                                    .totalCorrectAnswers(event.isCorrect() ? 1 : 0)
                                    .totalScore(event.isCorrect() ? 1 : 0)
                                    .averageScore(event.isCorrect() ? 1.0 : 0.0)
                                    .lastCompletedAt(Instant.now()) // Update on first answer
                                    .build();
                            userQuizSessionStatisticRepository.save(newStatistic);
                            log.debug("Created new statistic for user {} and quiz {}: {}", event.getUserId(), event.getQuizId(), newStatistic);
                        }
                );
    }

    @Transactional(readOnly = true)
    public List<UserQuizSessionStatistic> getUserQuizStatistics(UUID userId) {
        return userQuizSessionStatisticRepository.findByUserId(userId);
    }
}
