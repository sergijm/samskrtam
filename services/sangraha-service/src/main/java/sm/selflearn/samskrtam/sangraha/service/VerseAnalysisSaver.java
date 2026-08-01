package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
    private final ObjectMapper objectMapper;

    /**
     * Единственная транзакция анализа стиха. Порядок операций:
     * 1. Заполнить текст стиха (если не был введён вручную)
     * 2. Перезаписать VerseAnalysis (delete + insert)
     * 3. Пересоздать VerseWord[] (deleteAll + saveAll)
     * 4. Перевести статус ANALYZED (последним — гарантия атомарности)
     * Любой сбой на шагах 1–3 откатывает всю транзакцию — статус остаётся ANALYZING.
     */
        @Transactional
    public void saveResults(
            Verse verse, Work work, Chapter chapter,
            String textDevanagari, String textIast,
            String translationRu, String translationEn,
            JsonNode sandhiSplitsNode, JsonNode wordsNode,
            String rawResponse, String modelName, String analyzerName
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
                .analyzerName(analyzerName)
                .analyzedAt(Instant.now())
                .build();
        verseAnalysisRepository.save(analysis);

        // 3. Пересоздаём VerseWord[]
        verseWordRepository.deleteAllByVerseId(verse.getId());

        var words = buildWords(verse.getId(), wordsNode);
        verseWordRepository.saveAll(words);

        // 4. Статус ANALYZED — последним, гарантия атомарности
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
            var word = VerseWord.builder()
                    .verseId(verseId)
                    .position(w.get("position").asInt())
                    .surfaceIast(getString(w, "surfaceIast"))
                    .surfaceDevanagari(getString(w, "surfaceDevanagari"))
                    .lemmaIast(getString(w, "lemmaIast"))
                    .stem(getStringOrNull(w, "stem"))
                    .root(getStringOrNull(w, "root"))
                    .pos(safeEnum(PartOfSpeech.class, getString(w, "pos")))
                    .formType(safeEnum(FormType.class, getString(w, "formType")))
                    .isFinite(getBooleanOrNull(w, "isFinite"))
                    .lemmaGlossRu(getStringOrNull(w, "lemmaGlossRu"))
                    .lemmaGlossEn(getStringOrNull(w, "lemmaGlossEn"))
                    .contextGlossRu(getString(w, "glossRu"))
                    .contextGlossEn(getString(w, "glossEn"))
                    .formationRuleNumbers(getArrayAsString(w, "formationRuleNumbers"))
                    .analysisConfidence(safeEnum(AnalysisConfidence.class, getString(w, "analysisConfidence")))
                    .ambiguityNotes(getStringOrNull(w, "ambiguityNotes"))
                    .build();

            // Morphology from nested object
            JsonNode morphNode = w.get("morphology");
            if (morphNode != null && morphNode.isObject() && hasAnyNonNull(morphNode)) {
                var morph = VerseWordMorphology.builder()
                        .verseWord(word)
                        .caseType(safeEnum(GrammaticalCase.class, getString(morphNode, "case")))
                        .gender(safeEnum(Gender.class, getString(morphNode, "gender")))
                        .numberType(safeEnum(NumberType.class, getString(morphNode, "number")))
                        .person(safeEnum(Person.class, getString(morphNode, "person")))
                        .tense(safeEnum(Tense.class, getString(morphNode, "tense")))
                        .mood(safeEnum(Mood.class, getString(morphNode, "mood")))
                        .voice(safeEnum(Voice.class, getString(morphNode, "voice")))
                        .build();
                word.setMorphology(morph);
            }

            // Derivation from flat fields + nested derivation.description
            String derivationTypeStr = getString(w, "derivationType");
            String derivationalSuffix = getStringOrNull(w, "derivationalSuffix");
            String derivationalBase = getStringOrNull(w, "derivationalBase");
            JsonNode derivNode = w.get("derivation");
            String derivationDescription = null;
            if (derivNode != null && derivNode.isObject()) {
                derivationDescription = getStringOrNull(derivNode, "description");
            }

            if (derivationTypeStr != null || derivationalSuffix != null
                    || derivationalBase != null || derivationDescription != null) {
                var deriv = VerseWordDerivation.builder()
                        .verseWord(word)
                        .derivationType(safeEnum(DerivationType.class, derivationTypeStr))
                        .derivationalSuffix(derivationalSuffix)
                        .derivationalBase(derivationalBase)
                        .description(derivationDescription)
                        .build();
                word.setDerivation(deriv);
            }

            words.add(word);
        }
        return words;
    }

    private static boolean hasAnyNonNull(JsonNode node) {
        if (node == null || !node.isObject()) return false;
        var fields = node.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            if (!entry.getValue().isNull()) return true;
        }
        return false;
    }

    private static Boolean getBooleanOrNull(JsonNode node, String field) {
        var f = node.get(field);
        if (f == null || f.isNull()) return null;
        if (f.isBoolean()) return f.asBoolean();
        if (f.isTextual()) {
            String val = f.asText();
            if ("true".equalsIgnoreCase(val)) return true;
            if ("false".equalsIgnoreCase(val)) return false;
        }
        return null;
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

    /**
     * Сериализует JSON-массив целых чисел из node[field] в строку для TEXT-колонки.
     */
    public static String getArrayAsString(JsonNode node, String field) {
        var arr = node.get(field);
        if (arr == null || !arr.isArray() || arr.isEmpty()) {
            return null;
        }
        try {
            return new ObjectMapper().writeValueAsString(arr);
        } catch (Exception e) {
            log.warn("Failed to serialize array field '{}': {}", field, e.getMessage());
            return null;
        }
    }
}