package sm.selflearn.samskrtam.curriculum.paradigm;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.content.dto.DeclensionFormDto;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmDto;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmPageDto;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.curriculum.lexicon.imports.LexiconImportService;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeFrequencyRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Serves the v2 declension-paradigm page. Paradigms are loaded from
 * {@code curriculum.declension_form}, keyed by {@code (lemma_iast, vowel_type)};
 * nothing is composed at runtime. Two lemma-selection sources, chosen by
 * {@code Topic.code} (== lesson slug):
 *
 * <ol>
 *   <li><b>Regular noun classes</b> (e.g. {@code a-stem-masc}) — lexemes bound to the
 *       class via {@code morphology_class}; the paradigm of each lemma is the stored
 *       {@code declension_form} row set of {@code (lemmaIast, vowelType)}, where
 *       {@code vowelType} is derived from the morphology class.</li>
 *   <li><b>Suppletive (pronoun) stems</b> (personal/demonstrative/etc.) — lexemes
 *       found by the fixed {@code topic → (lemmaIast, PRON_* class)} correspondence
 *       ({@link ParadigmTopicCodeToLemmaMapper}).</li>
 * </ol>
 *
 * <p>Indexed carousel semantics match the removed content-service endpoint: items are
 * stably ordered, exactly one paradigm per call.
 */
@Service
@RequiredArgsConstructor
public class ParadigmService {

    private final ParadigmFormRepository paradigmFormRepository;
    private final LexemeRepository lexemeRepository;
    private final LexemeFrequencyRepository lexemeFrequencyRepository;

    @Transactional(readOnly = true)
    public DeclensionParadigmPageDto getParadigmPage(String topicCode, int index) {
        if (DeclensionClassMapper.isRegularDeclensionTopic(topicCode)) {
            return paradigmPageForRegularClass(topicCode, index);
        }
        return paradigmPageForSuppletive(topicCode, index);
    }

    /* ─── regular noun classes ─────────────────────────────────────── */

    private DeclensionParadigmPageDto paradigmPageForRegularClass(String topicCode, int index) {
        List<String> classCodes = DeclensionClassMapper.topicToClassCodes(topicCode);
        List<VowelType> vowelTypes = classCodes.stream()
                .map(DeclensionClassMapper::toVowelType)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (vowelTypes.isEmpty()) {
            return emptyPage(index, 0);
        }

        Set<String> lemmaIastsWithForms = new HashSet<>(
                paradigmFormRepository.findDistinctLemmaIastsByVowelTypeIn(vowelTypes));
        List<Lexeme> lexemes = lexemeRepository.findNounsWithMorphologyByCodeIn(classCodes);
        Map<UUID, Integer> ranks = frequencyRanks(lexemes);
        lexemes = lexemes.stream()
                .filter(l -> lemmaIastsWithForms.contains(l.getLemmaIast()))
                .sorted(Comparator.comparing((Lexeme l) -> rankOrMax(ranks, l), Comparator.naturalOrder())
                        .thenComparing(Lexeme::getLemmaIast)
                        .thenComparing(Lexeme::getId))
                .distinct()
                .limit(20)
                .toList();

        int totalCount = lexemes.size();
        if (index < 0 || index >= totalCount) {
            return emptyPage(index, totalCount);
        }

        Lexeme lexeme = lexemes.get(index);
        String lexemeClassCode = resolveClassCode(lexeme, classCodes);
        VowelType vowelType = DeclensionClassMapper.toVowelType(lexemeClassCode);
        if (vowelType == null) {
            return emptyPage(index, totalCount);
        }
        return paradigmPage(index, totalCount, lexeme, vowelType,
                toContentGender(resolveGender(lexemeClassCode, lexeme.getGender())),
                paradigmFormRepository.findByLemmaIastAndVowelType(lexeme.getLemmaIast(), vowelType));
    }

    /* ─── suppletive (pronoun) stems ───────────────────────────────── */

    private DeclensionParadigmPageDto paradigmPageForSuppletive(String topicCode, int index) {
        List<ParadigmTopicCodeToLemmaMapper.ParadigmRef> refs =
                ParadigmTopicCodeToLemmaMapper.mapTopicCodeToParadigms(topicCode);
        if (refs.isEmpty()) {
            return emptyPage(index, 0);
        }

        List<VowelType> vowelTypes = refs.stream().map(r -> r.vowelType()).toList();
        Set<String> lemmaIastsWithForms = new HashSet<>(
                paradigmFormRepository.findDistinctLemmaIastsByVowelTypeIn(vowelTypes));
        List<String> lemmas = refs.stream().map(r -> r.lemmaIast()).toList();
        List<Lexeme> lexemes = lexemeRepository.findByLemmaIastIn(lemmas).stream()
                .filter(l -> lemmaIastsWithForms.contains(l.getLemmaIast()))
                .sorted(Comparator.comparing(Lexeme::getLemmaIast).thenComparing(Lexeme::getId))
                .toList();

        int totalCount = lexemes.size();
        if (index < 0 || index >= totalCount) {
            return emptyPage(index, totalCount);
        }

        Lexeme lexeme = lexemes.get(index);
        VowelType vowelType = vowelTypeFor(lexeme.getLemmaIast(), refs);
        return paradigmPage(index, totalCount, lexeme, vowelType,
                toContentGender(lexeme.getGender()),
                paradigmFormRepository.findByLemmaIastAndVowelType(lexeme.getLemmaIast(), vowelType));
    }

    private static VowelType vowelTypeFor(String lemmaIast,
                                          List<ParadigmTopicCodeToLemmaMapper.ParadigmRef> refs) {
        return refs.stream()
                .filter(r -> r.lemmaIast().equals(lemmaIast))
                .map(ParadigmTopicCodeToLemmaMapper.ParadigmRef::vowelType)
                .findFirst()
                .orElse(null);
    }

    /* ─── shared DTO builder ───────────────────────────────────────── */

    private static DeclensionParadigmPageDto emptyPage(int index, int totalCount) {
        return DeclensionParadigmPageDto.builder()
                .index(index).totalCount(totalCount).paradigm(null)
                .build();
    }

    private DeclensionParadigmPageDto paradigmPage(int index, int totalCount, Lexeme lexeme, VowelType vowelType,
                                                   sm.selflearn.samskrtam.content.model.Gender gender,
                                                   List<ParadigmForm> forms) {
        List<DeclensionFormDto> formDtos = forms.stream()
                .map(f -> DeclensionFormDto.builder()
                        .declensionStemId(lexeme.getId())
                        .caseType(f.getCaseType())
                        .numberType(f.getNumberType())
                        .formIast(f.getFormIast())
                        .formDevanagari(f.getFormDevanagari())
                        .build())
                .toList();

        DeclensionParadigmDto paradigm = DeclensionParadigmDto.builder()
                .stemId(lexeme.getId())
                .stemIast(lexeme.getLemmaIast())
                .stemDevanagari(lexeme.getLemmaDevanagari())
                .translationRu(lexeme.getGlossRu())
                .translationEn(lexeme.getGlossEn())
                .gender(gender)
                .vowelType(vowelType)
                .forms(formDtos)
                .build();

        return DeclensionParadigmPageDto.builder()
                .index(index).totalCount(totalCount).paradigm(paradigm)
                .build();
    }

    /* ─── mapping helpers ─────────────────────────────────────────── */

    /** SANGRAHA_CORPUS frequency rank by lexeme id; lexemes without an entry sort last. */
    private Map<UUID, Integer> frequencyRanks(List<Lexeme> lexemes) {
        if (lexemes.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = lexemes.stream().map(Lexeme::getId).toList();
        return lexemeFrequencyRepository.findBySourceAndLexemeIdIn(
                        LexiconImportService.FREQUENCY_SOURCE, ids).stream()
                .collect(HashMap::new, (m, f) -> m.put(f.getId().getLexemeId(), f.getRank()), Map::putAll);
    }

    private static int rankOrMax(Map<UUID, Integer> ranks, Lexeme lexeme) {
        return ranks.getOrDefault(lexeme.getId(), Integer.MAX_VALUE);
    }

    /** First of {@code classCodes} the lexeme is actually bound to, or null. */
    private static String resolveClassCode(Lexeme lexeme, List<String> classCodes) {
        if (lexeme.getMorphologyClasses() == null) {
            return null;
        }
        for (String code : classCodes) {
            boolean bound = lexeme.getMorphologyClasses().stream().anyMatch(mc -> mc.getCode().equals(code));
            if (bound) {
                return code;
            }
        }
        return null;
    }

    private static sm.selflearn.samskrtam.content.model.Gender toContentGender(LexemeGender gender) {
        return switch (gender) {
            case MASCULINE -> sm.selflearn.samskrtam.content.model.Gender.MASCULINE;
            case FEMININE -> sm.selflearn.samskrtam.content.model.Gender.FEMININE;
            case NEUTER -> sm.selflearn.samskrtam.content.model.Gender.NEUTER;
            default -> sm.selflearn.samskrtam.content.model.Gender.UNSPECIFIED;
        };
    }

    private static LexemeGender resolveGender(String classCode, LexemeGender lexemeGender) {
        return switch (classCode) {
            case "a-stem-masc" -> LexemeGender.MASCULINE;
            case "a-stem-neut" -> LexemeGender.NEUTER;
            case "a-stem-fem" -> LexemeGender.FEMININE;
            case "a-stem" -> lexemeGender; // merged: use lexeme's own gender
            case "i-stem", "u-stem" ->
                    lexemeGender == LexemeGender.NEUTER ? LexemeGender.NEUTER : LexemeGender.MASCULINE;
            case "ii-stem", "uu-stem" -> LexemeGender.FEMININE;
            case "r-stem" ->
                    lexemeGender == LexemeGender.FEMININE ? LexemeGender.FEMININE : LexemeGender.MASCULINE;
            default -> lexemeGender == null ? LexemeGender.UNSPECIFIED : lexemeGender;
        };
    }
}
