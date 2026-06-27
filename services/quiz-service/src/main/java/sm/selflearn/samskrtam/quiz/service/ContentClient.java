package sm.selflearn.samskrtam.quiz.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.*;

import java.util.List;
import java.util.UUID;

@Component
public class ContentClient {

    private final WebClient webClient;

    public ContentClient(WebClient.Builder webClientBuilder,
                         @Value("${content.service.url:http://content-service:8081}") String contentBaseUrl) {
        this.webClient = webClientBuilder
                .baseUrl(contentBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<GeneratedQuizData> generateQuizData(UUID lessonId, String userLocale) {
        return webClient.post()
                .uri("/api/v1/content/lessons/{id}/generate-quiz-data", lessonId)
                .header("X-User-Locale", userLocale)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("LESSON_NOT_FOUND", "Lesson not found in content-service: " + lessonId)))
                .bodyToMono(GeneratedQuizData.class);
    }

    public Mono<GeneratedQuizData> getGeneratedQuizData(UUID generatedQuizDataId) {
        return webClient.get()
                .uri("/api/v1/content/generated-quiz-data/{id}", generatedQuizDataId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("GENERATED_QUIZ_DATA_NOT_FOUND", "Generated quiz data not found in content-service: " + generatedQuizDataId)))
                .bodyToMono(GeneratedQuizData.class);
    }

    public Mono<GeneratedQuizQuestionDto> getGeneratedQuestion(UUID questionId) {
        return webClient.get()
                .uri("/api/v1/content/generated-questions/{id}", questionId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("QUESTION_NOT_FOUND", "Generated question not found in content-service: " + questionId)))
                .bodyToMono(GeneratedQuizQuestionDto.class);
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
                .uri("/api/v1/content/lessons/{id}/summary", lessonId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("LESSON_NOT_FOUND", "Lesson summary not found in content-service: " + lessonId)))
                .bodyToMono(LessonItemResponse.class);
    }

    public Mono<LessonItemResponse> getLessonItemBySlug(String slug) {
        return webClient.get()
                .uri("/api/v1/content/lessons/by-slug/{slug}", slug)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("LESSON_NOT_FOUND", "Lesson summary not found for slug: " + slug)))
                .bodyToMono(LessonItemResponse.class);
    }
}
