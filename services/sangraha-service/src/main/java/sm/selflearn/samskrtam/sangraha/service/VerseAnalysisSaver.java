package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.event.SangrahaVocabularyEvent;
import sm.selflearn.samskrtam.sangraha.model.*;
import sm.selflearn.samskrtam.sangraha.repository.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class VerseAnalysisSaver {

    private final VerseRepository verseRepository;
    private final VerseAnalysisRepository verseAnalysisRepository;
    private final VerseWordRepository verseWordRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Единственная транзакция анализа стиха. Порядок операций:
     * 1. Заполнить текст стиха (если не был введён вручную)
     * 2. Перезаписать VerseAnalysis (delete + insert)
     * 3. Пересоздать VerseWord[] (deleteAll + saveAll)
     * 4. Опубликовать OutboxEvent (rollback, если не удалось сериализовать/сохранить)
     * 5. Перевести статус ANALYZED (последним — гарантия атомарности)
     * Любой сбой на шагах 1–4 откатывает всю транзакцию — статус остаётся ANALYZING.
     */
    @Transactional
    public void saveResults(
            Verse verse, Work work, Chapter chapter,
            String textDevanagari, String textIast,
            String translationRu, String translationEn,
            JsonNode sandhiSplitsNode, JsonNode wordsNode,
            String rawResponse, String modelName
    ) {
        // 1. Заполняем текст стиха, если не был введён вручную
        if (verse.getTextDevanagari() == null || verse.getTextDevanagari().isBlank()) {
            verse.setTextDevanagari(textDevanagari);
        }
        if (verse.getTextIast() == null || verse.getTextIast().isBlank()) {
            verse.setTextIast(textIast);
        }

        // 2. Перезаписываем VerseAnalysis
        verseAnalysisRepository.deleteByVerseId(verse.getId());

        var analysis = VerseAnalysis.builder()
                .verseId(verse.getId())
                .translationRu(translationRu)
                .translationEn(translationEn)
                .sandhiSplits(sandhiSplitsNode.toString())
                .rawModelResponse(rawResponse)
                .modelName(modelName)
                .analyzedAt(Instant.now())
                .build();
        verseAnalysisRepository.save(analysis);

        // 3. Пересоздаём VerseWord[]
        verseWordRepository.deleteAllByVerseId(verse.getId());

        var words = buildWords(verse.getId(), wordsNode);
        verseWordRepository.saveAll(words);

        // 4. Публикуем OutboxEvent для Kafka-синхронизации с content-service
        //    Исключение при сериализации/сохранении летит вверх — транзакция откатывается целиком
        publishVocabularyEvent(verse, work, chapter, words);

        // 5. Статус ANALYZED — последним, гарантия атомарности
        verse.setStatus(VerseStatus.ANALYZED);
        verse.setUpdatedAt(Instant.now());
        verseRepository.save(verse);

        log.info("Verse {} analyzed successfully, {} words extracted", verse.getId(), words.size());
    }

    @Transactional
    public void markFailed(Verse verse) {
        verse.setStatus(VerseStatus.FAILED);
        verse.setUpdatedAt(Instant.now());
        verseRepository.save(verse);
    }

    /**
     * Возвращает статус в DRAFT — после технической ошибки сохранения (можно повторить).
     * Вызывается из VerseAnalysisService.analyze() при исключении из saveResults.
     */
    @Transactional
    public void revertToDraft(Verse verse) {
        verse.setStatus(VerseStatus.DRAFT);
        verse.setUpdatedAt(Instant.now());
        verseRepository.save(verse);
    }

    private List<VerseWord> buildWords(UUID verseId, JsonNode wordsNode) {
        var words = new ArrayList<VerseWord>();
        for (var w : wordsNode) {
            words.add(VerseWord.builder()
                    .verseId(verseId)
                    .position(w.get("position").asInt())
                    .surfaceIast(getString(w, "surfaceIast"))
                    .surfaceDevanagari(getString(w, "surfaceDevanagari"))
                    .lemmaIast(getString(w, "lemmaIast"))
                    .stem(getString(w, "stem"))
                    .root(getStringOrNull(w, "root"))
                    .pos(safeEnum(PartOfSpeech.class, getString(w, "pos")))
                    .gender(safeEnum(Gender.class, getString(w, "gender")))
                    .caseType(safeEnum(GrammaticalCase.class, getString(w, "caseType")))
                    .numberType(safeEnum(NumberType.class, getString(w, "numberType")))
                    .person(safeEnum(Person.class, getString(w, "person")))
                    .tense(safeEnum(Tense.class, getString(w, "tense")))
                    .mood(safeEnum(Mood.class, getString(w, "mood")))
                    .voice(safeEnum(Voice.class, getString(w, "voice")))
                    .glossRu(getString(w, "glossRu"))
                    .glossEn(getString(w, "glossEn"))
                    .build());
        }
        return words;
    }

    private void publishVocabularyEvent(Verse verse, Work work, Chapter chapter, List<VerseWord> words) {
        var vocabWords = words.stream()
                .map(w -> SangrahaVocabularyEvent.SangrahaVocabularyWord.builder()
                        .wordIast(w.getLemmaIast())
                        .wordDevanagari(w.getSurfaceDevanagari())
                        .stem(w.getStem())
                        .root(w.getRoot())
                        .gender(w.getGender() != null ? w.getGender().name() : null)
                        .translationRu(w.getGlossRu())
                        .translationEn(w.getGlossEn())
                        .build())
                .toList();

        var event = SangrahaVocabularyEvent.builder()
                .eventType("VERSE_VOCABULARY_EXTRACTED")
                .verseId(verse.getId())
                .workSlug(work.getSlug())
                .workTitleRu(work.getTitleRu())
                .workTitleEn(work.getTitleEn())
                .chapterSlug(chapter.getSlug())
                .chapterTitleRu(chapter.getTitleRu())
                .chapterTitleEn(chapter.getTitleEn())
                .words(vocabWords)
                .build();

        try {
            var payload = objectMapper.writeValueAsString(event);
            var outbox = OutboxEvent.builder()
                    .aggregateId(verse.getId())
                    .eventType("VERSE_VOCABULARY_EXTRACTED")
                    .payload(payload)
                    .status("PENDING")
                    .createdAt(Instant.now())
                    .build();
            outboxEventRepository.save(outbox);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize vocabulary event for verse " + verse.getId(), e);
        }
    }

    // ---- утилиты ----

    public static String getString(JsonNode node, String field) {
        var f = node.get(field);
        return (f != null && !f.isNull()) ? f.asText() : null;
    }

    public static String getStringOrNull(JsonNode node, String field) {
        var f = node.get(field);
        if (f == null || f.isNull() || f.asText().isBlank()) return null;
        return f.asText();
    }

    public static <T extends Enum<T>> T safeEnum(Class<T> enumClass, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown enum value '{}' for {}, using null", value, enumClass.getSimpleName());
            return null;
        }
    }
}