package sm.selflearn.samskrtam.curriculum.questgen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.questgen.morphology.CaseType;
import sm.selflearn.samskrtam.curriculum.questgen.morphology.NumberType;
import sm.selflearn.samskrtam.curriculum.questgen.morphology.VowelType;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItemGenerationKey;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemGenerationKeyRepository;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;
import sm.selflearn.samskrtam.quest.GrammarQuestItemTypes;
import sm.selflearn.samskrtam.quest.QuestItemType;
import sm.selflearn.samskrtam.quest.declension.CaseRecognitionPayload;
import sm.selflearn.samskrtam.quest.declension.DeclensionFormPayload;
import sm.selflearn.samskrtam.quest.declension.DeclensionMatchPayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Offline batch generator that materializes the four DECLENSION quest types
 * ({@code DECLENSION_FORM}, {@code DECLENSION_FORM_CHOICE},
 * {@code CASE_RECOGNITION}, {@code DECLENSION_MATCH}) for a single grammatical
 * {@link Topic}. The Topic's code doubles as the {@code morphology_class.code}
 * it is bound to (e.g. {@code a-stem-masc}), so every lexeme linked to that
 * class is a candidate. See curriculum-quest-items.md §4.
 *
 * <p>Word forms are composed from the lexeme lemma and the canonical paradigm
 * endings of {@link DeclensionParadigm} (ported from content-service's
 * {@code content.case_endings}); the paradigm lookup is resolved by the lexeme's
 * gender for i-/u-/ṛ-stems and by a fixed gender for the a-/ā-stems.
 *
 * <p>Generation is idempotent: every produced item records a row in
 * {@code quest_item_generation_key} and existing keys short-circuit re-creation,
 * so re-running the batch for a topic never duplicates items for the same
 * (topic, itemType, lexeme, case, number).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeclensionQuestItemBatchGenerator {

    public static final String GENERATOR_SOURCE = "DECLENSION_BATCH";
    private static final Random RANDOM = new Random();

    private final TopicRepository topicRepository;
    private final LexemeRepository lexemeRepository;
    private final QuestItemRepository questItemRepository;
    private final QuestItemGenerationKeyRepository generationKeyRepository;
    private final DeclensionMatchProperties matchProperties;
    private final ObjectMapper objectMapper;

    private record ClassBinding(VowelType vowelType, LexemeGender fixedGender) {
    }

    /** One paradigm cell: (case, number) plus the composed word form. */
    private record Cell(CaseType caseType, NumberType numberType, WordFormBuilder.Form form) {
    }

    /** A not-yet-persisted item plus its idempotency key. */
    private record Pending(QuestItem item, String generationKey) {
    }

    /** The three groups of quest types the batch generator can produce in one pass. */
    public enum GenerationGroup {
        /** DECLENSION_FORM + DECLENSION_FORM_CHOICE, built together from the same paradigm cells. */
        FORMS,
        /** CASE_RECOGNITION. */
        CASE_RECOGNITION,
        /** DECLENSION_MATCH (blocked into pairs). */
        MATCH
    }

    /**
     * Generates all four declension quest types for a Topic.
     *
     * @return number of new quest items actually created (0 when everything was
     *         already generated or the topic is not a declension class)
     */
    @Transactional
    public int generateForTopic(UUID topicId, int lexemeLimit) {
        return generate(topicId, EnumSet.allOf(GenerationGroup.class), lexemeLimit);
    }

    @Transactional
    public int generateFormsForTopic(UUID topicId, int lexemeLimit) {
        return generate(topicId, EnumSet.of(GenerationGroup.FORMS), lexemeLimit);
    }

    @Transactional
    public int generateCaseRecognitionForTopic(UUID topicId, int lexemeLimit) {
        return generate(topicId, EnumSet.of(GenerationGroup.CASE_RECOGNITION), lexemeLimit);
    }

    @Transactional
    public int generateMatchForTopic(UUID topicId, int lexemeLimit) {
        return generate(topicId, EnumSet.of(GenerationGroup.MATCH), lexemeLimit);
    }

    private int generate(UUID topicId, Set<GenerationGroup> groups, int lexemeLimit) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new EntityNotFoundException("Topic not found: " + topicId));

        ClassBinding binding = bindMorphologyClass(topic.getCode());
        if (binding == null) {
            return 0;
        }

        List<Lexeme> lexemes = lexemeRepository.findByMorphologyClasses_Code(topic.getCode());
        if (lexemes.isEmpty()) {
            return 0;
        }

        if (lexemeLimit > 0 && lexemeLimit < lexemes.size()) {
            Collections.shuffle(lexemes, RANDOM);
            lexemes = lexemes.subList(0, lexemeLimit);
        }

        String classCode = topic.getCode();
        Map<String, Set<LexemeGender>> formGenders = collectFormGenders(binding, lexemes);

        List<Pending> pending = new ArrayList<>();
        for (Lexeme lexeme : lexemes) {
            LexemeGender gender = resolveGender(binding, lexeme.getGender());
            List<Cell> cells = cells(lexeme, binding, gender);
            if (groups.contains(GenerationGroup.FORMS)) {
                buildFormsItems(topic, classCode, lexeme, gender, cells, pending);
            }
            if (groups.contains(GenerationGroup.CASE_RECOGNITION)) {
                buildCaseRecognitionItems(topic, classCode, lexeme, gender, cells, formGenders, pending);
            }
            if (groups.contains(GenerationGroup.MATCH)) {
                buildMatchItems(topic, classCode, lexeme, gender, cells, pending);
            }
        }

        return persist(pending);
    }

    private void buildFormsItems(Topic topic, String classCode, Lexeme lexeme, LexemeGender gender,
                                 List<Cell> cells, List<Pending> pending) {
        for (Cell cell : cells) {
            buildFormItem(topic, classCode, lexeme, gender, cell, pending);
            buildFormChoiceItem(topic, classCode, lexeme, gender, cell, cells, pending);
        }
    }

    private void buildCaseRecognitionItems(Topic topic, String classCode, Lexeme lexeme, LexemeGender gender,
                                           List<Cell> cells, Map<String, Set<LexemeGender>> formGenders,
                                           List<Pending> pending) {
        for (Cell cell : cells) {
            boolean genderRequired = requiresGender(formGenders, cell.form().iast());
            buildCaseRecognitionItem(topic, classCode, lexeme, gender, cell, cells, genderRequired, pending);
        }
    }

    private void buildMatchItems(Topic topic, String classCode, Lexeme lexeme, LexemeGender gender,
                                 List<Cell> cells, List<Pending> pending) {
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
            String key = generationKey(topic.getId(), GrammarQuestItemTypes.DECLENSION_MATCH.code(),
                    lexeme.getId(), leading.caseType(), leading.numberType());
            if (generationKeyRepository.existsByGenerationKey(key)) {
                continue;
            }

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

            pending.add(new Pending(buildItem(topic, GrammarQuestItemTypes.DECLENSION_MATCH,
                    "Match each word form of '" + lexeme.getLemmaIast() + "' to its case and number.",
                    null, List.of(), payload, key), key));
        }
    }

    private void buildFormItem(Topic topic, String classCode, Lexeme lexeme, LexemeGender gender,
                               Cell cell, List<Pending> pending) {
        String key = generationKey(topic.getId(), GrammarQuestItemTypes.DECLENSION_FORM.code(),
                lexeme.getId(), cell.caseType(), cell.numberType());
        if (generationKeyRepository.existsByGenerationKey(key)) {
            return;
        }
        DeclensionFormPayload payload = formPayload(lexeme, classCode, gender, cell);
        pending.add(new Pending(buildItem(topic, GrammarQuestItemTypes.DECLENSION_FORM,
                prompt(lexeme, cell, false), cell.form().iast(), List.of(), payload, key), key));
    }

    private void buildFormChoiceItem(Topic topic, String classCode, Lexeme lexeme, LexemeGender gender,
                                     Cell cell, List<Cell> cells, List<Pending> pending) {
        String key = generationKey(topic.getId(), GrammarQuestItemTypes.DECLENSION_FORM_CHOICE.code(),
                lexeme.getId(), cell.caseType(), cell.numberType());
        if (generationKeyRepository.existsByGenerationKey(key)) {
            return;
        }
        List<String> distractors = choiceDistractors(cells, cell);
        DeclensionFormPayload payload = formPayload(lexeme, classCode, gender, cell);
        pending.add(new Pending(buildItem(topic, GrammarQuestItemTypes.DECLENSION_FORM_CHOICE,
                prompt(lexeme, cell, true), cell.form().iast(), distractors, payload, key), key));
    }

    private void buildCaseRecognitionItem(Topic topic, String classCode, Lexeme lexeme, LexemeGender gender,
                                          Cell cell, List<Cell> cells, boolean genderRequired,
                                          List<Pending> pending) {
        String key = generationKey(topic.getId(), GrammarQuestItemTypes.CASE_RECOGNITION.code(),
                lexeme.getId(), cell.caseType(), cell.numberType());
        if (generationKeyRepository.existsByGenerationKey(key)) {
            return;
        }
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

        pending.add(new Pending(buildItem(topic, GrammarQuestItemTypes.CASE_RECOGNITION,
                prompt, correctLabel, distractorCombinations, payload, key), key));
    }

    // ----------------------------------------------------------------------
    // Composition helpers
    // ----------------------------------------------------------------------

    private QuestItem buildItem(Topic topic, QuestItemType itemType,
                                String prompt, String correctAnswer, List<String> distractors,
                                Object payload, String generationKey) {
        QuestItem item = new QuestItem();
        item.setTopicId(topic.getId());
        item.setItemType(itemType.code());
        item.setAnswerMode(itemType.defaultAnswerMode().name());
        item.setPrompt(prompt);
        item.setCorrectAnswer(correctAnswer);
        item.setDistractors(toJson(distractors));
        item.setPayload(toJson(payload));
        item.setGeneratorSource(GENERATOR_SOURCE);
        return item;
    }

    private int persist(List<Pending> pending) {
        if (pending.isEmpty()) {
            return 0;
        }
        List<QuestItem> saved = questItemRepository.saveAll(pending.stream().map(Pending::item).toList());
        List<QuestItemGenerationKey> keys = new ArrayList<>(pending.size());
        for (int i = 0; i < pending.size(); i++) {
            QuestItemGenerationKey genKey = new QuestItemGenerationKey();
            genKey.setQuestItemId(saved.get(i).getId());
            genKey.setGenerationKey(pending.get(i).generationKey());
            keys.add(genKey);
        }
        generationKeyRepository.saveAll(keys);
        return saved.size();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize quest item content", e);
        }
    }

    private String generationKey(UUID topicId, String itemType, UUID lexemeId,
                                 CaseType caseType, NumberType numberType) {
        return topicId + ":" + itemType + ":" + lexemeId + ":" + caseType.name() + ":" + numberType.name();
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

    private List<String> caseCombinations(List<Cell> cells, Cell correct, LexemeGender gender, boolean withGender) {
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

    private List<Cell> cells(Lexeme lexeme, ClassBinding binding, LexemeGender gender) {
        List<Cell> cells = new ArrayList<>();
        for (CaseType caseType : CaseType.values()) {
            for (NumberType numberType : NumberType.values()) {
                DeclensionParadigm.Ending ending =
                        DeclensionParadigm.ending(binding.vowelType(), gender, caseType, numberType);
                if (ending == null) {
                    continue;
                }
                WordFormBuilder.Form form = WordFormBuilder.compose(
                        lexeme.getLemmaIast(), lexeme.getLemmaDevanagari(),
                        ending.endingIast(), ending.endingDevanagari());
                cells.add(new Cell(caseType, numberType, form));
            }
        }
        return cells;
    }

    /**
     * Collects, across all lexemes of the class, the set of genders that produce
     * each distinct word form. Used to decide {@code genderRequired}: a form is
     * grammatically ambiguous without gender iff more than one gender yields it.
     */
    private Map<String, Set<LexemeGender>> collectFormGenders(ClassBinding binding, List<Lexeme> lexemes) {
        Map<String, Set<LexemeGender>> result = new LinkedHashMap<>();
        for (Lexeme lexeme : lexemes) {
            LexemeGender gender = resolveGender(binding, lexeme.getGender());
            for (Cell cell : cells(lexeme, binding, gender)) {
                result.computeIfAbsent(cell.form().iast(), k -> new HashSet<>()).add(gender);
            }
        }
        return result;
    }

    /** True when the given word form can belong to more than one gender within the class. */
    static boolean requiresGender(Map<String, Set<LexemeGender>> formGenders, String formIast) {
        return formGenders != null && formGenders.getOrDefault(formIast, Set.of()).size() > 1;
    }

    // ----------------------------------------------------------------------
    // Morphology class mapping (topic code = morphology_class.code)
    // ----------------------------------------------------------------------

    private ClassBinding bindMorphologyClass(String code) {
        return switch (code) {
            case "a-stem-masc" -> new ClassBinding(VowelType.A_STEM, LexemeGender.MASCULINE);
            case "a-stem-neut" -> new ClassBinding(VowelType.A_STEM, LexemeGender.NEUTER);
            case "a-stem-fem" -> new ClassBinding(VowelType.AA_STEM, LexemeGender.FEMININE);
            case "i-stem" -> new ClassBinding(VowelType.I_STEM, null);
            case "u-stem" -> new ClassBinding(VowelType.U_STEM, null);
            case "r-stem" -> new ClassBinding(VowelType.R_STEM, null);
            default -> null;
        };
    }

    private LexemeGender resolveGender(ClassBinding binding, LexemeGender lexemeGender) {
        if (binding.fixedGender() != null) {
            return binding.fixedGender();
        }
        LexemeGender gender = lexemeGender == null ? LexemeGender.UNSPECIFIED : lexemeGender;
        return switch (binding.vowelType()) {
            case R_STEM -> gender == LexemeGender.FEMININE ? LexemeGender.FEMININE : LexemeGender.MASCULINE;
            default -> gender == LexemeGender.NEUTER ? LexemeGender.NEUTER : LexemeGender.MASCULINE;
        };
    }
}
