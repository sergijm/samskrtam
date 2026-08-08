package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.dto.GrammarLesson;
import sm.selflearn.samskrtam.quiz.dto.GrammarQuestionProgress;
import sm.selflearn.samskrtam.quiz.dto.LessonStatusSummary;
import sm.selflearn.samskrtam.quiz.dto.ProgressTagInfo;
import sm.selflearn.samskrtam.quiz.dto.TopicLessonDto;
import sm.selflearn.samskrtam.quiz.dto.WordStatus;
import sm.selflearn.samskrtam.quiz.localization.CaseNumberGenderLocalizer;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.model.QuizItemScore;
import sm.selflearn.samskrtam.quiz.repository.QuizItemScoreRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds a grammar lesson (progress grid) for a curriculum topic (API v2). The lesson
 * base data (metadata + unique progress tags with morphology attributes) comes from
 * curriculum-service via {@link CurriculumClient}; per-tag progress is read from
 * {@code quiz_item_score} (keyed as the resolved ItemType + progress_tag) and merged
 * in. This is the v2 replacement of the content-service-based {@link GrammarProgressBuilder}.
 */
@Service
@RequiredArgsConstructor
public class GrammarLessonV2Service {

    private final CurriculumClient curriculumClient;
    private final QuizItemScoreRepository quizItemScoreRepository;
    private final WordStatusResolver wordStatusResolver;

    public Mono<GrammarLesson> build(String topicCode, UUID userId) {
        return curriculumClient.fetchTopicLesson(topicCode)
                .flatMap(lesson -> {
                    Map<String, ProgressTagInfo> metadata = lesson.tagMetadata();
                    if (userId == null) {
                        return Mono.just(assemble(lesson, metadata, Map.of(), Instant.now()));
                    }
                    ItemType itemType = resolveItemType(metadata);
                    List<String> tags = List.copyOf(metadata.keySet());
                    return quizItemScoreRepository
                            .findByUserIdAndItemTypeAndProgressTagIn(userId, itemType, tags)
                            .collectMap(QuizItemScore::getProgressTag, score -> score)
                            .map(scoresMap -> assemble(lesson, metadata, scoresMap, Instant.now()));
                });
    }

    private ItemType resolveItemType(Map<String, ProgressTagInfo> metadata) {
        // Determine itemType from the first metadata entry
        for (ProgressTagInfo info : metadata.values()) {
            if (info.itemType() != null) {
                return QuestProgressTypes.resolve(info.itemType());
            }
        }
        return ItemType.DECLENSION_FORM;
    }

    private GrammarLesson assemble(
            TopicLessonDto lesson,
            Map<String, ProgressTagInfo> metadata,
            Map<String, QuizItemScore> scoresMap,
            Instant now) {

        int distinctCells = metadata.size();
        int newCount = 0;
        int learning = 0;
        int mastered = 0;
        int reviewDue = 0;

        List<GrammarQuestionProgress> items = new ArrayList<>();
        for (Map.Entry<String, ProgressTagInfo> entry : metadata.entrySet()) {
            String tag = entry.getKey();
            ProgressTagInfo info = entry.getValue();
            QuizItemScore score = scoresMap.get(tag);
            WordStatus status = wordStatusResolver.resolve(score, now);

            switch (status) {
                case NEW -> newCount++;
                case LEARNING -> learning++;
                case MASTERED -> mastered++;
                case REVIEW -> reviewDue++;
            }

            items.add(toProgress(tag, info, score, status, now));
        }

        int learned = mastered + reviewDue;

        GrammarLesson l = new GrammarLesson();
        l.setLessonId(lesson.topicId());
        l.setType("DECLENSIONS"); // v2 grammar topics are declension-family
        l.setTitleRu(lesson.titleRu());
        l.setTitleEn(lesson.titleEn());
        l.setDifficulty(lesson.learningLevel());
        l.setTotalQuestions(distinctCells);
        l.setLearnedQuestions(learned);
        l.setProgressPercent(distinctCells > 0 ? (float) learned / distinctCells * 100f : 0f);
        l.setStatusSummary(new LessonStatusSummary(distinctCells, newCount, learning, mastered, reviewDue));
        l.setItems(items);
        return l;
    }

    private GrammarQuestionProgress toProgress(
            String tag, ProgressTagInfo info, QuizItemScore score, WordStatus status, Instant now) {
        String caseType = info.caseType();
        String numberType = info.numberType();
        String gender = info.gender() != null ? info.gender() : "UNSPECIFIED";

        GrammarQuestionProgress p = new GrammarQuestionProgress();
        p.setQuestionId(UUID.nameUUIDFromBytes(tag.getBytes()));
        p.setTextRu(contentLabel("ru", caseType, numberType, info));
        p.setTextEn(contentLabel("en", caseType, numberType, info));
        p.setCaseType(caseType);
        p.setCaseRu(CaseNumberGenderLocalizer.caseTypeRu(caseType));
        p.setCaseEn(CaseNumberGenderLocalizer.caseTypeEn(caseType));
        p.setNumberType(numberType);
        p.setNumberRu(CaseNumberGenderLocalizer.numberTypeRu(numberType));
        p.setNumberEn(CaseNumberGenderLocalizer.numberTypeEn(numberType));
        p.setGender(gender);
        p.setGenderRu(CaseNumberGenderLocalizer.genderRu(gender));
        p.setGenderEn(CaseNumberGenderLocalizer.genderEn(gender));
        p.setScore(score != null ? score.getScore() : 0);
        p.setStatus(status);
        return p;
    }

    private String contentLabel(String lang, String caseType, String numberType, ProgressTagInfo info) {
        String caseName = "ru".equals(lang)
                ? CaseNumberGenderLocalizer.caseTypeRu(caseType)
                : CaseNumberGenderLocalizer.caseTypeEn(caseType);
        String numberName = "ru".equals(lang)
                ? CaseNumberGenderLocalizer.numberTypeRu(numberType)
                : CaseNumberGenderLocalizer.numberTypeEn(numberType);
        if (caseName != null || numberName != null) {
            return (caseName != null ? caseName : "") + ", " + (numberName != null ? numberName : "");
        }
        return info.formIast() != null ? info.formIast() : "?";
    }
}