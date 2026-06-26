package sm.selflearn.samskrtam.statistics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.statistics.dto.UserQuizStatisticDto;
import sm.selflearn.samskrtam.statistics.model.UserQuizSessionStatistic;
import sm.selflearn.samskrtam.statistics.repository.UserQuizSessionStatisticRepository;

import java.util.Optional;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticService {
    private final UserQuizSessionStatisticRepository statisticRepository;

    public Page<UserQuizSessionStatistic> getUserQuizStatistics(UUID userId, Pageable pageable) {
        return statisticRepository.findByUserId(userId, pageable);
    }

    public Optional<UserQuizSessionStatistic> getUserQuizStatistic(UUID userId, UUID quizId) {
        return statisticRepository.findByUserIdAndQuizId(userId, quizId);
    }
    public UserQuizStatisticDto toDto(UserQuizSessionStatistic statistic) {
        return UserQuizStatisticDto.builder()
                .quizId(statistic.getQuizId())
                .lessonType(statistic.getLessonType())
                .totalSessions(statistic.getTotalSessions())
                .totalQuestionsAnswered(statistic.getTotalQuestionsAnswered())
                .totalCorrectAnswers(statistic.getTotalCorrectAnswers())
                .totalScore(statistic.getTotalScore())
                .averageScore(statistic.getAverageScore())
                .lastCompletedAt(statistic.getLastCompletedAt())
                .build();
    }
}

