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
import sm.selflearn.samskrtam.content.event.VocabularyWordSyncedEvent;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.content.dto.Difficulty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    private final VocabularyAckPublisher vocabularyAckPublisher;

    /**
     * Обрабатывает событие sangraha-vocabulary-events.
     * Идемпотентно: повторный вызов с тем же event не создаёт дубликатов.
     *
     * @param event событие от sangraha-service
     */
    @Transactional
    public void processEvent(SangrahaVocabularyEvent event) {
        String workSlug = event.getWorkSlug();
        String chapterCode = workSlug + "." + event.getChapterSlug();

        // 1. Upsert root category (work level)
        VocabularyCategory rootCategory = upsertCategory(
                workSlug,
                null,
                event.getWorkTitleRu(),
                event.getWorkTitleEn()
        );

        // 2. Upsert chapter category
        VocabularyCategory chapterCategory = upsertCategory(
                chapterCode,
                rootCategory.getId(),
                event.getChapterTitleRu(),
                event.getChapterTitleEn()
        );

        // 3. Upsert Quiz (VOCABULARY) at work level (slug = workSlug)
        upsertQuiz(workSlug, event.getWorkTitleRu(), event.getWorkTitleEn());

        // 4. Process each word, collect WordSync for ack
        List<VocabularyWordSyncedEvent.WordSync> wordSyncList = new ArrayList<>();
        if (event.getWords() != null) {
            for (SangrahaVocabularyWord w : event.getWords()) {
                VocabularyWord savedWord = processWord(w, chapterCategory);
                if (savedWord != null && w.getVerseWordId() != null) {
                    wordSyncList.add(VocabularyWordSyncedEvent.WordSync.builder()
                            .verseWordId(w.getVerseWordId())
                            .vocabularyWordId(savedWord.getId())
                            .build());
                }
            }
        }

        // 5. Publish ack to sangraha-service
        if (!wordSyncList.isEmpty()) {
            vocabularyAckPublisher.publish(event.getVerseId(), wordSyncList);
        }

        log.info("Processed sangraha event: verseId={}, workSlug={}, chapterSlug={}, wordsCount={}, syncedCount={}",
                event.getVerseId(), workSlug, event.getChapterSlug(),
                event.getWords() != null ? event.getWords().size() : 0,
                wordSyncList.size());
    }

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
     * Upsert Quiz(VOCABULARY) по slug = workSlug.
     * Если квиз с таким slug уже существует — не перезаписываем.
     */
    private void upsertQuiz(String slug, String titleRu, String titleEn) {
        Optional<Lesson> existing = lessonRepository.findBySlug(slug);
        if (existing.isPresent()) {
            return;
        }
        Lesson lesson = new Lesson();
        lesson.setSlug(slug);
        lesson.setTitleRu(titleRu);
        lesson.setTitleEn(titleEn);
        lesson.setLessonType(LessonType.VOCABULARY);
        lesson.setDifficulty(Difficulty.BEGINNER);
        lesson.setQuestionsPerSession(10);
        lesson.setCreatedAt(Instant.now());
        lessonRepository.save(lesson);
        log.info("Created new VOCABULARY quiz: slug={}, titleRu={}", slug, titleRu);
    }

    /**
     * Обрабатывает одно слово из события.
     * Dedup по (wordIast, stem). Если слово уже существует — только добавляет
     * связь VocabularyWordCategory, если её ещё нет.
     * Если слова нет — создаёт и сразу связывает с категорией главы.
     */
    private VocabularyWord processWord(SangrahaVocabularyWord w, VocabularyCategory chapterCategory) {
        String wordIast = w.getWordIast();
        String stem = w.getStem();

        if (wordIast == null || wordIast.isBlank()) {
            log.warn("Skipping word with empty wordIast");
            return null;
        }
        if (stem == null || stem.isBlank()) {
            stem = wordIast;
        }

        // Dedup by (wordIast, stem) — ищем существующее слово
        Optional<VocabularyWord> existingWord = wordRepository.findByWordIastAndStem(wordIast, stem);
        VocabularyWord word;
        if (existingWord.isPresent()) {
            word = existingWord.get();
            log.debug("Found existing vocabulary word: id={}, wordIast={}", word.getId(), wordIast);
        } else {
            // Создаём новое слово
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
            log.debug("Created new vocabulary word: id={}, wordIast={}", word.getId(), wordIast);
        }

        // Upsert связь с категорией главы
        VocabularyWordCategoryId linkId = new VocabularyWordCategoryId(word.getId(), chapterCategory.getId());
        if (!wordCategoryRepository.existsById(linkId)) {
            VocabularyWordCategory link = VocabularyWordCategory.builder()
                    .id(linkId)
                    .vocabularyWord(word)
                    .category(chapterCategory)
                    .createdAt(Instant.now())
                    .build();
            wordCategoryRepository.save(link);
            log.debug("Linked word {} to category {}", word.getId(), chapterCategory.getId());
        }
        return word;
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