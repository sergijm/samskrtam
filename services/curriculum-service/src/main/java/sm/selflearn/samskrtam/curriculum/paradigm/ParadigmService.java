package sm.selflearn.samskrtam.curriculum.paradigm;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.content.dto.DeclensionFormDto;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmDto;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmPageDto;
import sm.selflearn.samskrtam.content.dto.frisch.FrischEntryDto;
import sm.selflearn.samskrtam.content.dto.frisch.FrischGenderDto;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.morphology.Gender;
import sm.selflearn.samskrtam.morphology.NumberType;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.curriculum.dictionary.DictionaryClient;
import sm.selflearn.samskrtam.common.transliteration.TransliterationService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Serves the v2 declension-paradigm page.
 *
 * <p>Леммы берутся напрямую из {@code curriculum.declension_form} (уникальный
 * {@code lemma_iast} по теме), сущность {@code Lexeme} не используется. Переводы
 * и грамматическая информация (род, часть речи и т.п.) подтягиваются из словаря
 * Фриша через dictionary-service (REST, {@link DictionaryClient}); явная
 * деванагари-форма леммы транслитерируется из IAST локально. Парадигма собирается
 * из ячеек {@code declension_form} для выбранной леммы и гласного типа.
 *
 * <p>Indexed carousel semantics match the removed content-service endpoint: items are
 * stably ordered (alphabetically by IAST), exactly one paradigm per call.
 */
@Service
@RequiredArgsConstructor
public class ParadigmService {

    private final ParadigmFormRepository paradigmFormRepository;
    private final DictionaryClient dictionaryClient;
    private final TransliterationService transliterationService;

    private static final int MAX_LEMMAS = 20;

    @Transactional(readOnly = true)
    public DeclensionParadigmPageDto getParadigmPage(String topicCode, int index) {
        return paradigmPageForRegularClass(topicCode, index);
    }

    /* ─── unified declension_form-driven selection ─────────────────── */

    private DeclensionParadigmPageDto paradigmPageForRegularClass(String topicCode, int index) {
        List<VowelType> vowelTypes = DeclensionClassMapper.topicToVowelTypes(topicCode);
        if (vowelTypes.isEmpty()) {
            return emptyPage(index, 0);
        }

        // 1) уникальные леммы из curriculum.declension_form
        Map<String, VowelType> lemmaVowelType = new HashMap<>();
        for (ParadigmFormRepository.LemmaVowelType p :
                paradigmFormRepository.findDistinctLemmaVowelTypeByVowelTypeIn(vowelTypes)) {
            lemmaVowelType.putIfAbsent(p.getLemmaIast(), p.getVowelType());
        }

        List<String> lemmas = new ArrayList<>(lemmaVowelType.keySet());
        lemmas.sort(Comparator.naturalOrder());
        if (lemmas.size() > MAX_LEMMAS) {
            lemmas = lemmas.subList(0, MAX_LEMMAS);
        }

        int totalCount = lemmas.size();
        if (index < 0 || index >= totalCount) {
            return emptyPage(index, totalCount);
        }

        String lemma = lemmas.get(index);
        VowelType vowelType = lemmaVowelType.get(lemma);
        if (vowelType == null) {
            return emptyPage(index, totalCount);
        }

        List<ParadigmForm> forms = paradigmFormRepository.findByLemmaIastAndVowelType(lemma, vowelType);

        // 2) переводы и грамматическая информация из словаря Фриша (dictionary-service, REST)
        List<FrischEntryDto> frischEntries = dictionaryClient.getFrischLemma(lemma);

        // Для местоимений базовая лемма в Фрише часто отсутствует — ищем по форме
        // именительного падежа единственного числа из declension_form.
        if (frischEntries.isEmpty() && isPronoun(vowelType)) {
            String nominativeSingular = nominativeSingularFormIast(forms);
            if (nominativeSingular != null) {
                frischEntries = dictionaryClient.getFrischLemma(nominativeSingular);
            }
        }

        Gender gender = resolveGender(frischEntries);

        return paradigmPage(index, totalCount, lemma, vowelType, gender, frischEntries, forms);
    }

    /* ─── shared DTO builder ───────────────────────────────────────── */

    private static DeclensionParadigmPageDto emptyPage(int index, int totalCount) {
        return DeclensionParadigmPageDto.builder()
                .index(index).totalCount(totalCount).paradigm(null)
                .build();
    }

    private DeclensionParadigmPageDto paradigmPage(int index, int totalCount, String lemma, VowelType vowelType,
                                                   Gender gender, List<FrischEntryDto> frischEntries,
                                                   List<ParadigmForm> forms) {
        UUID stemId = UUID.nameUUIDFromBytes(lemma.getBytes(StandardCharsets.UTF_8));
        String stemDevanagari = transliterationService.iastToDevanagari(lemma);

        List<DeclensionFormDto> formDtos = forms.stream()
                .map(f -> DeclensionFormDto.builder()
                        .declensionStemId(stemId)
                        .caseType(f.getCaseType())
                        .numberType(f.getNumberType())
                        .formIast(f.getFormIast())
                        .formDevanagari(f.getFormDevanagari())
                        .build())
                .toList();

        DeclensionParadigmDto paradigm = DeclensionParadigmDto.builder()
                .stemId(stemId)
                .stemIast(lemma)
                .stemDevanagari(stemDevanagari)
                .translationRu(frischTranslationRu(frischEntries))
                .translationEn(frischTranslationEn(frischEntries))
                .gender(gender)
                .vowelType(vowelType)
                .forms(formDtos)
                .build();

        return DeclensionParadigmPageDto.builder()
                .index(index).totalCount(totalCount).paradigm(paradigm)
                .build();
    }

    /* ─── Frisch (dictionary-service) integration ──────────────────── */

    /**
     * Род берётся из словаря Фриша (первый вариант с заполненным {@code genders}),
     * иначе — {@code UNSPECIFIED}.
     */
    private static boolean isPronoun(VowelType vowelType) {
        return vowelType != null && vowelType.name().startsWith("PRON_");
    }

    private static String nominativeSingularFormIast(List<ParadigmForm> forms) {
        return forms.stream()
                .filter(f -> f.getCaseType() == CaseType.NOMINATIVE && f.getNumberType() == NumberType.SINGULAR)
                .map(ParadigmForm::getFormIast)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static Gender resolveGender(List<FrischEntryDto> frischEntries) {
        for (FrischEntryDto entry : frischEntries) {
            if (entry.genders() != null) {
                for (FrischGenderDto gender : entry.genders()) {
                    Gender mapped = mapFrischGender(gender.gender());
                    if (mapped != null) {
                        return mapped;
                    }
                }
            }
        }
        return Gender.UNSPECIFIED;
    }

    private static Gender mapFrischGender(String gender) {
        if (gender == null) {
            return null;
        }
        return switch (gender) {
            case "MASCULINE" -> Gender.MASCULINE;
            case "FEMININE" -> Gender.FEMININE;
            case "NEUTER" -> Gender.NEUTER;
            default -> null;
        };
    }

    private static String frischTranslationRu(List<FrischEntryDto> entries) {
        return entries.stream()
                .map(FrischEntryDto::glossRu)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static String frischTranslationEn(List<FrischEntryDto> entries) {
        return entries.stream()
                .map(FrischEntryDto::glossEn)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
