package sm.selflearn.samskrtam.quiz.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * WebClient to sangraha-service internal endpoints for declension examples.
 */
@Component
public class SangrahaClient {

    private final WebClient webClient;

    public SangrahaClient(WebClient.Builder webClientBuilder,
                          @Value("${sangraha-service.url:}") String sangrahaUrl) {
        this.webClient = webClientBuilder
                .baseUrl(sangrahaUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * POST /sangraha/internal/content/declension-examples
     */
    public Mono<SangrahaExamplesSearchResponse> searchExamples(
            String vowelType, String gender, int limitPerGroup, int maxPhraseWords,
            List<Map<String, String>> cells) {
        Map<String, Object> body = Map.of(
                "vowelType", vowelType,
                "gender", gender,
                "limitPerGroup", limitPerGroup,
                "maxPhraseWords", maxPhraseWords,
                "cells", cells);
        return webClient.post()
                .uri("/sangraha/internal/content/declension-examples")
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        r -> Mono.error(new SamskrtamException("SANGRAHA_EXAMPLES_ERROR",
                                "Sangraha examples search failed")))
                .bodyToMono(SangrahaExamplesSearchResponse.class);
    }

    /**
     * POST /sangraha/internal/content/verses/batch
     */
    public Mono<SangrahaVersesBatchResponse> fetchVersesBatch(List<UUID> verseIds) {
        return webClient.post()
                .uri("/sangraha/internal/content/verses/batch")
                .bodyValue(Map.of("verseIds", verseIds))
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        r -> Mono.error(new SamskrtamException("SANGRAHA_BATCH_ERROR",
                                "Sangraha verses batch failed")))
                .bodyToMono(SangrahaVersesBatchResponse.class);
    }
}