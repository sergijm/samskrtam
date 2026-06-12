package sm.selflearn.samskrtam.statistics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import sm.selflearn.samskrtam.common.dto.PaginatedResponse; // Import PaginatedResponse
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

    @GetMapping(value = "/users/{userId}/quizzes", produces = "application/json") // Added produces
    @Operation(summary = "Get all quiz statistics for a user with pagination")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated user quiz statistics")
    public PaginatedResponse<UserQuizStatisticDto> getUserQuizStatistics(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "lastCompletedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<sm.selflearn.samskrtam.statistics.model.UserQuizSessionStatistic> statisticsPage = statisticService.getUserQuizStatistics(userId, pageable);

        List<UserQuizStatisticDto> dtoList = statisticsPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PaginatedResponse.<UserQuizStatisticDto>builder()
                .content(dtoList)
                .totalPages(statisticsPage.getTotalPages())
                .totalElements(statisticsPage.getTotalElements())
                .currentPage(statisticsPage.getNumber())
                .pageSize(statisticsPage.getSize())
                .isFirst(statisticsPage.isFirst())
                .isLast(statisticsPage.isLast())
                .build();
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
                .answerHistoryJson(statistic.getAnswerHistoryJson()) // Include the new field
                .build();
    }
}
