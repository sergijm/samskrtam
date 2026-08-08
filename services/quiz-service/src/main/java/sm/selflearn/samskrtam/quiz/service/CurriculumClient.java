package sm.selflearn.samskrtam.quiz.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmPageDto;
import sm.selflearn.samskrtam.quiz.dto.ComposedSessionResponseDto;
import sm.selflearn.samskrtam.quiz.dto.QuestItemDto;
import sm.selflearn.samskrtam.quiz.dto.QuestPoolItemDto;
import sm.selflearn.samskrtam.quiz.dto.QuestSessionTopicDto;
import sm.selflearn.samskrtam.quiz.dto.TopicLessonDto;
import sm.selflearn.samskrtam.quiz.dto.TopicLessonSummaryDto;

import java.util.List;
import java.util.UUID;

/**
 * WebClient к curriculum-service (API v2) за готовыми QuestItem declension-семейства
 * (DECLENSION_FORM / DECLENSION_FORM_CHOICE / CASE_RECOGNITION / DECLENSION_MATCH).
 * Эти типы идут через этот клиент, см. quest-engine.md §5.
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
     * Случайная выборка готовых вопросов одного типа для темы, см.
     * curriculum-quest-items.md §5–6.
     */
    public Flux<QuestItemDto> fetchQuestItems(UUID topicId, String itemType, int limit) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v2/curriculum/quest-items")
                        .queryParam("topicId", topicId)
                        .queryParam("itemType", itemType)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("QUEST_ITEMS_NOT_FOUND",
                                "Quest items not found in curriculum-service for topic=" + topicId
                                        + " itemType=" + itemType)))
                .bodyToFlux(QuestItemDto.class);
    }

    /**
     * Fetch the materialized quest-item pool of a topic (id + itemType) for progress
     * selection (v2 contract, curriculum-session-composition.md §5 step 2).
     */
    public Mono<List<QuestPoolItemDto>> fetchTopicPool(String topicCode) {
        return webClient.get()
                .uri("/api/v2/curriculum/topics/{code}/quest-items", topicCode)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("TOPIC_NOT_FOUND",
                                "Topic pool not found in curriculum-service: " + topicCode)))
                .bodyToFlux(QuestPoolItemDto.class)
                .collectList();
    }

    /**
     * Fetch the grammar-lesson read model of a topic (metadata + quest items with
     * morphology attributes) from curriculum-service (v2). Curriculum-side replacement
     * of the removed content-service lesson data.
     */
    public Mono<TopicLessonDto> fetchTopicLesson(String topicCode) {
        return webClient.get()
                .uri("/api/v2/curriculum/topics/{code}/lesson", topicCode)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("TOPIC_NOT_FOUND",
                                "Topic lesson not found in curriculum-service: " + topicCode)))
                .bodyToMono(TopicLessonDto.class);
    }

    /**
     * Fetch the list of all lessons (lesson picker rows) from curriculum-service (v2).
     */
    public Mono<List<TopicLessonSummaryDto>> fetchLessons() {
        return webClient.get()
                .uri("/api/v2/curriculum/lessons")
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("LESSONS_NOT_FOUND",
                                "Lesson list not found in curriculum-service")))
                .bodyToFlux(TopicLessonSummaryDto.class)
                .collectList();
    }

    /**
     * Fetch one suppleive declension paradigm page (carousel) for a topic from
     * curriculum-service (v2). Returns an empty page for topics without one.
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

    /**
     * Compose a quiz session question sequence from topics (universal engine, v2 contract).
     * curriculum-service renders the materialized questions (prompt + correctAnswer +
     * distractors + payload) and returns them in random order. When a topic carries
     * {@code itemIds}, curriculum renders exactly those (progress-selected); otherwise a
     * random sample of {@code count} is used.
     *
     * @param topics     (topicCode, count[, itemIds]) pairs; grammar + lexical topics may be mixed
     * @param userLocale optional locale hint
     * @return composed sequence of ready-made questions
     */
    public Mono<ComposedSessionResponseDto> composeSession(List<QuestSessionTopicDto> topics, String userLocale) {
        return webClient.post()
                .uri("/api/v2/curriculum/sessions/compose")
                .bodyValue(java.util.Map.of(
                        "topics", topics,
                        "userLocale", userLocale == null ? "" : userLocale))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("COMPOSE_FAILED",
                                "Session composition failed in curriculum-service (http " + r.statusCode().value() + ")")))
                .bodyToMono(ComposedSessionResponseDto.class);
    }
}