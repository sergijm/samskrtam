package sm.selflearn.samskrtam.quiz.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmPageDto;
import sm.selflearn.samskrtam.quiz.dto.QuestItemDto;
import sm.selflearn.samskrtam.quiz.dto.TopicDto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * WebClient к curriculum-service (API v2).
 */
@Component
public class CurriculumClient {

    private final WebClient webClient;

    public CurriculumClient(WebClient.Builder webClientBuilder,
                            @Value("${curriculum-service.base-url:http://curriculum-service:8091}") String curriculumBaseUrl) {
        this.webClient = webClientBuilder
                .baseUrl(curriculumBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Select one quest item per (progress_tag, item_type, answer_mode) group
     * via the curriculum window-function endpoint.
     *
     * @param topicCode    topic code
     * @param progressTags null/empty = all tags; otherwise filter by these
     * @param itemType     optional item type filter
     * @param answerMode   optional answer mode filter
     * @param limit        0 = no limit
     * @return full QuestItemDto list (prompt, distractors, payload included)
     */
    public Mono<List<QuestItemDto>> selectQuestItems(
            String topicCode,
            List<String> progressTags,
            String itemType,
            String answerMode,
            int limit) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("progressTags", progressTags);
        body.put("itemType", itemType);
        body.put("answerMode", answerMode);
        body.put("limit", limit);

        return webClient.post()
                .uri("/api/v2/curriculum/quest-items/select?topicCode={code}", topicCode)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("SELECT_FAILED",
                                "Quest item selection failed for topic=" + topicCode)))
                .bodyToFlux(QuestItemDto.class)
                .collectList();
    }

    /**
     * Fetch topics from curriculum-service (v2), optionally filtered by domain.
     */
    public Mono<List<TopicDto>> fetchTopics(String domain) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/v2/curriculum/topics");
                    if (domain != null) {
                        uriBuilder.queryParam("domain", domain);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("TOPICS_NOT_FOUND",
                                "Topics not found in curriculum-service")))
                .bodyToFlux(TopicDto.class)
                .collectList();
    }

    /**
     * Fetch quest items for a topic — all types, no limit.
     */
    public Mono<List<QuestItemDto>> fetchAllQuestItems(UUID topicId) {
        return fetchQuestItemsByTopic(topicId, null);
    }

    /**
     * Fetch quest items for a topic by item type (v2).
     */
    public Mono<List<QuestItemDto>> fetchQuestItemsByTopic(UUID topicId, String itemType) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/v2/curriculum/quest-items")
                            .queryParam("topicId", topicId);
                    if (itemType != null) {
                        uriBuilder.queryParam("itemType", itemType);
                    }
                    uriBuilder.queryParam("limit", 10000);
                    return uriBuilder.build();
                })
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("QUEST_ITEMS_NOT_FOUND",
                                "Quest items not found for topic=" + topicId)))
                .bodyToFlux(QuestItemDto.class)
                .collectList();
    }

    /**
     * Fetch one declension paradigm page for a topic from curriculum-service (v2).
     */
    public Mono<DeclensionParadigmPageDto> fetchParadigmPage(String topicCode, int index) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v2/curriculum/topics/{topicCode}/declension-paradigms")
                        .queryParam("index", index)
                        .build(topicCode))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("TOPIC_NOT_FOUND",
                                "Paradigm page not found in curriculum-service for topic=" + topicCode)))
                .bodyToMono(DeclensionParadigmPageDto.class);
    }
}