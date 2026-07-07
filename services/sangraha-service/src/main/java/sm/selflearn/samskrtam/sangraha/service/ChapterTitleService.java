package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.dto.CreateChapterRequest;
import sm.selflearn.samskrtam.sangraha.dto.UpdateChapterRequest;
import sm.selflearn.samskrtam.sangraha.model.Chapter;
import sm.selflearn.samskrtam.sangraha.repository.ChapterRepository;

import java.util.UUID;

import static sm.selflearn.samskrtam.sangraha.service.VerseAnalysisSaver.getString;

/**
 * Сервис для создания и обновления глав через LLM-анализ заголовка.
 * <p>
 * Шаг 1 — детекция языка по Unicode-диапазону (без LLM)
 * Шаг 2 — один вызов LLM с tool submit_chapter_metadata
 * Шаг 3 — детерминированная генерация slug: titleSaIast → SLP1
 * При коллизии slug (в пределах workId) добавляется числовой суффикс (-2, -3, ...).
 * Если orderIndex не передан — вычисляется как max(orderIndex) + 1.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterTitleService {

    private final ChapterRepository chapterRepository;
    private final TransliterationService transliterationService;
    private final ChapterMetadataClient chapterMetadataClient;
    private final ToolCallValidator toolCallValidator;
    private final JsonSchemas jsonSchemas;

    /**
     * Создание главы из сырого заголовка (POST /works/{workSlug}/chapters, §5.3).
     */
    @Transactional
    public Chapter createFromTitle(UUID workId, CreateChapterRequest request) {
        String detectedLanguage = transliterationService.detectLanguage(request.title());
        log.debug("Detected language for chapter title '{}': {}", request.title(), detectedLanguage);

        JsonNode llmResponse = chapterMetadataClient.call(detectedLanguage, request.title());
        if (llmResponse == null) {
            throw new RuntimeException("LLM call failed for chapter title: " + request.title());
        }

        JsonNode arguments = chapterMetadataClient.extractToolArguments(llmResponse);
        if (arguments == null) {
            throw new RuntimeException("LLM did not return submit_chapter_metadata tool call for: " + request.title());
        }

        if (!toolCallValidator.validate(arguments, jsonSchemas.getChapterMetadataSchema())) {
            throw new RuntimeException("LLM returned invalid metadata for chapter: " + request.title());
        }

        String titleRu = getString(arguments, "titleRu");
        String titleEn = getString(arguments, "titleEn");
        String titleSaIast = getString(arguments, "titleSaIast");
        String titleSaDevanagari = getString(arguments, "titleSaDevanagari");

        if (titleRu == null || titleEn == null || titleSaIast == null || titleSaDevanagari == null) {
            throw new RuntimeException("LLM returned incomplete chapter metadata (missing required title fields)");
        }

        // Поле языка, указанное пользователем, не перезаписывается
        switch (detectedLanguage) {
            case "RU" -> titleRu = request.title();
            case "EN" -> titleEn = request.title();
            case "SANSKRIT" -> {
                String script = transliterationService.detectScript(request.title());
                if ("devanagari".equals(script)) {
                    titleSaDevanagari = request.title();
                } else {
                    titleSaIast = request.title();
                }
            }
        }

        // Детерминированная генерация slug из titleSaIast (коллизия — per work)
        String baseSlug = transliterationService.iastToSlug(titleSaIast);
        String slug = baseSlug;
        int suffix = 2;
        while (chapterRepository.existsByWorkIdAndSlug(workId, slug)) {
            slug = baseSlug + "-" + suffix;
            suffix++;
        }

        // Вычисление orderIndex
        Integer orderIndex = request.orderIndex();
        if (orderIndex == null) {
            orderIndex = chapterRepository.findAllByWorkIdAndDeletedAtIsNullOrderByOrderIndexAsc(workId)
                    .stream()
                    .mapToInt(Chapter::getOrderIndex)
                    .max()
                    .orElse(0) + 1;
        }

        Chapter chapter = Chapter.builder()
                .workId(workId)
                .slug(slug)
                .orderIndex(orderIndex)
                .titleRu(titleRu)
                .titleEn(titleEn)
                .titleSaIast(titleSaIast)
                .titleSaDevanagari(titleSaDevanagari)
                .build();

        Chapter saved = chapterRepository.save(chapter);
        log.info("Created chapter: slug={}, titleRu={}, titleEn={}, workId={}",
                saved.getSlug(), saved.getTitleRu(), saved.getTitleEn(), workId);
        return saved;
    }

    /**
     * Обновление главы через title (PUT /chapters/{chapterId}, §5.3).
     * Если title передан — выполняется LLM-перевод и обновление titleRu/En/SaIast/SaDevanagari.
     * Если orderIndex передан — обновляется только он.
     */
    @Transactional
    public Chapter updateFromTitle(UUID chapterId, UpdateChapterRequest request, Chapter existingChapter) {
        if (request.title() != null && !request.title().isBlank()) {
            String detectedLanguage = transliterationService.detectLanguage(request.title());
            log.debug("Detected language for updated chapter title '{}': {}", request.title(), detectedLanguage);

            JsonNode llmResponse = chapterMetadataClient.call(detectedLanguage, request.title());
            if (llmResponse == null) {
                throw new RuntimeException("LLM call failed for chapter title update: " + request.title());
            }

            JsonNode arguments = chapterMetadataClient.extractToolArguments(llmResponse);
            if (arguments == null) {
                throw new RuntimeException("LLM did not return submit_chapter_metadata tool call for: " + request.title());
            }

            if (!toolCallValidator.validate(arguments, jsonSchemas.getChapterMetadataSchema())) {
                throw new RuntimeException("LLM returned invalid metadata for chapter update: " + request.title());
            }

            String titleRu = getString(arguments, "titleRu");
            String titleEn = getString(arguments, "titleEn");
            String titleSaIast = getString(arguments, "titleSaIast");
            String titleSaDevanagari = getString(arguments, "titleSaDevanagari");

            if (titleRu == null || titleEn == null || titleSaIast == null || titleSaDevanagari == null) {
                throw new RuntimeException("LLM returned incomplete chapter metadata (missing required title fields)");
            }

            switch (detectedLanguage) {
                case "RU" -> titleRu = request.title();
                case "EN" -> titleEn = request.title();
                case "SANSKRIT" -> {
                    String script = transliterationService.detectScript(request.title());
                    if ("devanagari".equals(script)) {
                        titleSaDevanagari = request.title();
                    } else {
                        titleSaIast = request.title();
                    }
                }
            }

            existingChapter.setTitleRu(titleRu);
            existingChapter.setTitleEn(titleEn);
            existingChapter.setTitleSaIast(titleSaIast);
            existingChapter.setTitleSaDevanagari(titleSaDevanagari);
        }

        if (request.orderIndex() != null) {
            existingChapter.setOrderIndex(request.orderIndex());
        }

        return chapterRepository.save(existingChapter);
    }
}