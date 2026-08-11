package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.model.ClassificationStatus;
import sm.selflearn.samskrtam.sangraha.model.Lemma;
import sm.selflearn.samskrtam.sangraha.model.LemmaStatistics;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;
import sm.selflearn.samskrtam.sangraha.repository.LemmaRepository;
import sm.selflearn.samskrtam.sangraha.repository.LemmaStatisticsRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;
import sm.selflearn.samskrtam.sangraha.service.LemmaClassificationPromptBuilder.LemmaBatchItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClassificationCandidateReader {

    private final LemmaRepository lemmaRepository;
    private final LemmaStatisticsRepository statisticsRepository;
    private final VerseWordRepository verseWordRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<LemmaBatchItem> loadCandidates(String schemeCode, int batchSize, int batchCount) {
        if (batchCount <= 0) throw new IllegalArgumentException("batchCount must be positive");

        List<Lemma> candidates = orderByTotalOccurrences(
                lemmaRepository.findCandidatesForClassification(schemeCode, ClassificationStatus.REJECTED));
        List<Lemma> selected = sublist(candidates, 0, batchSize * batchCount);

        List<LemmaStatistics> allStats = statisticsRepository.findByLemmaIdIn(
                selected.stream().map(Lemma::getId).toList());
        Map<UUID, LemmaStatistics> dominantByLemma = allStats.stream()
                .collect(Collectors.toMap(s -> s.getLemma().getId(),
                        Function.identity(),
                        this::pickDominant));

        List<LemmaBatchItem> items = new ArrayList<>(selected.size());
        for (Lemma lemma : selected) {
            LemmaStatistics dominant = dominantByLemma.get(lemma.getId());
            items.add(new LemmaBatchItem(
                    lemma,
                    dominant == null ? null : dominant.getGender(),
                    dominant == null ? null : dominant.getDominantPosCode(),
                    examplesFor(lemma)));
        }
        return items;
    }

    private LemmaStatistics pickDominant(LemmaStatistics a, LemmaStatistics b) {
        int byCount = Integer.compare(a.getOccurrenceCount(), b.getOccurrenceCount());
        if (byCount != 0) return byCount > 0 ? a : b;
        return String.valueOf(a.getGender()).compareTo(String.valueOf(b.getGender())) >= 0 ? a : b;
    }

    private List<String> examplesFor(Lemma lemma) {
        List<VerseWord> words = verseWordRepository.findTop2ByLemmaIastOrderByPositionAsc(lemma.getLemmaIast());
        List<String> examples = new ArrayList<>(words.size());
        for (VerseWord w : words) examples.add(w.getSurfaceIast());
        return examples;
    }

    private List<Lemma> orderByTotalOccurrences(List<Lemma> candidates) {
        List<LemmaStatistics> allStats = statisticsRepository.findAll();
        Map<UUID, Integer> totals = new HashMap<>();
        for (LemmaStatistics s : allStats) totals.merge(s.getLemma().getId(), s.getOccurrenceCount(), Integer::sum);
        List<Lemma> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator
                .comparingInt((Lemma l) -> totals.getOrDefault(l.getId(), 0)).reversed()
                .thenComparing(Lemma::getLemmaSlp1));
        return sorted;
    }

    private static <T> List<T> sublist(List<T> list, int from, int size) {
        int to = Math.min(from + size, list.size());
        return from >= to ? List.of() : list.subList(from, to);
    }
}