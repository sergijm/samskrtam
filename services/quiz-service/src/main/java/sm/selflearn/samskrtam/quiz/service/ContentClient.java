package sm.selflearn.samskrtam.quiz.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.DeclensionFormDto;
import sm.selflearn.samskrtam.content.dto.DeclensionStemDto;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizData;
import sm.selflearn.samskrtam.content.dto.LessonItemResponse;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.content.dto.CaseEndingDto;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ContentClient {

    public Mono<Set<UUID>> getVocabularyWordIdsForLesson(String slug) {
        return webClient.get()
                .uri("/api/v1/content/lessons/{slug}/vocabulary-word-ids", slug)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("VOCABULARY_WORDS_NOT_FOUND",
                                "Vocabulary word IDs not found for lesson slug: " + slug)))
                .bodyToFlux(UUID.class)
                .collect(Collectors.toSet());
    }

    private final WebClient webClient;

    public ContentClient(WebClient.Builder webClientBuilder,
                         @Value("${content.service.url:http://content-service:8081}") String contentBaseUrl) {
        this.webClient = webClientBuilder
                .baseUrl(contentBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<List<LessonItemResponse>> getQuizzesByCategory(String category) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/content/lessons")
                        .queryParam("category", category)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("CATEGORY_NOT_FOUND", "No quizzes found for category: " + category)))
                .bodyToFlux(LessonItemResponse.class)
                .collectList();
    }

    public Mono<GeneratedQuizData> generateQuizData(UUID lessonId, String userLocale) {
        return generateQuizData(lessonId, userLocale, null, null, null, null);
    }

    public Mono<GeneratedQuizData> generateQuizData(UUID lessonId, String userLocale,
                                                    String filterScope, String filterCaseTypes, String filterNumberTypes, String filterCombinations) {
        return webClient.post()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/api/v1/content/lessons/{id}/generate-quiz-data");
                    if (filterScope != null) builder.queryParam("filterScope", filterScope);
                    if (filterCaseTypes != null) builder.queryParam("filterCaseTypes", filterCaseTypes);
                    if (filterNumberTypes != null) builder.queryParam("filterNumberTypes", filterNumberTypes);
                    if (filterCombinations != null) builder.queryParam("filterCombinations", filterCombinations);
                    return builder.build(lessonId);
                })
                .header("X-User-Locale", userLocale)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("LESSON_NOT_FOUND", "Lesson not found in content-service: " + lessonId)))
                .bodyToMono(GeneratedQuizData.class);
    }

    public Mono<List<DeclensionFormDto>> getDeclensionForms(UUID declensionStemId) {
        return webClient.get()
                .uri("/api/v1/content/declension-stems/{id}/forms", declensionStemId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("DECLENSION_STEM_NOT_FOUND", "Declension stem not found in content-service: " + declensionStemId)))
                .bodyToFlux(DeclensionFormDto.class)
                .collectList();
    }

    public Mono<List<VocabularyWordDto>> getVocabularyWordsForLesson(UUID lessonId, int limit) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/content/lessons/{lessonId}/vocabulary-words")
                        .queryParam("limit", limit)
                        .build(lessonId))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("VOCABULARY_WORDS_NOT_FOUND", "Vocabulary words not found for lesson: " + lessonId)))
                .bodyToFlux(VocabularyWordDto.class)
                .collectList();
    }

    public Mono<VocabularyWordDto> getVocabularyWordById(UUID wordId) {
        return webClient.get()
                .uri("/api/v1/content/vocabulary/words/{wordId}", wordId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("VOCABULARY_WORD_NOT_FOUND", "Vocabulary word not found in content-service: " + wordId)))
                .bodyToMono(VocabularyWordDto.class);
    }

    public Mono<LessonItemResponse> getLessonItem(UUID lessonId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/content/lessons")
                        .queryParam("id", lessonId)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("LESSON_NOT_FOUND", "Lesson summary not found in content-service: " + lessonId)))
                .bodyToMono(LessonItemResponse.class);
    }

    public Mono<LessonItemResponse> getLessonItemBySlug(String slug) {
        return webClient.get()
                .uri("/api/v1/content/lessons/{slug}", slug)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("LESSON_NOT_FOUND", "Lesson summary not found for slug: " + slug)))
                .bodyToMono(LessonItemResponse.class);
    }

    public Mono<List<DeclensionStemDto>> getDeclensionStemsForLesson(String slug) {
        return webClient.get()
                .uri("/api/v1/content/lessons/{slug}/declension-stems", slug)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("STEMS_NOT_FOUND", "Declension stems not found for lesson slug: " + slug)))
                .bodyToFlux(DeclensionStemDto.class)
                .collectList();
    }

    public Mono<List<CaseEndingDto>> getCaseEndingsByVowelType(String vowelType) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/content/case-endings")
                        .queryParam("vowelType", vowelType)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("CASE_ENDINGS_NOT_FOUND",
                                "Case endings not found for vowel type: " + vowelType)))
                .bodyToFlux(CaseEndingDto.class)
                .collectList();
    }

    public Mono<List<CaseEndingDto>> getCaseEndingsForLesson(
            String slug,
            String caseType,
            String numberType,
            String gender) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/api/v1/content/lessons/{slug}/case-endings");
                    if (caseType != null) builder.queryParam("caseType", caseType);
                    if (numberType != null) builder.queryParam("numberType", numberType);
                    if (gender != null) builder.queryParam("gender", gender);
                    return builder.build(slug);
                })
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("CASE_ENDINGS_NOT_FOUND",
                                "Case endings not found for lesson slug: " + slug)))
                .bodyToFlux(CaseEndingDto.class)
                .collectList();
    }

}
