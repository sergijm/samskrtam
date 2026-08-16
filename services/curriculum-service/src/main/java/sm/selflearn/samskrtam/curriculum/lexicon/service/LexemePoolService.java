package sm.selflearn.samskrtam.curriculum.lexicon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexemeCandidateDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.PoolCriteria;
import sm.selflearn.samskrtam.curriculum.lexicon.imports.LexiconImportService;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeLexicalTopic;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SemanticClass;
import sm.selflearn.samskrtam.curriculum.lexicon.model.UserLexemeProgress;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeFrequencyRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeLexicalTopicRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.UserCollectionItemRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.UserLexemeProgressRepository;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Разрешение пула кандидатов для lexical-сессий (lexical-quizzes.md §3,
 * task-curriculum-15). База пула — все лексемы; тема-фильтр резолвится как
 * объединение семантических классов темы ({@code semantic_class_topic} →
 * {@code lexeme_semantic_class}) и явных привязок {@code lexeme_lexical_topic}.
 * Переданные
 * измерения пересекаются (AND), значения внутри измерения объединяются (OR).
 * Балансировка: квота на тему при topicIds.size() &gt; 1 и финальный reshuffle
 * «не более 2 подряд одного posCode».
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LexemePoolService {

    public static final int MASTERY_MASTERED = 90;

    private final LexemeRepository lexemeRepository;
    private final TopicRepository topicRepository;
    private final LexemeFrequencyRepository lexemeFrequencyRepository;
    private final UserCollectionItemRepository userCollectionItemRepository;
    private final UserLexemeProgressRepository userLexemeProgressRepository;
    private final LexemeLexicalTopicRepository lexemeLexicalTopicRepository;

    @Transactional(readOnly = true)
    public List<LexemeCandidateDto> resolve(PoolCriteria criteria) {
        if (criteria == null) {
            criteria = new PoolCriteria(List.of(), null, null, List.of(), List.of(),
                    null, null, 100);
        }

        List<UUID> ids = baseIds();
        ids = applyTopicFilter(ids, criteria.topicIds());
        ids = applyFrequencyFilter(ids, criteria.frequencyRankMin(), criteria.frequencyRankMax());
        ids = applyPosFilter(ids, criteria.posCodes());
        ids = applyMorphologyFilter(ids, criteria.morphologyClassCodes());
        ids = applyCollectionFilter(ids, criteria.collectionId());
        ids = applyMasteredExclusion(ids, criteria.excludeMasteredForUserId());

        List<Lexeme> lexemes = lexemeRepository.findWithDetailsByIdIn(ids);

        if (criteria.topicIds() != null && criteria.topicIds().size() > 1) {
            lexemes = applyTopicQuota(lexemes, criteria.topicIds(), criteria.effectivePoolLimit());
        }
        lexemes = truncate(lexemes, criteria.effectivePoolLimit());
        lexemes = reshuffleNoConsecutivePos(lexemes);

        return lexemes.stream().map(this::toDto).toList();
    }

    private List<UUID> baseIds() {
        return lexemeRepository.findAll()
                .stream()
                .map(Lexeme::getId)
                .toList();
    }

    private List<UUID> applyTopicFilter(List<UUID> ids, List<UUID> topicIds) {
        if (topicIds == null || topicIds.isEmpty()) {
            return ids;
        }
        Set<UUID> allowed = new HashSet<>();
        topicLexemeIds(topicIds).values().forEach(allowed::addAll);
        return intersect(ids, allowed);
    }

    /**
     * topicId → set of lexeme ids. A LEXICON topic's lexemes come from BOTH
     * sources (lexical-curriculum.md §1): classified lexemes via the topic's
     * semantic classes ({@code semantic_class_topic} → {@code lexeme_semantic_class})
     * and explicit bindings via {@code lexeme_lexical_topic} (unclassified / VERSE
     * lessons). Topics with neither contribute nothing.
     */
    private Map<UUID, Set<UUID>> topicLexemeIds(Collection<UUID> topicIds) {
        Map<UUID, Set<UUID>> result = new LinkedHashMap<>();
        for (Topic topic : topicRepository.findAllById(topicIds)) {
            Set<UUID> lexemeIds = new HashSet<>();
            Set<UUID> semanticClassIds = topic.getSemanticClasses().stream()
                    .map(SemanticClass::getId)
                    .collect(java.util.stream.Collectors.toSet());
            if (!semanticClassIds.isEmpty()) {
                lexemeIds.addAll(lexemeRepository.findLexemeIdsBySemanticClassIds(semanticClassIds));
            }
            lexemeIds.addAll(lexemeLexicalTopicRepository.findByIdLexicalTopicId(topic.getId())
                    .stream()
                    .map(binding -> binding.getId().getLexemeId())
                    .toList());
            result.put(topic.getId(), lexemeIds);
        }
        return result;
    }

    private List<UUID> applyFrequencyFilter(List<UUID> ids, Integer min, Integer max) {
        if (min == null && max == null) {
            return ids;
        }
        List<UUID> ranked = lexemeFrequencyRepository.findLexemeIdsBySourceAndRankRange(
                LexiconImportService.FREQUENCY_SOURCE, min, max);
        return intersect(ids, new HashSet<>(ranked));
    }

    private List<UUID> applyPosFilter(List<UUID> ids, List<String> posCodes) {
        if (posCodes == null || posCodes.isEmpty()) {
            return ids;
        }
        List<Lexeme> byPos = lexemeRepository.findByPartsOfSpeech_CodeIn(posCodes);
        Set<UUID> allowed = new HashSet<>();
        byPos.forEach(l -> allowed.add(l.getId()));
        return intersect(ids, allowed);
    }

    private List<UUID> applyMorphologyFilter(List<UUID> ids, List<String> morphologyCodes) {
        if (morphologyCodes == null || morphologyCodes.isEmpty()) {
            return ids;
        }
        List<Lexeme> byMorph = lexemeRepository.findByMorphologyClasses_CodeIn(morphologyCodes);
        Set<UUID> allowed = new HashSet<>();
        byMorph.forEach(l -> allowed.add(l.getId()));
        return intersect(ids, allowed);
    }

    private List<UUID> applyCollectionFilter(List<UUID> ids, UUID collectionId) {
        if (collectionId == null) {
            return ids;
        }
        Set<UUID> fromCollection = new HashSet<>();
        userCollectionItemRepository.findByIdCollectionId(collectionId)
                .forEach(item -> fromCollection.add(item.getId().getLexemeId()));
        return intersect(ids, fromCollection);
    }

    private List<UUID> applyMasteredExclusion(List<UUID> ids, UUID userId) {
        if (userId == null) {
            return ids;
        }
        Set<UUID> excluded = new HashSet<>();
        for (UserLexemeProgress progress : userLexemeProgressRepository.findByIdUserId(userId)) {
            if (progress.getMasteryScore() != null && progress.getMasteryScore() >= MASTERY_MASTERED) {
                excluded.add(progress.getId().getLexemeId());
            }
        }
        List<UUID> result = new ArrayList<>();
        for (UUID id : ids) {
            if (!excluded.contains(id)) {
                result.add(id);
            }
        }
        return result;
    }

    /**
     * Квота на тему: не более ceil(poolLimit / topicIds.size()) + 2 лексем от
     * одной темы. Если лексема привязана к нескольким запрошенным темам, она
     * считается в каждой (перепредставленность режется случайной выборкой).
     */
    private List<Lexeme> applyTopicQuota(List<Lexeme> lexemes, List<UUID> topicIds, int poolLimit) {
        int perTopicQuota = (int) Math.ceil((double) poolLimit / topicIds.size()) + 2;

        Map<UUID, Set<UUID>> byTopic = topicLexemeIds(topicIds);
        Map<UUID, List<Lexeme>> grouped = new LinkedHashMap<>();
        for (UUID topicId : topicIds) {
            Set<UUID> lexemeIds = byTopic.getOrDefault(topicId, Set.of());
            grouped.put(topicId, lexemes.stream().filter(l -> lexemeIds.contains(l.getId())).toList());
        }

        Set<UUID> selected = new HashSet<>();
        List<Lexeme> result = new ArrayList<>();
        for (UUID topicId : topicIds) {
            List<Lexeme> candidates = new ArrayList<>(grouped.getOrDefault(topicId, List.of()));
            Collections.shuffle(candidates);
            int taken = 0;
            for (Lexeme candidate : candidates) {
                if (taken >= perTopicQuota) {
                    break;
                }
                if (selected.add(candidate.getId())) {
                    result.add(candidate);
                    taken++;
                }
            }
        }
        return result;
    }

    private List<Lexeme> truncate(List<Lexeme> lexemes, int limit) {
        return lexemes.size() <= limit ? lexemes : new ArrayList<>(lexemes.subList(0, limit));
    }

    /**
     * Финальный reshuffle: не более 2 подряд одного posCode (lexical-quizzes.md
     * §3 п.2). Жадный: если третий подряд — обменять с ближайшей лексемой
     * другого posCode.
     */
    static List<Lexeme> reshuffleNoConsecutivePos(List<Lexeme> lexemes) {
        List<Lexeme> result = new ArrayList<>(lexemes);
        int i = 0;
        while (i < result.size()) {
            String first = posCode(result.get(i));
            if (first == null) {
                i++;
                continue;
            }
            int run = 1;
            int j = i + 1;
            while (j < result.size() && run < 2 && first.equals(posCode(result.get(j)))) {
                run++;
                j++;
            }
            if (j < result.size() && run == 2 && first.equals(posCode(result.get(j)))) {
                int swapWith = -1;
                for (int k = j + 1; k < result.size(); k++) {
                    if (!first.equals(posCode(result.get(k)))) {
                        swapWith = k;
                        break;
                    }
                }
                if (swapWith >= 0) {
                    Collections.swap(result, j, swapWith);
                    i = j - 1;
                    continue;
                }
            }
            i = j;
        }
        return result;
    }

    private static String posCode(Lexeme lexeme) {
        return lexeme.getPartsOfSpeech().isEmpty()
                ? null
                : lexeme.getPartsOfSpeech().iterator().next().getCode();
    }

    private static List<UUID> intersect(List<UUID> ids, Set<UUID> allowed) {
        List<UUID> result = new ArrayList<>();
        for (UUID id : ids) {
            if (allowed.contains(id)) {
                result.add(id);
            }
        }
        return result;
    }

    private LexemeCandidateDto toDto(Lexeme lexeme) {
        List<LexemeCandidateDto.WordFormDto> wordForms = lexeme.getWordForms().stream()
                .limit(3)
                .map(wf -> new LexemeCandidateDto.WordFormDto(
                        wf.getFormIast(), wf.getFormDevanagari(), wf.getGrammaticalNote()))
                .toList();
        String posCode = lexeme.getPartsOfSpeech().isEmpty()
                ? null : lexeme.getPartsOfSpeech().iterator().next().getCode();
        List<String> morphologyCodes = lexeme.getMorphologyClasses().stream()
                .map(mc -> mc.getCode())
                .toList();
        return new LexemeCandidateDto(
                lexeme.getId(),
                lexeme.getLemmaIast(),
                lexeme.getLemmaDevanagari(),
                lexeme.getLemmaSlp1(),
                lexeme.getGlossRu(),
                lexeme.getGlossEn(),
                lexeme.getGender() == null ? null : lexeme.getGender().name(),
                posCode,
                morphologyCodes,
                wordForms);
    }
}
