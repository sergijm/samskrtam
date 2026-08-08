package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.dto.LemmaRefreshResponse;
import sm.selflearn.samskrtam.sangraha.model.Lemma;
import sm.selflearn.samskrtam.sangraha.model.PartOfSpeech;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;
import sm.selflearn.samskrtam.sangraha.model.VerseWordMorphology;
import sm.selflearn.samskrtam.sangraha.repository.LemmaRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Пересчёт {@code sangraha.lemma} из корпуса {@code VerseWord}
 * (lemma-classification.md §1.3, task-sangraha-17 шаги 7–9).
 *
 * <p>Агрегирует только стихи {@code status = ANALYZED} (тот же фильтр, что у
 * {@link VerseWordExportService}). Группа — {@code (lemmaSlp1, gender)}:
 * {@code lemmaSlp1} — IAST→SLP1 конвертер, {@code gender} — мода среди
 * ненулевых {@code verse_word_morphology.gender} по всем вхождениям леммы
 * (алфавитный tie-break), {@code null}, если у леммы нет gender-вхождений.
 * По группе агрегируются {@code occurrenceCount}, {@code dominantPosCode}
 * (мода по POS). {@code frequencyRank} потом пересчитывается по всем строкам
 * разом: 1 — максимальный {@code occurrenceCount}, tie-break по
 * {@code lemmaSlp1} (детерминированно). После upsert'а проставляется
 * {@code VerseWord.lemmaId} для каждой строки группы.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LemmaRefreshService {

    private static final int PAGE_SIZE = 500;

    private final VerseRepository verseRepository;
    private final VerseWordRepository verseWordRepository;
    private final LemmaRepository lemmaRepository;
    private final TransliterationService transliterationService;

    /**
     * @return {lemmaCount, newLemmaCount, updatedLemmaCount}.
     * Идемпотентен: повторный вызов на тех же данных не создаёт дублей Lemma.
     */
    @Transactional
    public LemmaRefreshResponse refresh() {
        Map<String, List<VerseWord>> wordsBySlp1 = collectAnalyzedWords();
        Map<String, Lemma> lemmaBySlp1 = loadExistingBySlp1();

        int newCount = 0;
        int updatedCount = 0;
        for (Map.Entry<String, List<VerseWord>> e : wordsBySlp1.entrySet()) {
            String lemmaSlp1 = e.getKey();
            List<VerseWord> words = e.getValue();
            String gender = dominantGender(words);
            int count = words.size();
            PartOfSpeech dominantPos = dominantPos(words);

            Lemma lemma = lemmaBySlp1.get(lemmaSlp1);
            if (lemma == null) {
                lemma = new Lemma();
                lemma.setLemmaSlp1(lemmaSlp1);
                lemma.setGender(gender);
                lemma.setLemmaIast(words.get(0).getLemmaIast());
                lemma.setLemmaDevanagari(transliterationService.iastToDevanagari(words.get(0).getLemmaIast()));
                lemma.setOccurrenceCount(count);
                lemma.setDominantPosCode(dominantPos.name());
                lemmaBySlp1.put(lemmaSlp1, lemma);
                newCount++;
            } else {
                boolean countChanged = lemma.getOccurrenceCount() != count;
                boolean posChanged = !Objects.equals(lemma.getDominantPosCode(), dominantPos.name());
                boolean genderChanged = !Objects.equals(lemma.getGender(), gender);
                lemma.setOccurrenceCount(count);
                lemma.setDominantPosCode(dominantPos.name());
                lemma.setGender(gender);
                if (lemma.getLemmaIast() == null) {
                    lemma.setLemmaIast(words.get(0).getLemmaIast());
                }
                if (lemma.getLemmaDevanagari() == null) {
                    lemma.setLemmaDevanagari(transliterationService.iastToDevanagari(lemma.getLemmaIast()));
                }
                if (countChanged || posChanged || genderChanged) {
                    updatedCount++;
                }
            }
        }

        List<Lemma> all = new ArrayList<>(lemmaBySlp1.values());
        assignFrequencyRanks(all);
        lemmaRepository.saveAll(all);
        lemmaRepository.flush();

        linkLemmaIds(wordsBySlp1, lemmaBySlp1);
        lemmaRepository.flush();

        log.info("Lemma refresh done: count={}, new={}, updated={}", all.size(), newCount, updatedCount);
        return new LemmaRefreshResponse(all.size(), newCount, updatedCount);
    }

    /** Проход по всем ANALYZED стихам (курсором) → слова, сгруппированные по lemmaSlp1. */
    private Map<String, List<VerseWord>> collectAnalyzedWords() {
        Map<String, List<VerseWord>> wordsBySlp1 = new HashMap<>();
        UUID cursor = null;
        while (true) {
            List<Verse> page = verseRepository.findAllByStatusAndDeletedAtIsNullAndIdGreaterThan(
                    VerseStatus.ANALYZED, cursor, PageRequest.of(0, PAGE_SIZE));
            if (page.isEmpty()) {
                break;
            }
            for (Verse verse : page) {
                for (VerseWord w : verse.getVerseWords()) {
                    if (w.getLemmaIast() == null || w.getLemmaIast().isBlank()) {
                        continue;
                    }
                    String slp1 = transliterationService.iastToSlp1(w.getLemmaIast());
                    wordsBySlp1.computeIfAbsent(slp1, k -> new ArrayList<>()).add(w);
                }
            }
            cursor = page.get(page.size() - 1).getId();
        }
        return wordsBySlp1;
    }

    private Map<String, Lemma> loadExistingBySlp1() {
        Map<String, Lemma> result = new HashMap<>();
        for (Lemma l : lemmaRepository.findAll()) {
            result.put(l.getLemmaSlp1(), l);
        }
        return result;
    }

    private void assignFrequencyRanks(List<Lemma> lemmas) {
        lemmas.sort(Comparator
                .comparingInt(Lemma::getOccurrenceCount).reversed()
                .thenComparing(Lemma::getLemmaSlp1));
        for (int i = 0; i < lemmas.size(); i++) {
            lemmas.get(i).setFrequencyRank(i + 1);
        }
    }

    private void linkLemmaIds(
            Map<String, List<VerseWord>> wordsBySlp1,
            Map<String, Lemma> lemmaBySlp1) {
        for (Map.Entry<String, Lemma> e : lemmaBySlp1.entrySet()) {
            List<VerseWord> words = wordsBySlp1.get(e.getKey());
            if (words == null || words.isEmpty()) {
                continue;
            }
            Lemma lemma = e.getValue();
            for (VerseWord w : words) {
                w.setLemmaId(lemma.getId());
            }
            verseWordRepository.saveAll(words);
        }
    }

    static String dominantGender(List<VerseWord> words) {
        Map<String, Integer> byGender = new HashMap<>();
        for (VerseWord w : words) {
            VerseWordMorphology m = w.getMorphology();
            if (m != null && m.getGender() != null) {
                byGender.merge(m.getGender().name(), 1, Integer::sum);
            }
        }
        if (byGender.isEmpty()) {
            return null;
        }
        return byGender.entrySet().stream()
                .max(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    static PartOfSpeech dominantPos(List<VerseWord> words) {
        Map<PartOfSpeech, Integer> posCounts = new HashMap<>();
        for (VerseWord w : words) {
            posCounts.merge(w.getPos() == null ? PartOfSpeech.OTHER : w.getPos(), 1, Integer::sum);
        }
        return posCounts.entrySet().stream()
                .max((a, b) -> {
                    int byCount = Integer.compare(a.getValue(), b.getValue());
                    return byCount == 0 ? a.getKey().name().compareTo(b.getKey().name()) : byCount;
                })
                .map(Map.Entry::getKey)
                .orElse(PartOfSpeech.OTHER);
    }
}