package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.content.model.Lesson;
import sm.selflearn.samskrtam.content.model.VocabularyCategory;
import sm.selflearn.samskrtam.content.model.VocabularyWord;
import sm.selflearn.samskrtam.content.model.VocabularyWordCategory;
import sm.selflearn.samskrtam.content.model.VocabularyWordCategoryId;
import sm.selflearn.samskrtam.content.model.Gender;
import sm.selflearn.samskrtam.content.repository.LessonRepository;
import sm.selflearn.samskrtam.content.repository.VocabularyCategoryRepository;
import sm.selflearn.samskrtam.content.repository.VocabularyWordCategoryRepository;
import sm.selflearn.samskrtam.content.repository.VocabularyWordRepository;
import sm.selflearn.samskrtam.sangraha.event.SangrahaVocabularyEvent;
import sm.selflearn.samskrtam.sangraha.event.SangrahaVocabularyEvent.SangrahaVocabularyWord;
import sm.selflearn.samskrtam.content.dto.SangrahaVocabularyResponse;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.content.dto.Difficulty;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VocabularySyncService {

    private final VocabularyCategoryRepository categoryRepository;
    private final VocabularyWordRepository wordRepository;
    private final VocabularyWordCategoryRepository wordCategoryRepository;
    private final LessonRepository lessonRepository;

        /**
     * Обрабатывает запрос sangraha-service на создание лексического квиза стиха.
     * Идемпотентно: повторный вызов с тем же verseId возвращает тот же quizSlug.
     * Строит иерархию категорий work → chapter → verse, upsert-ит Quiz(VOCABULARY)
     * и линкует слова к chapterCategory (тема) и verseCategory (квиз).
     *
     * @param event событие от sangraha-service
     * @return ответ с quizSlug (verseCode)
     */
                @Transactional
    public SangrahaVocabularyResponse processEvent(SangrahaVocabularyEvent event) {
        String workSlug = event.getWorkSlug();
        String chapterCode = workSlug + "." + event.getChapterSlug();
        String verseCode = chapterCode + ".verse-" + event.getVerseId();
        String verseTitleRu = event.getWorkTitleRu() + ", стих " + event.getVerseOrderIndex();
        String verseTitleEn = event.getWorkTitleEn() + ", verse " + event.getVerseOrderIndex();

        // 1. Upsert root category (work level) — без изменений
        VocabularyCategory rootCategory = upsertCategory(
                workSlug, null, event.getWorkTitleRu(), event.getWorkTitleEn());

        // 2. Upsert chapter category — без изменений
        VocabularyCategory chapterCategory = upsertCategory(
                chapterCode, rootCategory.getId(), event.getChapterTitleRu(), event.getChapterTitleEn());

        // 3. NEW: Upsert verse-level category — квиз строится на ней, а не на chapterCategory
        VocabularyCategory verseCategory = upsertCategory(
                verseCode, chapterCategory.getId(), verseTitleRu, verseTitleEn);

        // 4. Upsert Lesson(VOCABULARY) — slug = verseCode, теперь возвращает (Lesson, wasCreated)
        UpsertQuizResult quizResult = upsertQuiz(verseCode, verseTitleRu, verseTitleEn);

        // 5. Process each word — линкуем и на chapterCategory (тема), и на verseCategory (квиз)
        if (event.getWords() != null) {
            for (SangrahaVocabularyWord w : event.getWords()) {
                processWord(w, chapterCategory, verseCategory);
            }
        }

        log.info("Processed vocabulary-quiz request: verseId={}, workSlug={}, chapterSlug={}, wordsCount={}, quizSlug={}, quizId={}, wasCreated={}",
                event.getVerseId(), workSlug, event.getChapterSlug(),
                event.getWords() != null ? event.getWords().size() : 0,
                verseCode, quizResult.lesson().getId(), quizResult.wasCreated());

        return SangrahaVocabularyResponse.builder()
                .quizSlug(verseCode)
                .quizId(quizResult.lesson().getId())
                .quizStatus(quizResult.wasCreated() ? "CREATED" : "EXISTING")
                .build();
    }

    /** Пара (Lesson, был ли он только что создан этим вызовом) — нужна для quizStatus в ответе. */
    private record UpsertQuizResult(Lesson lesson, boolean wasCreated) {}

    /**
     * Upsert VocabularyCategory: ищет по code, если нет — создаёт.
     */
    private VocabularyCategory upsertCategory(String code, UUID parentId, String nameRu, String nameEn) {
        Optional<VocabularyCategory> existing = categoryRepository.findByCodeIgnoreCase(code);
        if (existing.isPresent()) {
            return existing.get();
        }
        VocabularyCategory category = VocabularyCategory.builder()
                .code(code)
                .parentId(parentId)
                .nameRu(nameRu)
                .nameEn(nameEn)
                .build();
        return categoryRepository.save(category);
    }

                /**
     * Upsert Quiz(VOCABULARY) по slug.
     * Возвращает (Lesson, wasCreated) — нужно для quizStatus в ответе.
     */
    private UpsertQuizResult upsertQuiz(String slug, String titleRu, String titleEn) {
        Optional<Lesson> existing = lessonRepository.findBySlug(slug);
        if (existing.isPresent()) {
            return new UpsertQuizResult(existing.get(), false);
        }
        Lesson lesson = new Lesson();
        lesson.setSlug(slug);
        lesson.setTitleRu(titleRu);
        lesson.setTitleEn(titleEn);
        lesson.setLessonType(LessonType.VOCABULARY);
        lesson.setDifficulty(Difficulty.BEGINNER);
        lesson.setQuestionsPerSession(10);
        lesson.setCreatedAt(Instant.now());
        Lesson saved = lessonRepository.save(lesson);
        log.info("Created new VOCABULARY quiz: slug={}, titleRu={}", slug, titleRu);
        return new UpsertQuizResult(saved, true);
    }

        /**
     * Обрабатывает одно слово из события.
     * Dedup по (wordIast, stem). Если слово уже существует — только добавляет
     * связи VocabularyWordCategory, если их ещё нет.
     * Если слова нет — создаёт и линкует к обеим категориям (chapter + verse).
     */
        private VocabularyWord processWord(SangrahaVocabularyWord w, VocabularyCategory chapterCategory, VocabularyCategory verseCategory) {
        String wordIast = w.getWordIast();
        String stem = w.getStem();

        if (wordIast == null || wordIast.isBlank()) {
            log.warn("Skipping word with empty wordIast");
            return null;
        }
        if (stem == null || stem.isBlank()) {
            stem = wordIast;
        }

        Optional<VocabularyWord> existingWord = wordRepository.findByWordIastAndStem(wordIast, stem);
        VocabularyWord word;
        if (existingWord.isPresent()) {
            word = existingWord.get();
        } else {
            Gender gender = parseGender(w.getGender());
            word = VocabularyWord.builder()
                    .wordIast(wordIast)
                    .wordDevanagari(w.getWordDevanagari() != null ? w.getWordDevanagari() : "")
                    .stem(stem)
                    .root(w.getRoot())
                    .gender(gender)
                    .translationRu(w.getTranslationRu() != null ? w.getTranslationRu() : "")
                    .translationEn(w.getTranslationEn() != null ? w.getTranslationEn() : "")
                    .explanationRu(w.getExplanationRu() != null ? w.getExplanationRu() : "")
                    .explanationEn(w.getExplanationEn() != null ? w.getExplanationEn() : "")
                    .build();
            word = wordRepository.save(word);
        }

        linkWordToCategory(word, chapterCategory);
        linkWordToCategory(word, verseCategory);
        return word;
    }

    private void linkWordToCategory(VocabularyWord word, VocabularyCategory category) {
        VocabularyWordCategoryId linkId = new VocabularyWordCategoryId(word.getId(), category.getId());
        if (!wordCategoryRepository.existsById(linkId)) {
            VocabularyWordCategory link = VocabularyWordCategory.builder()
                    .id(linkId)
                    .vocabularyWord(word)
                    .category(category)
                    .createdAt(Instant.now())
                    .build();
            wordCategoryRepository.save(link);
        }
    }

    /**
     * Парсит строковое представление gender из события sangraha.
     * null и пустые строки → UNSPECIFIED.
     */
    private Gender parseGender(String genderStr) {
        if (genderStr == null || genderStr.isBlank()) {
            return Gender.UNSPECIFIED;
        }
        try {
            return Gender.valueOf(genderStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown gender value '{}', falling back to UNSPECIFIED", genderStr);
            return Gender.UNSPECIFIED;
        }
    }
}