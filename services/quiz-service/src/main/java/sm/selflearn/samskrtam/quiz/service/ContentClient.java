package sm.selflearn.samskrtam.quiz.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.DeclensionFormDto;
import sm.selflearn.samskrtam.content.dto.QuizSummaryDto;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizData; // Corrected import
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.dto.GeneratedQuizQuestionDto;

import java.util.List;
import java.util.UUID;

@Component
public class ContentClient {

    private final WebClient webClient;
    private final String contentBaseUrl;

    public ContentClient(WebClient webClient, @Value("${content.service.url}") String contentBaseUrl) {
        this.webClient = webClient;
        this.contentBaseUrl = contentBaseUrl;
    }

    public Mono<GeneratedQuizData> generateQuizData(UUID quizId) { // Renamed method and changed return type
        return webClient.post() // Changed to POST
                .uri(contentBaseUrl + "/api/v1/content/quizzes/{id}/generate-quiz-data", quizId) // Updated URI
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("QUIZ_NOT_FOUND", "Quiz not found in content-service: " + quizId)))
                .bodyToMono(GeneratedQuizData.class); // Updated return type
    }

    public Mono<GeneratedQuizData> getGeneratedQuizData(UUID generatedQuizDataId) { // New method
        return webClient.get()
                .uri(contentBaseUrl + "/api/v1/content/generated-quiz-data/{id}", generatedQuizDataId) // New URI
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("GENERATED_QUIZ_DATA_NOT_FOUND", "Generated quiz data not found in content-service: " + generatedQuizDataId)))
                .bodyToMono(GeneratedQuizData.class);
    }

    public Mono<GeneratedQuizQuestionDto> getGeneratedQuestion(UUID questionId) {
        return webClient.get()
                .uri(contentBaseUrl + "/api/v1/content/generated-questions/{id}", questionId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("QUESTION_NOT_FOUND", "Generated question not found in content-service: " + questionId)))
                .bodyToMono(GeneratedQuizQuestionDto.class);
    }

    // Removed getGeneratedQuestionsForSession as it's replaced by getGeneratedQuizData

    public Mono<List<DeclensionFormDto>> getDeclensionForms(UUID declensionStemId) {
        return webClient.get()
                .uri(contentBaseUrl + "/api/v1/content/declension-stems/{id}/forms", declensionStemId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("DECLENSION_STEM_NOT_FOUND", "Declension stem not found in content-service: " + declensionStemId)))
                .bodyToFlux(DeclensionFormDto.class)
                .collectList();
    }

    public Mono<List<VocabularyWordDto>> getVocabularyWordsForQuiz(UUID quizId, int limit) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(contentBaseUrl + "/api/v1/content/quizzes/{quizId}/vocabulary-words")
                        .queryParam("limit", limit)
                        .build(quizId))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("VOCABULARY_WORDS_NOT_FOUND", "Vocabulary words not found for quiz: " + quizId)))
                .bodyToFlux(VocabularyWordDto.class)
                .collectList();
    }

    public Mono<QuizSummaryDto> getQuizSummary(UUID quizId) {
        return webClient.get()
                .uri(contentBaseUrl + "/api/v1/content/quizzes/{id}/summary", quizId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("QUIZ_NOT_FOUND", "Quiz summary not found in content-service: " + quizId)))
                .bodyToMono(QuizSummaryDto.class);
    }
}
