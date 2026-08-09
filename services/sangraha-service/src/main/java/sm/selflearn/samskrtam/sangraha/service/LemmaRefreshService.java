package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.dto.LemmaRefreshResponse;
import sm.selflearn.samskrtam.sangraha.model.Lemma;
import sm.selflearn.samskrtam.sangraha.repository.LemmaRepository;
import sm.selflearn.samskrtam.sangraha.repository.LemmaStatisticsRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Пересчёт словаря {@code sangraha.lemma} и статистики {@code sangraha.lemma_statistics}
 * из корпуса {@code VerseWord} (lemma-classification.md §1.3).
 *
 * <p>1) Словарь: из verse_words берутся DISTINCT {@code lemma_iast}, которых ещё нет
 * в словаре ({@link VerseWordRepository#findDistinctLemmaIast}, фильтр NOT EXISTS по
 * тексту lemma_iast), транслитерируются в SLP1/devanagari и сохраняются через
 * {@link LemmaRepository#saveAll}.
 * <p>2) Статистика пересчитывается нативной функцией
 * {@code sangraha.compute_lemma_statistics} (upsert по (lemma_id, gender)) — вызов
 * {@link LemmaStatisticsRepository#refreshStatistics} со всеми леммами (null).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LemmaRefreshService {

    private final VerseWordRepository verseWordRepository;
    private final LemmaRepository lemmaRepository;
    private final LemmaStatisticsRepository statisticsRepository;
    private final TransliterationService transliterationService;

    /**
     * @return счётчики словаря и статистики. Идемпотентен: повторный вызов на
     * тех же данных не создаёт дублей Lemma/статистики.
     */
    @Transactional
    public LemmaRefreshResponse refresh() {
        LemmaDictionarySummary dictionary = refreshDictionary();
        int statisticsTotal = statisticsRepository.refreshStatistics(null);

        log.info("Lemma refresh done: lemmas={}, newLemmas={}, updatedLemmas={}, stats={}",
                dictionary.total, dictionary.newCount, dictionary.updatedCount, statisticsTotal);
        return new LemmaRefreshResponse(
                dictionary.total,
                dictionary.newCount,
                dictionary.updatedCount,
                statisticsTotal, 0, 0);
    }

    /** Добавление в словарь новых лемм: DISTINCT новых lemma_iast → saveAll. */
    private LemmaDictionarySummary refreshDictionary() {
        List<Lemma> toSave = new ArrayList<>();
        for (String iast : verseWordRepository.findDistinctLemmaIast()) {
            if (iast == null || iast.isBlank()) {
                continue;
            }
            toSave.add(Lemma.builder()
                    .lemmaSlp1(transliterationService.iastToSlp1(iast))
                    .lemmaIast(iast)
                    .lemmaDevanagari(transliterationService.iastToDevanagari(iast))
                    .build());
        }
        if (!toSave.isEmpty()) {
            lemmaRepository.saveAll(toSave);
            lemmaRepository.flush();
        }
        return new LemmaDictionarySummary(toSave.size(), toSave.size(), 0);
    }

    /** Свод подсчёта словаря (текущий вызов: добавлялись только новые строки). */
    private record LemmaDictionarySummary(int total, int newCount, int updatedCount) {
    }
}