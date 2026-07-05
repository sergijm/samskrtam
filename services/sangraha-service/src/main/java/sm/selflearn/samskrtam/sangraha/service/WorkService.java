package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.dto.CreateWorkRequest;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.repository.WorkRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static sm.selflearn.samskrtam.sangraha.service.VerseAnalysisSaver.getString;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkService {

    private final WorkRepository workRepository;
    private final TransliterationService transliterationService;
    private final WorkMetadataClient workMetadataClient;
    private final ToolCallValidator toolCallValidator;
    private final JsonSchemas jsonSchemas;

    @Transactional(readOnly = true)
    public List<Work> getAllWorks() {
        return workRepository.findAllByDeletedAtIsNullOrderByCreatedAtAsc();
    }

    @Transactional(readOnly = true)
    public Work getWorkById(UUID id) {
        return workRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work not found: " + id));
    }

    @Transactional(readOnly = true)
    public Work getWorkBySlug(String slug) {
        return workRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new RuntimeException("Work not found by slug: " + slug));
    }

    /**
     * Get a work by UUID string or slug. Tries UUID parsing first,
     * falls back to slug lookup.
     */
    @Transactional(readOnly = true)
    public Work getWorkByIdOrSlug(String idOrSlug) {
        try {
            return getWorkById(UUID.fromString(idOrSlug));
        } catch (IllegalArgumentException e) {
            return getWorkBySlug(idOrSlug);
        }
    }

    @Transactional
    public Work createWork(Work work) {
        if (workRepository.existsBySlug(work.getSlug())) {
            throw new RuntimeException("Work with slug '" + work.getSlug() + "' already exists");
        }
        work.setCreatedAt(Instant.now());
        return workRepository.save(work);
    }

    /**
     * Создание произведения из сырого заголовка (POST /works, §5.2).
     * <p>
     * Шаг 1 — детекция языка по Unicode-диапазону (без LLM)
     * Шаг 2 — один вызов LLM с tool submit_work_metadata
     * Шаг 3 — детерминированная генерация slug: titleSaIast → SLP1
     * При коллизии slug добавляется числовой суффикс (-2, -3, ...).
     */
    @Transactional
    public Work createWorkFromTitle(CreateWorkRequest request) {
        // Шаг 1: детекция языка
        String detectedLanguage = transliterationService.detectLanguage(request.title());
        log.debug("Detected language for title '{}': {}", request.title(), detectedLanguage);

        // Шаг 2: LLM вызов
        JsonNode llmResponse = workMetadataClient.call(detectedLanguage, request.title(), request.description());
        if (llmResponse == null) {
            throw new RuntimeException("LLM call failed for work title: " + request.title());
        }

        JsonNode arguments = workMetadataClient.extractToolArguments(llmResponse);
        if (arguments == null) {
            throw new RuntimeException("LLM did not return submit_work_metadata tool call for: " + request.title());
        }

        // Валидация по JSON Schema
        if (!toolCallValidator.validate(arguments, jsonSchemas.getWorkMetadataSchema())) {
            throw new RuntimeException("LLM returned invalid metadata for: " + request.title());
        }

        String titleRu = getString(arguments, "titleRu");
        String titleEn = getString(arguments, "titleEn");
        String titleSaIast = getString(arguments, "titleSaIast");
        String titleSaDevanagari = getString(arguments, "titleSaDevanagari");
        String author = getString(arguments, "author");
        String descriptionRu = getString(arguments, "descriptionRu");
        String descriptionEn = getString(arguments, "descriptionEn");

        if (titleRu == null || titleEn == null || titleSaIast == null || titleSaDevanagari == null) {
            throw new RuntimeException("LLM returned incomplete metadata (missing required title fields)");
        }

        // Поле языка, указанное пользователем, не перезаписывается
        switch (detectedLanguage) {
            case "RU" -> titleRu = request.title();
            case "EN" -> titleEn = request.title();
            case "SANSKRIT" -> {
                // Определяем по Unicode-диапазону: деванагари или IAST
                String script = transliterationService.detectScript(request.title());
                if ("devanagari".equals(script)) {
                    titleSaDevanagari = request.title();
                } else {
                    titleSaIast = request.title();
                }
            }
        }

        // Если пользователь передал описание — подставляем в нужное поле
        if (request.description() != null && !request.description().isBlank()) {
            if ("RU".equals(detectedLanguage)) {
                descriptionRu = request.description();
            } else if ("EN".equals(detectedLanguage)) {
                descriptionEn = request.description();
            } else {
                // SANSKRIT — кладём в descriptionRu, LLM дополнит descriptionEn
                descriptionRu = request.description();
            }
        }

        // author — nullable: если LLM вернула null, не проставляем
        if (author != null && author.isBlank()) {
            author = null;
        }

        // Шаг 3: детерминированная генерация slug из titleSaIast
        String baseSlug = transliterationService.iastToSlug(titleSaIast);
        String slug = baseSlug;
        int suffix = 2;
        while (workRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + suffix;
            suffix++;
        }

        // Создаём Work
        Work work = Work.builder()
                .slug(slug)
                .titleRu(titleRu)
                .titleEn(titleEn)
                .titleSaIast(titleSaIast)
                .titleSaDevanagari(titleSaDevanagari)
                .descriptionRu(descriptionRu)
                .descriptionEn(descriptionEn)
                .author(author)
                .createdAt(Instant.now())
                .build();

        Work saved = workRepository.save(work);
        log.info("Created work: slug={}, titleRu={}, titleEn={}, author={}",
            saved.getSlug(), saved.getTitleRu(), saved.getTitleEn(), saved.getAuthor());
        return saved;
    }

    @Transactional
    public Work updateWork(UUID id, Work update) {
        Work work = getWorkById(id);
        work.setTitleRu(update.getTitleRu());
        work.setTitleEn(update.getTitleEn());
        work.setTitleSaIast(update.getTitleSaIast());
        work.setTitleSaDevanagari(update.getTitleSaDevanagari());
        work.setDescriptionRu(update.getDescriptionRu());
        work.setDescriptionEn(update.getDescriptionEn());
        work.setAuthor(update.getAuthor());
        return workRepository.save(work);
    }

    @Transactional
    public Work updateWorkBySlug(String slug, Work update) {
        Work work = getWorkBySlug(slug);
        work.setTitleRu(update.getTitleRu());
        work.setTitleEn(update.getTitleEn());
        work.setTitleSaIast(update.getTitleSaIast());
        work.setTitleSaDevanagari(update.getTitleSaDevanagari());
        work.setDescriptionRu(update.getDescriptionRu());
        work.setDescriptionEn(update.getDescriptionEn());
        work.setAuthor(update.getAuthor());
        return workRepository.save(work);
    }

    @Transactional
    public void deleteWork(UUID id) {
        Work work = getWorkById(id);
        work.setDeletedAt(Instant.now());
        workRepository.save(work);
    }

    @Transactional
    public void deleteWorkBySlug(String slug) {
        Work work = getWorkBySlug(slug);
        work.setDeletedAt(Instant.now());
        workRepository.save(work);
    }
}