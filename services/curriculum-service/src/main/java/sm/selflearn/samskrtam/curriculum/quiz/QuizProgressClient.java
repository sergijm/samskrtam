package sm.selflearn.samskrtam.curriculum.quiz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * HTTP-клиент к quiz-service за «реальным» прогрессом пользователя.
 *
 * <p>Прогресс (таблица {@code quiz.quiz_item_score}) — зона ответственности
 * quiz-service, поэтому curriculum-service при формировании карты обучения
 * обращается сюда, а не считает прогресс сам.
 *
 * <p>Если {@code quiz-service.url} не задан — клиент отключён, прогресс
 * считается нулевым (как для анонима).
 */
@Component
public class QuizProgressClient {

    private final RestClient restClient;
    private final boolean enabled;

    public QuizProgressClient(RestClient.Builder builder,
                             @Value("${quiz-service.url:}") String quizServiceUrl) {
        this.enabled = quizServiceUrl != null && !quizServiceUrl.isBlank();
        this.restClient = enabled ? builder.baseUrl(quizServiceUrl).build() : null;
    }

    /**
     * Пакетно получить оценки прогресса (0..100) по progress_tag для одного itemType.
     *
     * @return карта progress_tag → оценка; пустая, если клиент отключён или userId нет
     */
    public Map<String, Integer> bulkScores(java.util.UUID userId, String itemType, List<String> progressTags) {
        if (!enabled || userId == null || progressTags == null || progressTags.isEmpty()) {
            return Map.of();
        }
        try {
            BulkProgressResponse response = restClient.post()
                    .uri("/api/v2/quiz/progress/bulk")
                    .header("X-User-Id", userId.toString())
                    .body(new BulkProgressRequest(itemType, progressTags))
                    .retrieve()
                    .body(BulkProgressResponse.class);
            return response != null && response.getScores() != null ? response.getScores() : Map.of();
        } catch (Exception e) {
            return Map.of();
        }
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BulkProgressRequest {
        private String itemType;
        private List<String> progressTags;

        public BulkProgressRequest() {
        }

        public BulkProgressRequest(String itemType, List<String> progressTags) {
            this.itemType = itemType;
            this.progressTags = progressTags;
        }
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BulkProgressResponse {
        private String itemType;
        private Map<String, Integer> scores;
    }
}
