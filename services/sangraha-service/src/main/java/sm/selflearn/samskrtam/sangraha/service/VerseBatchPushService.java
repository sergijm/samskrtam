package sm.selflearn.samskrtam.sangraha.service;

import sm.selflearn.samskrtam.common.transliteration.TransliterationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.content.dto.VerseLemmaBatch;
import sm.selflearn.samskrtam.content.dto.VerseLemmaBatch.Word;
import sm.selflearn.samskrtam.sangraha.model.Chapter;
import sm.selflearn.samskrtam.sangraha.model.Lemma;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.repository.LemmaRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Строит и отправляет пачку лемм стиха в curriculum-service после успешного
 * анализа (lexicon-content-pipeline.md §7). Не является частью транзакции анализа:
 * при сбое curriculum-service пачка теряется (лог-предупреждение), анализ стиха
 * уже сохранён и не откатывается.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerseBatchPushService {

    private static final List<String> NO_CATEGORIES = List.of();

    private final LemmaRepository lemmaRepository;
    private final TransliterationService transliterationService;
    private final CurriculumLexiconClient curriculumLexiconClient;

    /**
     * Push пачки. Для standalone-стихов (без главы/произведения) топик
     * персональный ({@code user-{ownerId}}); для стихов глав — по
     * {@code workSlp1}_{chapterNumber}. Стих без главы и без владельца — пропуск.
     * Любой сбой (в т.ч. недоступность curriculum-service) только логируется —
     * анализ стиха уже сохранён и не откатывается.
     */
    public void push(Verse verse, Work work, Chapter chapter, List<VerseWord> words) {
        try {
            pushInternal(verse, work, chapter, words);
        } catch (Exception e) {
            log.warn("Failed to push verse batch for verse {}: {}", verse.getId(), e.getMessage());
        }
    }

    private void pushInternal(Verse verse, Work work, Chapter chapter, List<VerseWord> words) {
        if ((work == null || chapter == null) && verse.getOwnerId() == null) {
            log.debug("Verse {} has no work/chapter context and no owner, skipping verse batch push",
                    verse.getId());
            return;
        }
        List<Word> batchWords = buildWords(words);
        if (batchWords.isEmpty()) {
            log.debug("Verse {} has no analyzable words, skipping verse batch push", verse.getId());
            return;
        }
        VerseLemmaBatch batch = new VerseLemmaBatch(
                verse.getId(),
                verse.getOwnerId(),
                work == null ? null : work.getSlug(),
                work == null ? null : transliterationService.iastToSlp1(work.getSlug()),
                chapter == null ? 0 : chapter.getOrderIndex() == null ? 0 : chapter.getOrderIndex(),
                chapter == null ? null : chapter.getSlug(),
                work == null ? null : work.getTitleRu(),
                work == null ? null : work.getTitleEn(),
                batchWords);
        curriculumLexiconClient.pushVerseBatch(batch);
    }

    private List<Word> buildWords(List<VerseWord> words) {
        Map<String, Word> unique = new LinkedHashMap<>();
        for (VerseWord w : words) {
            if (w.getLemmaIast() == null || w.getLemmaIast().isBlank()) {
                continue;
            }
            Optional<Lemma> lemma = lemmaRepository.findByLemmaIast(w.getLemmaIast());

            String lemmaSlp1 = lemma.map(Lemma::getLemmaSlp1)
                    .orElseGet(() -> transliterationService.iastToSlp1(w.getLemmaIast()));
            String lemmaDevanagari = lemma.map(Lemma::getLemmaDevanagari).orElse("");
            String gender = w.getMorphology() != null && w.getMorphology().getGender() != null
                    ? w.getMorphology().getGender().name() : null;
            String pos = w.getPos() != null ? w.getPos().name() : null;

            Word word = new Word(
                    lemma.map(Lemma::getId).orElse(null),
                    lemmaSlp1,
                    w.getLemmaIast(),
                    lemmaDevanagari,
                    gender,
                    pos,
                    1,
                    NO_CATEGORIES,
                    w.getLemmaGlossRu(),
                    w.getLemmaGlossEn(),
                    null);
            unique.putIfAbsent(lemmaSlp1 + "|" + (gender == null ? "" : gender), word);
        }
        return new ArrayList<>(unique.values());
    }
}