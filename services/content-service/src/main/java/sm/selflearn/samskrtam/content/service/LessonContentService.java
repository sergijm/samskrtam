package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.LessonItemResponse;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.content.model.Lesson;
import sm.selflearn.samskrtam.content.repository.LessonRepository;
import sm.selflearn.samskrtam.content.repository.VocabularyCategoryRepository;
import sm.selflearn.samskrtam.content.repository.VocabularyWordCategoryRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonContentService {

    private final LessonRepository lessonRepository;
    private final VocabularyCategoryRepository vocabularyCategoryRepository;
    private final VocabularyWordCategoryRepository vocabularyWordCategoryRepository;

    public List<LessonItemResponse> getLessonsList(String category) {
        return lessonRepository.findAll().stream()
                .filter(lesson -> {
                    if (category == null) {
                        return true;
                    }
                    switch (category.toLowerCase()) {
                        case "declensions":
                            return LessonType.isDeclensions(lesson.getLessonType());
                        case "conjugations":
                            return lesson.getLessonType() == LessonType.CONJUGATIONS;
                        case "vocabulary":
                            return LessonType.isVocabulary(lesson.getLessonType());
                        case "vocabulary-basic":
                        case "vocabulary_basic":
                            return lesson.getLessonType() == LessonType.VOCABULARY_BASIC;
                        case "vocabulary-text":
                        case "vocabulary-texts":
                        case "vocabulary_texts":
                            return lesson.getLessonType() == LessonType.VOCABULARY_TEXTS;
                        case "grammar":
                            return !LessonType.isVocabulary(lesson.getLessonType());
                        default:
                            try {
                                return lesson.getLessonType() == LessonType.valueOf(category.toUpperCase());
                            } catch (IllegalArgumentException e) {
                                return true;
                            }
                    }
                })
                .map(this::mapToLessonItemResponse)
                .collect(Collectors.toList());
    }

    public LessonItemResponse getLessonItemBySlug(String slug) {
        log.debug("getLessonBySlug called with slug: {}", slug);
        return lessonRepository.findBySlug(slug)
                .map(this::mapToLessonItemResponse)
                .orElseThrow(() -> new SamskrtamException("LESSON_NOT_FOUND", "Lesson not found with slug: " + slug));
    }

    public LessonItemResponse getLessonItemById(UUID lessonId) {
        log.debug("getLessonById called with lessonId: {}", lessonId);
        return lessonRepository.findById(lessonId)
                .map(this::mapToLessonItemResponse)
                .orElseThrow(() -> new SamskrtamException("LESSON_NOT_FOUND", "Lesson not found with ID: " + lessonId));
    }

    private LessonItemResponse mapToLessonItemResponse(Lesson lesson) {
        int wordCount = 0;
        if (LessonType.isVocabulary(lesson.getLessonType())) {
            wordCount = vocabularyCategoryRepository.findByCodeIgnoreCase(lesson.getSlug())
                    .map(category -> {
                        List<UUID> allCategoryIds = vocabularyCategoryRepository.findAllChildrenIds(category.getId());
                        return vocabularyWordCategoryRepository.countByCategoryIdIn(allCategoryIds);
                    })
                    .orElse(0);
        }

        return LessonItemResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitleEn())
                .titleRu(lesson.getTitleRu())
                .titleEn(lesson.getTitleEn())
                .description(lesson.getDescriptionEn())
                .descriptionRu(lesson.getDescriptionRu())
                .descriptionEn(lesson.getDescriptionEn())
                .lessonType(lesson.getLessonType())
                .difficulty(lesson.getDifficulty())
                .slug(lesson.getSlug())
                .totalQuestions(lesson.getQuestionsPerSession())
                .wordCount(wordCount)
                .build();
    }
}