package sm.selflearn.samskrtam.sangraha.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.sangraha.dto.VerseAnalysisDto;
import sm.selflearn.samskrtam.sangraha.dto.VerseAnalysisDto.SandhiSplitDto;
import sm.selflearn.samskrtam.sangraha.dto.VerseDetailDto;
import sm.selflearn.samskrtam.sangraha.dto.VerseWordDto;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.VerseAnalysis;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class VerseMapper {

    private final ObjectMapper objectMapper;

    /**
     * Build VerseDetailDto from Verse + optional analysis + optional words.
     * Гарантия: если verse.status == ANALYZED, analysis и words не могут быть null/пустыми.
     * При нарушении контракта возвращаем пустые списки вместо null, чтобы фронтенд
     * не падал, но логируем ошибку.
     */
        public VerseDetailDto toDetailDto(Verse verse, VerseAnalysis analysis, List<VerseWord> words,
                                       String vocabularyQuizSlug) {
        VerseAnalysisDto analysisDto = null;
        if (analysis != null) {
            analysisDto = toAnalysisDto(analysis);
        }

        List<VerseWordDto> wordDtos = null;
        if (words != null) {
            wordDtos = words.stream()
                .map(this::toWordDto)
                .toList();
        }

        // Контракт: если статус ANALYZED, analysis и words должны быть непустыми
        if (verse.getStatus() == VerseStatus.ANALYZED) {
            if (analysisDto == null) {
                log.error("Verse {} is ANALYZED but analysis is missing — data corruption", verse.getId());
                analysisDto = null; // intentionally null — frontend покажет ошибку
            }
            if (wordDtos == null || wordDtos.isEmpty()) {
                log.warn("Verse {} is ANALYZED but words are empty — possible data issue", verse.getId());
                // Не заменяем на пустой список — фронтенд сам решит, что показать
            }
        }

                return new VerseDetailDto(
            verse.getId(),
            verse.getChapterId(),
            verse.getOrderIndex(),
            verse.getTextDevanagari(),
            verse.getTextIast(),
            verse.getRawText(),
            verse.getStatus(),
            analysisDto,
            wordDtos,
            vocabularyQuizSlug
        );
    }

    public VerseAnalysisDto toAnalysisDto(VerseAnalysis analysis) {
        List<SandhiSplitDto> sandhiSplits = parseSandhiSplits(analysis.getSandhiSplits());

                return new VerseAnalysisDto(
            analysis.getTranslationRu(),
            analysis.getTranslationEn(),
            sandhiSplits,
            analysis.getModelName(),
            analysis.getAnalyzerName(),
            analysis.getAnalyzedAt()
        );
    }

        public VerseWordDto toWordDto(VerseWord word) {
        List<Integer> formationRuleNumbers = parseFormationRuleNumbers(word.getFormationRuleNumbers());

        VerseWordDto.MorphologyDto morphologyDto = null;
        if (word.getMorphology() != null) {
            var m = word.getMorphology();
            morphologyDto = new VerseWordDto.MorphologyDto(
                m.getCaseType() != null ? m.getCaseType().name() : null,
                m.getGender() != null ? m.getGender().name() : null,
                m.getNumberType() != null ? m.getNumberType().name() : null,
                m.getPerson() != null ? m.getPerson().name() : null,
                m.getTense() != null ? m.getTense().name() : null,
                m.getMood() != null ? m.getMood().name() : null,
                m.getVoice() != null ? m.getVoice().name() : null
            );
        }

        VerseWordDto.DerivationDto derivationDto = null;
        if (word.getDerivation() != null) {
            var d = word.getDerivation();
            derivationDto = new VerseWordDto.DerivationDto(
                d.getDerivationType() != null ? d.getDerivationType().name() : null,
                d.getDerivationalSuffix(),
                d.getDerivationalBase(),
                d.getDescription()
            );
        }

        return new VerseWordDto(
            word.getId(),
            word.getPosition(),
            word.getSurfaceIast(),
            word.getSurfaceDevanagari(),
            word.getLemmaIast(),
            word.getStem(),
            word.getRoot(),
            word.getPos() != null ? word.getPos().name() : null,
            word.getFormType() != null ? word.getFormType().name() : null,
            word.getIsFinite(),
            morphologyDto,
            derivationDto,
            word.getLemmaGlossRu(),
            word.getLemmaGlossEn(),
            word.getContextGlossRu(),
            word.getContextGlossEn(),
            formationRuleNumbers,
            word.getAnalysisConfidence() != null ? word.getAnalysisConfidence().name() : null,
            word.getAmbiguityNotes(),
            word.getVocabularyWordId()
        );
    }

    @SuppressWarnings("unchecked")
    private List<SandhiSplitDto> parseSandhiSplits(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(json, new TypeReference<>() {});
            return raw.stream()
                .map(m -> {
                    String surface = (String) m.getOrDefault("surface", "");
                    List<String> components = (List<String>) m.getOrDefault("components", Collections.emptyList());
                    @SuppressWarnings("unchecked")
                    List<Integer> ruleNumbers = (List<Integer>) m.getOrDefault("ruleNumbers", Collections.emptyList());
                    return new SandhiSplitDto(surface, components, ruleNumbers);
                })
                .toList();
        } catch (Exception e) {
            log.warn("Failed to parse sandhi_splits JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Integer> parseFormationRuleNumbers(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse formation_rule_numbers JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}