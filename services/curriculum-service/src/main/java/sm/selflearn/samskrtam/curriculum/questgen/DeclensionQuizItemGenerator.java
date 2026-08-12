package sm.selflearn.samskrtam.curriculum.questgen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.questgen.morphology.CaseType;
import sm.selflearn.samskrtam.curriculum.questgen.morphology.NumberType;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.quest.GrammarQuestItemTypes;
import sm.selflearn.samskrtam.quest.QuestItemType;
import sm.selflearn.samskrtam.quest.declension.CaseRecognitionPayload;
import sm.selflearn.samskrtam.quest.declension.DeclensionFormPayload;
import sm.selflearn.samskrtam.quest.declension.DeclensionMatchPayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Generates the four DECLENSION quest types ({@code DECLENSION_FORM},
 * {@code DECLENSION_FORM_CHOICE}, {@code CASE_RECOGNITION},
 * {@code DECLENSION_MATCH}) for the regular noun topics of the curriculum.
 *
 * <p>The topic slug maps to one or more {@code morphology_class.code}s (the
 * merged {@code a-stem} lesson covers both {@code a-stem-masc} and
 * {@code a-stem-neut}, {@code i-u-stems} covers {@code i-stem}/{@code u-stem},
 * {@code r-stems} covers {@code r-stem}); every lexeme bound to one of those
 * classes is a candidate. No more than {@value #MAX_LEXEMES} lexemes are used
 * per topic (random sample when there are more), and for each lexeme all
 * possible questions are composed from its paradigm cells — see
 * curriculum-quest-items.md §4.
 *
 * <p>Word forms are composed from the lemma and the canonical paradigm endings
 * ({@link DeclensionNounParadigmComposer}, ported from
 * {@code content.case_endings}). Every produced row carries a {@code progress_tag}
 * ({@code caseType|numberType|gender}; {@code DECLENSION_MATCH} takes the first
 * pair and {@code gender=UNSPECIFIED}), see migration V13.
 */
@Service
@RequiredArgsConstructor
public class DeclensionQuizItemGenerator extends QuizItemGenerator {

    public static final String GENERATOR_SOURCE = "DECLENSION_BATCH";

    /** Random sample size of lexemes per topic ("no more than 10 basic words"). */
    static final int MAX_LEXEMES = 10;

    private static final Random RANDOM = new Random();

    /** Topic slug → the morphology classes the topic's declension is built from. */
    private static final Map<String, List<String>> SLUG_CLASS_CODES = Map.ofEntries(
            Map.entry("a-stem", List.of("a-stem-masc", "a-stem-neut")), // merged lesson (V10)
            Map.entry("a-stem-fem", List.of("a-stem-fem")),
            Map.entry("i-u-stems", List.of("i-stem", "u-stem")),
            Map.entry("r-stems", List.of("r-stem"))
    );

    private final LexemeRepository lexemeRepository;
    private final QuestItemRepository questItemRepository;
    private final DeclensionMatchProperties matchProperties;
    private final ObjectMapper objectMapper;

    /** One paradigm cell: (case, number) plus the composed word form. */
    private record Cell(CaseType caseType, NumberType numberType, DeclensionNounParadigmComposer.Form form) {
    }

    @Override
    public Set<String> supportedTopicSlugs() {
        return SLUG_CLASS_CODES.keySet();
    }

    @Override
    @Transactional
    public int generate(Topic topic) {
        List<String> classCodes = SLUG_CLASS_CODES.get(topic.getCode());
        if (classCodes == null) {
            return 0;
        }

        List<Lexeme> lexemes = lexemeRepository.findWithMorphologyByCodeIn(classCodes);
        if (lexemes.isEmpty()) {
            return 0;
        }
        if (lexemes.size() > MAX_LEXEMES) {
            List<Lexeme> shuffled = new ArrayList<>(lexemes);
            Collections.shuffle(shuffled, RANDOM);
            lexemes = shuffled.subList(0, MAX_LEXEMES);
        }

        Map<String, Set<LexemeGender>> formGenders = collectFormGenders(classCodes, lexemes);

        List<QuestItem> items = new ArrayList<>();
        for (Lexeme lexeme : lexemes) {
            String classCode = resolveClassCode(lexeme, classCodes);
            if (classCode == null) {
                continue;
            }
            LexemeGender gender = resolveGender(classCode, lexeme.getGender());
            List<Cell> cells = cells(classCode, lexeme);
            for (Cell cell : cells) {
                buildForm(topic, lexeme, classCode, gender, cell, items);
                buildFormChoice(topic, lexeme, classCode, gender, cell, cells, items);
                buildCaseRecognition(topic, lexeme, classCode, gender, cell, cells, formGenders, items);
            }
            buildMatch(topic, lexeme, classCode, gender, cells, items);
        }

        return persist(items);
    }

    private void buildForm(Topic topic, Lexeme lexeme, String classCode, LexemeGender gender,
                           Cell cell, List<QuestItem> items) {
        DeclensionFormPayload payload = formPayload(lexeme, classCode, gender, cell);
        String progressTag = progressTag(cell, gender);
        items.add(buildItem(topic, GrammarQuestItemTypes.DECLENSION_FORM,
                prompt(lexeme, cell, false), cell.form().iast(), List.of(), payload, progressTag));
    }

    private void buildFormChoice(Topic topic, Lexeme lexeme, String classCode, LexemeGender gender,
                                 Cell cell, List<Cell> cells, List<QuestItem> items) {
        List<String> distractors = choiceDistractors(cells, cell);
        DeclensionFormPayload payload = formPayload(lexeme, classCode, gender, cell);
        items.add(buildItem(topic, GrammarQuestItemTypes.DECLENSION_FORM_CHOICE,
                prompt(lexeme, cell, true), cell.form().iast(), distractors, payload,
                progressTag(cell, gender)));
    }

    private void buildCaseRecognition(Topic topic, Lexeme lexeme, String classCode, LexemeGender gender,
                                      Cell cell, List<Cell> cells,
                                      Map<String, Set<LexemeGender>> formGenders, List<QuestItem> items) {
        boolean genderRequired = formGenders.getOrDefault(cell.form().iast(), Set.of()).size() > 1;
        List<String> distractorCombinations = caseCombinations(cells, cell, gender, genderRequired);

        String correctLabel = label(cell.caseType(), cell.numberType(), gender, genderRequired);
        String prompt = "Identify the case" + (genderRequired ? ", number and gender" : " and number")
                + " of the word form '" + cell.form().iast() + "'.";

        CaseRecognitionPayload payload = new CaseRecognitionPayload(
                cell.form().iast(),
                cell.form().devanagari(),
                lexeme.getLemmaIast(),
                classCode,
                cell.caseType().name(),
                cell.numberType().name(),
                gender.name(),
                genderRequired,
                distractorCombinations);

        items.add(buildItem(topic, GrammarQuestItemTypes.CASE_RECOGNITION,
                prompt, correctLabel, distractorCombinations, payload,
                progressTag(cell, gender)));
    }

    private void buildMatch(Topic topic, Lexeme lexeme, String classCode, LexemeGender gender,
                            List<Cell> cells, List<QuestItem> items) {
        int pairsPerItem = matchProperties.getPairsPerItem();
        if (pairsPerItem <= 0) {
            return;
        }
        for (int from = 0; from < cells.size(); from += pairsPerItem) {
            List<Cell> block = cells.subList(from, Math.min(from + pairsPerItem, cells.size()));
            if (block.isEmpty()) {
                continue;
            }
            Cell leading = block.get(0);
            List<DeclensionMatchPayload.DeclensionMatchPair> pairs = new ArrayList<>(block.size());
            for (Cell cell : block) {
                pairs.add(new DeclensionMatchPayload.DeclensionMatchPair(
                        UUID.randomUUID().toString(),
                        cell.form().iast(),
                        cell.form().devanagari(),
                        cell.caseType().name(),
                        cell.numberType().name()));
            }
            DeclensionMatchPayload payload = new DeclensionMatchPayload(
                    lexeme.getLemmaIast(), classCode, pairs);

            String progressTag = leading.caseType() + "|" + leading.numberType() + "|" + LexemeGender.UNSPECIFIED;
            items.add(buildItem(topic, GrammarQuestItemTypes.DECLENSION_MATCH,
                    "Match each word form of '" + lexeme.getLemmaIast() + "' to its case and number.",
                    null, List.of(), payload, progressTag));
        }
    }

    // ----------------------------------------------------------------------
    // Composition helpers
    // ----------------------------------------------------------------------

    private QuestItem buildItem(Topic topic, QuestItemType itemType,
                                String prompt, String correctAnswer, List<String> distractors,
                                Object payload, String progressTag) {
        QuestItem item = new QuestItem();
        item.setTopicId(topic.getId());
        item.setItemType(itemType.code());
        item.setAnswerMode(itemType.defaultAnswerMode().name());
        item.setPrompt(prompt);
        item.setCorrectAnswer(correctAnswer);
        item.setDistractors(toJson(distractors));
        item.setPayload(toJson(payload));
        item.setProgressTag(progressTag);
        item.setGeneratorSource(GENERATOR_SOURCE);
        return item;
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
            throw new IllegalStateException("Failed to serialize quest item content", e);
        }
    }

    private String prompt(Lexeme lexeme, Cell cell, boolean choice) {
        String verb = choice ? "Choose the correct" : "Enter the correct";
        return verb + " " + cell.caseType().name().toLowerCase() + " " + cell.numberType().name().toLowerCase()
                + " form of '" + lexeme.getLemmaIast() + "'.";
    }

    private DeclensionFormPayload formPayload(Lexeme lexeme, String classCode, LexemeGender gender, Cell cell) {
        return new DeclensionFormPayload(
                lexeme.getLemmaIast(),
                lexeme.getLemmaDevanagari(),
                classCode,
                gender.name(),
                cell.caseType().name(),
                cell.numberType().name(),
                cell.form().iast(),
                cell.form().devanagari());
    }

    private String progressTag(Cell cell, LexemeGender gender) {
        return cell.caseType().name() + "|" + cell.numberType().name() + "|" + gender.name();
    }

    private String label(CaseType caseType, NumberType numberType, LexemeGender gender, boolean withGender) {
        String base = titleCase(caseType.name()) + " " + titleCase(numberType.name());
        return withGender ? base + " " + titleCase(gender.name()) : base;
    }

    private static String titleCase(String value) {
        return value.charAt(0) + value.substring(1).toLowerCase();
    }

    private List<String> choiceDistractors(List<Cell> cells, Cell correct) {
        Set<String> others = new LinkedHashSet<>();
        for (Cell cell : cells) {
            if (!cell.form().iast().equals(correct.form().iast())) {
                others.add(cell.form().iast());
            }
        }
        List<String> candidates = new ArrayList<>(others);
        Collections.shuffle(candidates, RANDOM);
        return candidates.subList(0, Math.min(3, candidates.size()));
    }

    private List<String> caseCombinations(List<Cell> cells, Cell correct,
                                          LexemeGender gender, boolean withGender) {
        List<String> combos = new ArrayList<>();
        for (Cell cell : cells) {
            if (cell.caseType() == correct.caseType() && cell.numberType() == correct.numberType()) {
                continue;
            }
            if (cell.form().iast().equals(correct.form().iast())) {
                continue; // ambiguous cell — useless as a distractor
            }
            combos.add(label(cell.caseType(), cell.numberType(), gender, withGender));
            if (combos.size() == 3) {
                break;
            }
        }
        return combos;
    }

    private List<Cell> cells(String classCode, Lexeme lexeme) {
        return DeclensionNounParadigmComposer.compose(
                        classCode, lexeme.getLemmaIast(), lexeme.getLemmaDevanagari(), lexeme.getGender())
                .stream()
                .map(c -> new Cell(c.caseType(), c.numberType(), c.form()))
                .toList();
    }

    /**
     * Collects, across all lexemes of the topic, the set of genders that produce
     * each distinct word form. Used to decide {@code genderRequired}: a form is
     * grammatically ambiguous without gender iff more than one gender yields it.
     */
    private Map<String, Set<LexemeGender>> collectFormGenders(List<String> classCodes, List<Lexeme> lexemes) {
        Map<String, Set<LexemeGender>> result = new LinkedHashMap<>();
        for (Lexeme lexeme : lexemes) {
            String classCode = resolveClassCode(lexeme, classCodes);
            if (classCode == null) {
                continue;
            }
            LexemeGender gender = resolveGender(classCode, lexeme.getGender());
            for (Cell cell : cells(classCode, lexeme)) {
                result.computeIfAbsent(cell.form().iast(), k -> new HashSet<>()).add(gender);
            }
        }
        return result;
    }

    // ----------------------------------------------------------------------
    // Morphology class mapping (topic slug -> lexeme's morphology class)
    // ----------------------------------------------------------------------

    /**
     * Which of the topic's morphology classes the lexeme is actually bound to.
     * Iterates {@code classCodes} in priority order and returns the first code
     * present in the lexeme's (already fetched) class set; {@code null} when the
     * lexeme belongs to none of them.
     */
    private String resolveClassCode(Lexeme lexeme, List<String> classCodes) {
        if (lexeme.getMorphologyClasses() == null) {
            return null;
        }
        for (String code : classCodes) {
            boolean bound = lexeme.getMorphologyClasses().stream().anyMatch(mc -> mc.getCode().equals(code));
            if (bound) {
                return code;
            }
        }
        return null;
    }

    private LexemeGender resolveGender(String classCode, LexemeGender lexemeGender) {
        return switch (classCode) {
            case "a-stem-masc" -> LexemeGender.MASCULINE;
            case "a-stem-neut" -> LexemeGender.NEUTER;
            case "a-stem-fem" -> LexemeGender.FEMININE;
            case "i-stem", "u-stem" ->
                    lexemeGender == LexemeGender.NEUTER ? LexemeGender.NEUTER : LexemeGender.MASCULINE;
            case "r-stem" ->
                    lexemeGender == LexemeGender.FEMININE ? LexemeGender.FEMININE : LexemeGender.MASCULINE;
            default -> lexemeGender == null ? LexemeGender.UNSPECIFIED : lexemeGender;
        };
    }
}