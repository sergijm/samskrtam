package sm.selflearn.samskrtam.curriculum.paradigm;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.content.dto.DeclensionFormDto;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmDto;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmPageDto;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.questgen.DeclensionNounParadigmComposer;
import sm.selflearn.samskrtam.curriculum.questgen.morphology.CaseType;
import sm.selflearn.samskrtam.curriculum.questgen.morphology.NumberType;

import java.util.Comparator;
import java.util.List;

/**
 * Serves the v2 declension-paradigm page. Two sources, selected by {@code Topic.code}
 * (== lesson slug):
 *
 * <ol>
 *   <li><b>Regular noun classes</b> (e.g. {@code a-stem-masc}) — the paradigm of each
 *       lexeme bound to the class is composed on the fly from the canonical endings
 *       ({@link DeclensionNounParadigmComposer}), mirroring the batch generator.</li>
 *   <li><b>Suppletive (pronoun) stems</b> (personal/demonstrative/etc.) — stored in the
 *       {@code curriculum} schema (V9 mirror of content.declension_stems/forms).</li>
 * </ol>
 *
 * <p>Indexed carousel semantics match the removed content-service endpoint: items are
 * stably ordered, exactly one paradigm per call.
 */
@Service
@RequiredArgsConstructor
public class ParadigmService {

    private final ParadigmStemRepository paradigmStemRepository;
    private final ParadigmFormRepository paradigmFormRepository;
    private final LexemeRepository lexemeRepository;

    @Transactional(readOnly = true)
    public DeclensionParadigmPageDto getParadigmPage(String topicCode, int index) {
        if (DeclensionNounParadigmComposer.isRegularDecensionClass(topicCode)) {
            return paradigmPageForRegularClass(topicCode, index);
        }
        return paradigmPageForSuppletive(topicCode, index);
    }

    /* ─── regular noun classes ─────────────────────────────────────── */

    private DeclensionParadigmPageDto paradigmPageForRegularClass(String topicCode, int index) {
        List<Lexeme> lexemes = lexemeRepository.findByMorphologyClasses_Code(topicCode).stream()
                .sorted(Comparator.comparing(Lexeme::getId))
                .toList();

        int totalCount = lexemes.size();
        if (index < 0 || index >= totalCount) {
            return DeclensionParadigmPageDto.builder()
                    .index(index).totalCount(totalCount).paradigm(null)
                    .build();
        }

        Lexeme lexeme = lexemes.get(index);
        List<DeclensionNounParadigmComposer.Cell> cells = DeclensionNounParadigmComposer.compose(
                topicCode, lexeme.getLemmaIast(), lexeme.getLemmaDevanagari(), lexeme.getGender());

        List<DeclensionFormDto> forms = cells.stream()
                .map(cell -> DeclensionFormDto.builder()
                        .declensionStemId(lexeme.getId())
                        .caseType(toContentCase(cell.caseType()))
                        .numberType(toContentNumber(cell.numberType()))
                        .formIast(cell.form().iast())
                        .formDevanagari(cell.form().devanagari())
                        .build())
                .toList();

        DeclensionParadigmDto paradigm = DeclensionParadigmDto.builder()
                .stemId(lexeme.getId())
                .stemIast(lexeme.getLemmaIast())
                .stemDevanagari(lexeme.getLemmaDevanagari())
                .translationRu(lexeme.getGlossRu())
                .translationEn(lexeme.getGlossEn())
                .gender(toContentGender(resolveGender(topicCode, lexeme.getGender())))
                .vowelType(toContentVowel(topicCode))
                .forms(forms)
                .build();

        return DeclensionParadigmPageDto.builder()
                .index(index).totalCount(totalCount).paradigm(paradigm)
                .build();
    }

    /* ─── suppletive (pronoun) stems ───────────────────────────────── */

    private DeclensionParadigmPageDto paradigmPageForSuppletive(String topicCode, int index) {
        List<VowelType> vowelTypes = ParadigmTopicCodeToVowelMapper.mapTopicCodeToVowelTypes(topicCode);

        List<ParadigmStem> stems = vowelTypes.isEmpty()
                ? List.of()
                : paradigmStemRepository.findByVowelTypeIn(vowelTypes).stream()
                        .sorted(Comparator.comparing(ParadigmStem::getId))
                        .toList();

        int totalCount = stems.size();
        if (index < 0 || index >= totalCount) {
            return DeclensionParadigmPageDto.builder()
                    .index(index).totalCount(totalCount).paradigm(null)
                    .build();
        }

        ParadigmStem stem = stems.get(index);
        List<ParadigmForm> forms = paradigmFormRepository.findByDeclensionStemId(stem.getId());

        return DeclensionParadigmPageDto.builder()
                .index(index)
                .totalCount(totalCount)
                .paradigm(toSuppletiveParadigmDto(stem, forms))
                .build();
    }

    private DeclensionParadigmDto toSuppletiveParadigmDto(ParadigmStem stem, List<ParadigmForm> forms) {
        List<DeclensionFormDto> formDtos = forms.stream()
                .map(f -> DeclensionFormDto.builder()
                        .declensionStemId(stem.getId())
                        .caseType(f.getCaseType())
                        .numberType(f.getNumberType())
                        .formIast(f.getFormIast())
                        .formDevanagari(f.getFormDevanagari())
                        .build())
                .toList();

        return DeclensionParadigmDto.builder()
                .stemId(stem.getId())
                .stemIast(stem.getStemIast())
                .stemDevanagari(stem.getStemDevanagari())
                .translationRu(stem.getTranslationRu())
                .translationEn(stem.getTranslationEn())
                .gender(stem.getGender())
                .vowelType(stem.getVowelType())
                .forms(formDtos)
                .build();
    }

    /* ─── mapping helpers ─────────────────────────────────────────── */

    private static sm.selflearn.samskrtam.content.model.Gender toContentGender(LexemeGender gender) {
        return switch (gender) {
            case MASCULINE -> sm.selflearn.samskrtam.content.model.Gender.MASCULINE;
            case FEMININE -> sm.selflearn.samskrtam.content.model.Gender.FEMININE;
            case NEUTER -> sm.selflearn.samskrtam.content.model.Gender.NEUTER;
            default -> sm.selflearn.samskrtam.content.model.Gender.UNSPECIFIED;
        };
    }

    private static sm.selflearn.samskrtam.content.model.CaseType toContentCase(CaseType caseType) {
        return sm.selflearn.samskrtam.content.model.CaseType.valueOf(caseType.name());
    }

    private static sm.selflearn.samskrtam.content.model.NumberType toContentNumber(NumberType numberType) {
        return sm.selflearn.samskrtam.content.model.NumberType.valueOf(numberType.name());
    }

    private static VowelType toContentVowel(String classCode) {
        return switch (classCode) {
            case "a-stem-masc" -> VowelType.A_STEM;
            case "a-stem-neut" -> VowelType.A_STEM;
            case "a-stem-fem" -> VowelType.AA_STEM;
            case "i-stem" -> VowelType.I_STEM;
            case "u-stem" -> VowelType.U_STEM;
            case "r-stem" -> VowelType.R_STEM;
            default -> null;
        };
    }

    private static LexemeGender resolveGender(String classCode, LexemeGender lexemeGender) {
        return switch (classCode) {
            case "a-stem-masc" -> LexemeGender.MASCULINE;
            case "a-stem-neut" -> LexemeGender.NEUTER;
            case "a-stem-fem" -> LexemeGender.FEMININE;
            default -> lexemeGender;
        };
    }
}