package sm.selflearn.samskrtam.statistics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sm.selflearn.samskrtam.statistics.dto.UserQuizStatisticDto;
import sm.selflearn.samskrtam.statistics.service.StatisticService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/statistics")
@Tag(name = "Statistics", description = "APIs for user statistics")
@RequiredArgsConstructor
public class StatisticController {

    private final StatisticService statisticService;

    @GetMapping("/users/{userId}/quizzes")
    @Operation(summary = "Get all quiz statistics for a user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved user quiz statistics")
    public List<UserQuizStatisticDto> getUserQuizStatistics(@PathVariable UUID userId) {
        return statisticService.getUserQuizStatistics(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private UserQuizStatisticDto mapToDto(sm.selflearn.samskrtam.statistics.model.UserQuizSessionStatistic statistic) {
        return UserQuizStatisticDto.builder()
                .quizId(statistic.getQuizId())
                .quizType(statistic.getQuizType())
                .totalSessions(statistic.getTotalSessions())
                .totalQuestionsAnswered(statistic.getTotalQuestionsAnswered())
                .totalCorrectAnswers(statistic.getTotalCorrectAnswers())
                .totalScore(statistic.getTotalScore())
                .averageScore(statistic.getAverageScore())
                .lastCompletedAt(statistic.getLastCompletedAt())
                .build();
    }
}
