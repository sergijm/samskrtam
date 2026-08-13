package sm.selflearn.samskrtam.curriculum.questgen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;
import sm.selflearn.samskrtam.quest.QuestItemType;
import sm.selflearn.samskrtam.quest.VocabularyQuestItemTypes;
import sm.selflearn.samskrtam.quest.lexicon.VocabularyWordPayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates the VOCABULARY_WORD quest type (recognition direction:
 * lemma -> meaning) for every lexical topic (domain {@code LEXICON}, slug
 * prefix {@code lex-}, see V6). One {@code quest_item} row per lexeme bound to
 * the topic; distractors are the glosses of other lexemes of the same topic.
 * Rows without a gloss in either language, and topics with too few glossed
 * lexemes for a meaningful choice, are skipped.
 *
 * <p>Every produced row carries {@code progress_tag = lemmaSlp1} and is fully
 * bilingual ({@code *_ru} columns mirror the English content), matching the
 * localization convention of the declension generator.
 */
@Service
@RequiredArgsConstructor
public class LexicalQuizItemGenerator extends QuizItemGenerator {

    public static final String GENERATOR_SOURCE = "LEXICAL_BATCH";

    /** Max number of distractor glosses per choice item. */
    static final int DISTRACTOR_COUNT = 3;

    private static final Random RANDOM = new Random();

    private final TopicRepository topicRepository;
    private final LexemeRepository lexemeRepository;
    private final QuestItemRepository questItemRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Set<String> supportedTopicSlugs() {
        return topicRepository.findByDomain(TopicDomain.LEXICON).stream()
                .map(Topic::getCode)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @Transactional
    public int generate(Topic topic) {
        List<Lexeme> lexemes = lexemeRepository.findByLexicalTopics_Code(topic.getCode());
        if (lexemes.size() < DISTRACTOR_COUNT + 1) {
            return 0;
        }

        List<Lexeme> glossed = lexemes.stream()
                .filter(LexicalQuizItemGenerator::isGlossed)
                .collect(Collectors.toList());
        if (glossed.size() < DISTRACTOR_COUNT + 1) {
            return 0;
        }

        List<QuestItem> items = new ArrayList<>(glossed.size());
        for (Lexeme lexeme : glossed) {
            List<String[]> distractors = distractors(glossed, lexeme);
            if (distractors.size() < DISTRACTOR_COUNT) {
                continue;
            }
            List<String> distractorsEn = new ArrayList<>(DISTRACTOR_COUNT);
            List<String> distractorsRu = new ArrayList<>(DISTRACTOR_COUNT);
            for (String[] pair : distractors) {
                distractorsEn.add(pair[0]);
                distractorsRu.add(pair[1]);
            }
            items.add(buildItem(topic, lexeme, distractorsEn, distractorsRu));
        }
        return persist(items);
    }

    private static boolean isGlossed(Lexeme lexeme) {
        return notBlank(lexeme.getGlossEn()) && notBlank(lexeme.getGlossRu());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private List<String[]> distractors(List<Lexeme> glossed, Lexeme correct) {
        Set<GlossPair> seen = new LinkedHashSet<>();
        List<String[]> candidates = new ArrayList<>();
        for (Lexeme lexeme : glossed) {
            if (lexeme.getId().equals(correct.getId())) {
                continue;
            }
            String glossEn = lexeme.getGlossEn();
            String glossRu = lexeme.getGlossRu();
            if (glossEn.equals(correct.getGlossEn()) || glossRu.equals(correct.getGlossRu())) {
                continue;
            }
            if (!seen.add(new GlossPair(glossEn, glossRu))) {
                continue;
            }
            candidates.add(new String[]{glossEn, glossRu});
            if (candidates.size() == DISTRACTOR_COUNT) {
                break;
            }
        }
        Collections.shuffle(candidates, RANDOM);
        return candidates;
    }

    private record GlossPair(String en, String ru) {
    }

    private QuestItem buildItem(Topic topic, Lexeme lexeme,
                                List<String> distractorsEn, List<String> distractorsRu) {
        String lemmaDevanagari = lexeme.getLemmaDevanagari();
        VocabularyWordPayload payload = new VocabularyWordPayload(
                lexeme.getLemmaSlp1(),
                lexeme.getLemmaIast(),
                lemmaDevanagari,
                lexeme.getGlossEn(),
                lexeme.getGlossRu());

        QuestItem item = new QuestItem();
        item.setTopicId(topic.getId());
        item.setItemType(VocabularyQuestItemTypes.VOCABULARY_WORD.code());
        item.setAnswerMode(VocabularyQuestItemTypes.VOCABULARY_WORD.defaultAnswerMode().name());
        item.setPrompt("What does '" + lemmaDevanagari + "' mean?");
        item.setPromptRu("Что значит " + quoteRu(lemmaDevanagari) + "?");
        item.setCorrectAnswer(lexeme.getGlossEn());
        item.setCorrectAnswerRu(lexeme.getGlossRu());
        item.setDistractors(toJson(distractorsEn));
        item.setDistractorsRu(toJson(distractorsRu));
        item.setPayload(toJson(payload));
        item.setProgressTag(lexeme.getLemmaSlp1());
        item.setGeneratorSource(GENERATOR_SOURCE);
        return item;
    }

    private static String quoteRu(String value) {
        return "«" + value + "»";
    }

    private int persist(List<QuestItem> items) {
        if (items.isEmpty()) {
            return 0;
        }
        return questItemRepository.saveAll(items).size();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize quest item payload", e);
        }
    }
}
