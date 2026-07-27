package sm.selflearn.samskrtam.sangraha.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import sm.selflearn.samskrtam.content.dto.SangrahaVocabularyResponse;
import sm.selflearn.samskrtam.sangraha.event.SangrahaVocabularyEvent;

/**
 * Синхронный клиент content-service — вызывается только по кнопке «Изучить»
 * (VocabularyQuizService), никакого Outbox/фоновых ретраев больше нет.
 */
@Slf4j
@Component
public class ContentServiceVocabularyClient {

    private final RestClient restClient;

    public ContentServiceVocabularyClient(@Value("${app.content-service.url}") String contentServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(contentServiceUrl)
                .build();
    }

    public SangrahaVocabularyResponse requestVocabularyQuiz(SangrahaVocabularyEvent request) {
        return restClient.post()
                .uri("/content/internal/sangraha/vocabulary-quiz")
                .body(request)
                .retrieve()
                .body(SangrahaVocabularyResponse.class);
    }
}
