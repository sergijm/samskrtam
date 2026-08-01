package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import sm.selflearn.samskrtam.content.dto.SangrahaDeclensionExamplesRequestDto;
import sm.selflearn.samskrtam.content.dto.SangrahaDeclensionExamplesResponseDto;
import sm.selflearn.samskrtam.content.dto.SangrahaVersesBatchRequestDto;
import sm.selflearn.samskrtam.content.dto.SangrahaVersesBatchResponseDto;

/**
 * HTTP-клиент к internal-эндпоинтам sangraha-service (§9 sangraha-service.md).
 * Использует RestClient с baseUrl = SANGRAHA_SERVICE_URL.
 * <p>
 * Два эндпоинта:
 * - POST /sangraha/internal/content/declension-examples — поиск примеров склонений
 * - POST /sangraha/internal/content/verses/batch — пакетный запрос стихов по ID
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SangrahaDeclensionExamplesClient {

    private final RestClient sangrahaRestClient;

    /**
     * Вызов sangraha-service для поиска примеров склонений по классу (vowelType, gender)
     * и набору ячеек (caseType, numberType). Возвращает verseId[] по каждой ячейке.
     */
    public SangrahaDeclensionExamplesResponseDto searchDeclensionExamples(SangrahaDeclensionExamplesRequestDto request) {
        log.debug("Calling sangraha declension-examples: vowelType={}, gender={}, cells={}",
                request.getVowelType(), request.getGender(), request.getCells().size());
        try {
            return sangrahaRestClient.post()
                    .uri("/sangraha/internal/content/declension-examples")
                    .body(request)
                    .retrieve()
                    .body(SangrahaDeclensionExamplesResponseDto.class);
        } catch (Exception e) {
            log.error("Failed to call sangraha declension-examples: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Пакетный запрос стихов по ID. Не найденные/не-ANALYZED/удалённые стихи
     * просто отсутствуют в ответе — без ошибки.
     */
    public SangrahaVersesBatchResponseDto fetchVersesBatch(SangrahaVersesBatchRequestDto request) {
        log.debug("Calling sangraha verses/batch: {} ids", request.getVerseIds().size());
        try {
            return sangrahaRestClient.post()
                    .uri("/sangraha/internal/content/verses/batch")
                    .body(request)
                    .retrieve()
                    .body(SangrahaVersesBatchResponseDto.class);
        } catch (Exception e) {
            log.error("Failed to call sangraha verses/batch: {}", e.getMessage(), e);
            throw e;
        }
    }
}