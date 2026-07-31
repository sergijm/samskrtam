package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.content.dto.SangrahaVocabularyResponse;
import sm.selflearn.samskrtam.sangraha.dto.VerseDetailDto;
import sm.selflearn.samskrtam.sangraha.dto.VocabularyQuizResponse;
import sm.selflearn.samskrtam.sangraha.event.SangrahaVocabularyEvent;
import sm.selflearn.samskrtam.sangraha.mapper.VerseMapper;
import sm.selflearn.samskrtam.sangraha.model.Chapter;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.VerseAnalysis;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.repository.ChapterRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseAnalysisRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;
import sm.selflearn.samskrtam.sangraha.repository.WorkRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerseService {

        private final VerseRepository verseRepository;
        private final VerseAnalysisRepository verseAnalysisRepository;
        private final VerseWordRepository verseWordRepository;
        private final ChapterRepository chapterRepository;
        private final WorkRepository workRepository;
        private final ChapterService chapterService;
        private final VerseMapper verseMapper;
        private final TransliterationService transliterationService;
        private final WorkService workService;
        private final ContentServiceVocabularyClient contentServiceVocabularyClient;

    @Transactional(readOnly = true)
    public List<Verse> getVersesByChapterId(UUID chapterId) {
        chapterService.getChapterById(chapterId);
        return verseRepository.findAllByChapterIdAndDeletedAtIsNullOrderByOrderIndexAsc(chapterId);
    }

        @Transactional(readOnly = true)
        public VerseDetailDto getVerseDetail(UUID id) {
            Verse verse = getVerseById(id);
            Optional<VerseAnalysis> analysis = verseAnalysisRepository.findByVerseId(id);
            List<VerseWord> words = verseWordRepository.findAllByVerseIdOrderByPositionAsc(id);

            return verseMapper.toDetailDto(verse, analysis.orElse(null),
                    words.isEmpty() ? null : words,
                    verse.getVocabularyQuizSlug());
        }

    @Transactional(readOnly = true)
    public Verse getVerseById(UUID id) {
        return verseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Verse not found: " + id));
    }

    @Transactional(readOnly = true)
    public VerseAnalysis getVerseAnalysis(UUID verseId) {
        return verseAnalysisRepository.findByVerseId(verseId)
                .orElseThrow(() -> new RuntimeException("Verse analysis not found: " + verseId));
    }

    @Transactional(readOnly = true)
    public List<VerseWord> getVerseWords(UUID verseId) {
        return verseWordRepository.findAllByVerseIdOrderByPositionAsc(verseId);
    }

    @Transactional
    public Verse createVerse(UUID chapterId, Verse verse) {
        chapterService.getChapterById(chapterId);
        verse.setChapterId(chapterId);
        verse.setStatus(VerseStatus.DRAFT);
        verse.setCreatedAt(Instant.now());
        return verseRepository.save(verse);
    }

    /**
     * PUT /verses/{id}/text — единое поле text.
     * Backend определяет письменность по Unicode-диапазону деванагари (\u0900–\u097F)
     * и кладёт в textDevanagari либо textIast.
     */
    @Transactional
    public Verse updateVerseText(UUID id, String text) {
        Verse verse = getVerseById(id);
        String script = transliterationService.detectScript(text);
        if ("devanagari".equals(script)) {
            verse.setTextDevanagari(text);
            verse.setTextIast(null);
        } else {
            verse.setTextIast(text);
            verse.setTextDevanagari(null);
        }
        verse.setUpdatedAt(Instant.now());
        return verseRepository.save(verse);
    }

    @Transactional
    public void deleteVerse(UUID id) {
        Verse verse = getVerseById(id);
        verse.setDeletedAt(Instant.now());
        verseRepository.save(verse);
    }

    /**
     * PUT /verses/{id} — обновление orderIndex и rawText.
     * Если текст изменился (rawText не равен сохранённому) — очищает результаты анализа
     * и сбрасывает статус в DRAFT.
     */
    @Transactional
    public Verse updateVerse(UUID id, int orderIndex, String rawText) {
        Verse verse = getVerseById(id);
        verse.setOrderIndex(orderIndex);
        verse.setUpdatedAt(Instant.now());

        if (rawText != null) {
            boolean textChanged = !rawText.equals(verse.getRawText());
            verse.setRawText(rawText);
            if (textChanged) {
                // Очистить результаты анализа
                verseAnalysisRepository.deleteByVerseId(id);
                verseWordRepository.deleteAllByVerseId(id);
                verse.setTextDevanagari(null);
                verse.setTextIast(null);
                verse.setStatus(VerseStatus.DRAFT);
            }
        }

                return verseRepository.save(verse);
    }

        /**
     * Кнопка «Изучить» на VersePage — POST /verses/{verseId}/vocabulary-quiz.
     * Если у стиха уже есть закэшированный vocabularyQuizSlug — возвращает его сразу.
     * Иначе синхронно вызывает content-service, кэширует результат и возвращает.
     */
    /**
     * Кнопка «Изучить» на VersePage — POST /verses/{verseId}/vocabulary-quiz.
     * Если у стиха уже есть закэшированный vocabularyQuizSlug — возвращает его сразу.
     * Иначе синхронно вызывает content-service, кэширует результат и возвращает.
     */
    @Transactional
    public VocabularyQuizResponse getOrCreateVocabularyQuiz(UUID verseId) {
        Verse verse = getVerseById(verseId);

        boolean quizAlreadyExists = verse.getVocabularyQuizSlug() != null
                && !verse.getVocabularyQuizSlug().isBlank()
                && verse.getVocabularyQuizId() != null;

        List<VerseWord> words = verseWordRepository.findAllByVerseIdOrderByPositionAsc(verseId);

        if (quizAlreadyExists) {
            boolean allMapped = words.stream().allMatch(w -> w.getVocabularyWordId() != null);
            if (allMapped) {
                return new VocabularyQuizResponse(
                        verse.getVocabularyQuizSlug(),
                        verse.getVocabularyQuizId(),
                        "EXISTING");
            }
            // Некоторые слова не имеют vocabularyWordId — проваливаемся дальше для дозаполнения
        } else {
            if (verse.getStatus() != VerseStatus.ANALYZED) {
                throw new RuntimeException("Verse is not analyzed yet: " + verseId);
            }
            if (words.isEmpty()) {
                throw new RuntimeException("Verse has no words: " + verseId);
            }
        }

        Chapter chapter = chapterRepository.findById(verse.getChapterId())
                .orElseThrow(() -> new RuntimeException("Chapter not found: " + verse.getChapterId()));
        Work work = workService.getWorkById(chapter.getWorkId());

        // Дедуп слов стиха по (lemmaIast, stem) перед отправкой
        Map<String, VerseWord> deduped = new LinkedHashMap<>();
        for (VerseWord w : words) {
            String key = w.getLemmaIast() + "|" + w.getStem();
            deduped.putIfAbsent(key, w);
        }

        List<SangrahaVocabularyEvent.SangrahaVocabularyWord> vocabWords = deduped.values().stream()
                .map(w -> SangrahaVocabularyEvent.SangrahaVocabularyWord.builder()
                        .verseWordId(w.getId())
                        .wordIast(w.getLemmaIast())
                        .wordDevanagari(w.getSurfaceDevanagari())
                                                .stem(w.getStem())
                        .root(w.getRoot())
                        .gender(w.getMorphology() != null && w.getMorphology().getGender() != null
                                ? w.getMorphology().getGender().name() : null)
                        .translationRu(w.getContextGlossRu())
                        .translationEn(w.getContextGlossEn())
                        .build())
                .toList();

        SangrahaVocabularyEvent request = SangrahaVocabularyEvent.builder()
                .verseId(verseId)
                .workSlug(work.getSlug())
                .workTitleRu(work.getTitleRu())
                .workTitleEn(work.getTitleEn())
                .chapterSlug(chapter.getSlug())
                .chapterTitleRu(chapter.getTitleRu())
                .chapterTitleEn(chapter.getTitleEn())
                .verseOrderIndex(verse.getOrderIndex())
                .words(vocabWords)
                .build();

        SangrahaVocabularyResponse response = contentServiceVocabularyClient.requestVocabularyQuiz(request);

        if (!quizAlreadyExists) {
            verse.setVocabularyQuizSlug(response.getQuizSlug());
            verse.setVocabularyQuizId(response.getQuizId());
            verse.setUpdatedAt(Instant.now());
            verseRepository.save(verse);
        }

        // Сохранить маппинг verseWordId → vocabularyWordId в verse_words
        // Используем ключ lemmaIast|stem, чтобы корректно обработать слова-дубли
        if (response.getWordMappings() != null && !response.getWordMappings().isEmpty()) {
            // verseWordId → lemmaIast|stem (из дедуплицированных представителей)
            Map<UUID, String> verseWordIdToKey = new HashMap<>();
            for (var entry : deduped.entrySet()) {
                verseWordIdToKey.put(entry.getValue().getId(), entry.getKey());
            }

            // lemmaIast|stem → vocabularyWordId
            Map<String, UUID> lemmaStemToVocabId = new HashMap<>();
            for (var m : response.getWordMappings()) {
                String key = verseWordIdToKey.get(m.getVerseWordId());
                if (key != null) {
                    lemmaStemToVocabId.put(key, m.getVocabularyWordId());
                }
            }

            for (VerseWord w : words) {
                String key = w.getLemmaIast() + "|" + w.getStem();
                UUID vocabId = lemmaStemToVocabId.get(key);
                if (vocabId != null) {
                    w.setVocabularyWordId(vocabId);
                }
            }
            verseWordRepository.saveAll(words);
        }

        String status = quizAlreadyExists ? "EXISTING" : response.getQuizStatus();
        return new VocabularyQuizResponse(
                quizAlreadyExists ? verse.getVocabularyQuizSlug() : response.getQuizSlug(),
                quizAlreadyExists ? verse.getVocabularyQuizId() : response.getQuizId(),
                status);
    }
}

