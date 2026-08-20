package sm.selflearn.samskrtam.curriculum.paradigm;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.content.dto.ConjugationFormDto;
import sm.selflearn.samskrtam.content.dto.ConjugationParadigmDto;
import sm.selflearn.samskrtam.content.dto.ConjugationParadigmPageDto;
import sm.selflearn.samskrtam.content.model.Voice;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serves the v2 conjugation-paradigm page (index-based carousel, one verb
 * lemma per page). Paradigm cells come from {@code curriculum.conjugation_forms},
 * keyed by {@code topic_code} (== lesson slug). Optionally filtered by voice.
 */
@Service
@RequiredArgsConstructor
public class ConjugationParadigmService {

    /** Person row order in the table: 3rd → 2nd → 1st (Prathama → Madhyama → Uttama). */
    private static final List<Integer> PERSON_ORDER = List.of(3, 2, 1);

    private final ConjugationFormRepository conjugationFormRepository;

    @Transactional(readOnly = true)
    public ConjugationParadigmPageDto getParadigmPage(String topicCode, int index, Voice voice) {
        List<ConjugationForm> forms = (voice == null)
                ? conjugationFormRepository.findByTopicCodeOrderByLemmaIastAscVoiceAscPersonDescNumberTypeAsc(topicCode)
                : conjugationFormRepository.findByTopicCodeAndVoiceOrderByLemmaIastAscPersonDescNumberTypeAsc(topicCode, voice);
        if (forms.isEmpty()) {
            return emptyPage(index, 0);
        }

        Map<String, List<ConjugationForm>> byLemma = groupByLemma(forms);
        List<String> lemmas = List.copyOf(byLemma.keySet());

        int totalCount = lemmas.size();
        if (index < 0 || index >= totalCount) {
            return emptyPage(index, totalCount);
        }

        String lemmaIast = lemmas.get(index);
        List<ConjugationForm> lemmaForms = byLemma.get(lemmaIast);
        ConjugationParadigmDto paradigm = toParadigm(lemmaForms);
        return ConjugationParadigmPageDto.builder()
                .index(index).totalCount(totalCount).paradigm(paradigm)
                .build();
    }

    private static Map<String, List<ConjugationForm>> groupByLemma(List<ConjugationForm> forms) {
        // Input is ordered by lemma_iast asc → LinkedHashMap preserves stable order.
        Map<String, List<ConjugationForm>> grouped = new LinkedHashMap<>();
        for (ConjugationForm f : forms) {
            grouped.computeIfAbsent(f.getLemmaIast(), k -> new ArrayList<>()).add(f);
        }
        return grouped;
    }

    private ConjugationParadigmDto toParadigm(List<ConjugationForm> forms) {
        ConjugationForm first = forms.get(0);
        List<ConjugationFormDto> formDtos = forms.stream()
                .map(f -> ConjugationFormDto.builder()
                        .person(f.getPerson())
                        .numberType(f.getNumberType())
                        .sentenceIast(f.getSentenceIast())
                        .sentenceDevanagari(f.getSentenceDevanagari())
                        .translationRu(f.getTranslationRu())
                        .build())
                .collect(java.util.stream.Collectors.toList());
        // Ensure stable display order even if repository ordering changes.
        formDtos.sort(Comparator
                .comparingInt((ConjugationFormDto f) -> PERSON_ORDER.indexOf(f.getPerson()))
                .thenComparing(f -> f.getNumberType().ordinal()));

        return ConjugationParadigmDto.builder()
                .lemmaIast(first.getLemmaIast())
                .lemmaDevanagari(first.getLemmaDevanagari())
                .meaningRu(first.getMeaningRu())
                .voice(first.getVoice())
                .forms(formDtos)
                .build();
    }

    private static ConjugationParadigmPageDto emptyPage(int index, int totalCount) {
        return ConjugationParadigmPageDto.builder()
                .index(index).totalCount(totalCount).paradigm(null)
                .build();
    }
}