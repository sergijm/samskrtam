package sm.selflearn.samskrtam.curriculum.questgen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.content.dto.frisch.FrischEntryDto;
import sm.selflearn.samskrtam.content.dto.frisch.FrischGenderDto;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.morphology.NumberType;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.curriculum.dictionary.DictionaryClient;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.service.TransliterationService;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.paradigm.DeclensionClassMapper;
import sm.selflearn.samskrtam.curriculum.paradigm.ParadigmForm;
import sm.selflearn.samskrtam.curriculum.paradigm.ParadigmFormRepository;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.quest.GrammarQuestItemTypes;
import sm.selflearn.samskrtam.quest.HighlightToken;
import sm.selflearn.samskrtam.quest.QuestItemType;
import sm.selflearn.samskrtam.quest.QuestPatterns;
import sm.selflearn.samskrtam.quest.declension.CaseRecognitionPayload;
import sm.selflearn.samskrtam.quest.declension.DeclensionFormPayload;
import sm.selflearn.samskrtam.quest.declension.DeclensionMatchPayload;
import sm.selflearn.samskrtam.quiz.localization.CaseNumberGenderLocalizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Generates the four DECLENSION quest types ({@code DECLENSION_FORM},
 * {@code DECLENSION_FORM_CHOICE}, {@code CASE_RECOGNITION},
 * {@code DECLENSION_MATCH}) for declension topics of the curriculum
 * (regular noun/adjective classes and pronoun classes alike).
 *
 * <p>Кандидаты берутся напрямую из {@code curriculum.declension_form}
 * (уникальный {@code lemma_iast} по {@code vowel_type} темы через
 * {@link DeclensionClassMapper}) — сущность {@code Lexeme} не используется.
 * Род и переводы подтягиваются из словаря Фриша через dictionary-service
 * (REST, {@link DictionaryClient}); для местоимений, чья базовая лемма в Фрише
 * отсутствует, делается повторный поиск по форме именительного падежа
 * единственного числа из {@code declension_form}.
 *
 * <p>Paradigm cells are read from {@code curriculum.declension_form}
 * ({@link ParadigmFormRepository}), keyed by {@code (lemma_iast, vowel_type)};
 * nothing is composed at generator time. Every produced row carries a
 * {@code progress_tag} ({@code caseType|numberType|gender};
 * {@code DECLENSION_MATCH} takes the first pair and {@code gender=UNSPECIFIED}),
 * see migration V13.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeclensionQuizItemGenerator extends QuizItemGenerator {

    public static final String GENERATOR_SOURCE = "DECLENSION_BATCH";

    /** Random sample size of lemmas per topic ("no more than 10 basic words"). */
    static final int MAX_LEXEMES = 10;

    private static final Random RANDOM = new Random();

    /** Родительный падеж (для «формы … падежа») русских названий падежей. */
    private static final Map<String, String> CASE_GENITIVE_RU = Map.ofEntries(
            Map.entry("NOMINATIVE", "именительного"),
            Map.entry("ACCUSATIVE", "винительного"),
            Map.entry("INSTRUMENTAL", "творительного"),
            Map.entry("DATIVE", "дательного"),
            Map.entry("ABLATIVE", "отложительного"),
            Map.entry("GENITIVE", "родительного"),
            Map.entry("LOCATIVE", "местного"),
            Map.entry("VOCATIVE", "звательного"));

    /** Родительный падеж (для «… числа») русских названий чисел. */
    private static final Map<String, String> NUMBER_GENITIVE_RU = Map.ofEntries(
            Map.entry("SINGULAR", "единственного"),
            Map.entry("DUAL", "двойственного"),
            Map.entry("PLURAL", "множественного"));

    private final QuestItemRepository questItemRepository;
    private final ParadigmFormRepository paradigmFormRepository;
    private final DeclensionMatchProperties matchProperties;
    private final ObjectMapper objectMapper;
    private final DictionaryClient dictionaryClient;
    private final TransliterationService transliterationService;

    /** One paradigm cell: (case, number) plus the word form. */
    protected record Cell(CaseType caseType, NumberType numberType, Form form) {
    }

    /** One surface word form. */
    protected record Form(String iast, String devanagari) {
    }

    /** Declension stem decoupled from Lexeme: a lemma + its grammatical context. */
    protected record Stem(
            String lemmaIast,
            String lemmaDevanagari,
            LexemeGender gender,
            VowelType vowelType,
            String classCode,
            String glossRu,
            String glossEn) {
    }


    @Override
    public boolean isDomainSupported(TopicDomain domain) {
        return domain == TopicDomain.NOMINAL_MORPHOLOGY || domain == TopicDomain.PRONOUNS;
    }

    @Override
    @Transactional
    public int generate(Topic topic) {
        List<VowelType> vowelTypes = DeclensionClassMapper.topicToVowelTypes(topic.getCode());
        if (vowelTypes.isEmpty()) {
            return 0;
        }

        // уникальные леммы из curriculum.declension_form (с привязанным vowel_type)
        Map<String, VowelType> lemmaVowelType = new LinkedHashMap<>();
        for (ParadigmFormRepository.LemmaVowelType p :
                paradigmFormRepository.findDistinctLemmaVowelTypeByVowelTypeIn(vowelTypes)) {
            lemmaVowelType.putIfAbsent(p.getLemmaIast(), p.getVowelType());
        }

        List<String> lemmas = new ArrayList<>(lemmaVowelType.keySet());
        if (lemmas.size() > MAX_LEXEMES) {
            Collections.shuffle(lemmas, RANDOM);
            lemmas = lemmas.subList(0, MAX_LEXEMES);
        }

        List<Stem> stems = new ArrayList<>();
        for (String lemma : lemmas) {
            VowelType vowelType = lemmaVowelType.get(lemma);
            stems.add(toStem(lemma, vowelType));
        }

        Map<String, Set<LexemeGender>> formGenders = collectFormGenders(stems);

        List<QuestItem> items = new ArrayList<>();
        for (Stem stem : stems) {
            List<Cell> cells = cells(stem.vowelType(), stem.lemmaIast());
            items.addAll(buildItemsForStem(topic, stem, cells, formGenders));
        }

        return persist(items);
    }

    /** Builds a {@link Stem} from a declension_form lemma, enriching it with Frisch data. */
    private Stem toStem(String lemma, VowelType vowelType) {
        List<FrischEntryDto> frischEntries = dictionaryClient.getFrischLemma(lemma);

        // Для местоимений базовая лемма в Фрише часто отсутствует — ищем по форме
        // именительного падежа единственного числа из declension_form.
        if (frischEntries.isEmpty() && isPronoun(vowelType)) {
            String nominativeSingular = nominativeSingularFormIast(lemma, vowelType);
            if (nominativeSingular != null) {
                frischEntries = dictionaryClient.getFrischLemma(nominativeSingular);
            }
        }

        LexemeGender gender = resolveGender(frischEntries, vowelType);
        String devanagari = transliterationService.iastToDevanagari(lemma);
        return new Stem(lemma, devanagari, gender, vowelType, vowelType.name(),
                firstGlossRu(frischEntries), firstGlossEn(frischEntries));
    }

    protected List<QuestItem> buildItemsForStem(Topic topic, Stem stem, List<Cell> cells,
                                                Map<String, Set<LexemeGender>> formGenders) {
        List<QuestItem> items = new ArrayList<>();
        for (Cell cell : cells) {
            buildForm(topic, stem, cell, items);
            buildFormChoice(topic, stem, cell, cells, items);
            buildCaseRecognition(topic, stem, cell, cells, formGenders, items);
        }
        buildMatch(topic, stem, cells, items);
        return items;
    }

    private void buildForm(Topic topic, Stem stem, Cell cell, List<QuestItem> items) {
        DeclensionFormPayload payload = formPayload(stem, cell);
        String progressTag = progressTag(cell, stem.gender());
        items.add(buildItem(topic, GrammarQuestItemTypes.DECLENSION_FORM,
                prompt(stem, cell, false), cell.form().iast(), List.of(),
                promptRu(stem, cell, false), null, null,
                payload, progressTag));
    }

    private void buildFormChoice(Topic topic, Stem stem, Cell cell, List<Cell> cells, List<QuestItem> items) {
        List<String> distractors = choiceDistractors(cells, cell);
        DeclensionFormPayload payload = formPayload(stem, cell);
        items.add(buildItem(topic, GrammarQuestItemTypes.DECLENSION_FORM_CHOICE,
                prompt(stem, cell, true), cell.form().iast(), distractors,
                promptRu(stem, cell, true), null, null,
                payload, progressTag(cell, stem.gender())));
    }

    private void buildCaseRecognition(Topic topic, Stem stem, Cell cell, List<Cell> cells,
                                       Map<String, Set<LexemeGender>> formGenders, List<QuestItem> items) {
        boolean genderRequired = formGenders.getOrDefault(cell.form().iast(), Set.of()).size() > 1;
        List<String> distractorCombinations = caseCombinations(cells, cell, stem.gender(), genderRequired);
        List<String> distractorCombinationsRu = caseCombinationsRu(cells, cell, stem.gender(), genderRequired);

        String correctLabel = label(cell.caseType(), cell.numberType(), stem.gender(), genderRequired);
        String correctLabelRu = labelRu(cell.caseType(), cell.numberType(), stem.gender(), genderRequired);
        String word = sanskritWord(cell.form().iast(), cell.form().devanagari());
        String prompt = "Identify the case" + (genderRequired ? ", number and gender" : " and number")
                + " of the word form " + word + ".";
        String promptRu = "Определите " + (genderRequired ? "падеж, число и род" : "падеж и число")
                + " словоформы " + word + ".";
        List<HighlightToken> highlights = List.of(
                new HighlightToken(cell.form().iast(), cell.form().iast()));

        CaseRecognitionPayload payload = new CaseRecognitionPayload(
                cell.form().iast(),
                cell.form().devanagari(),
                stem.lemmaIast(),
                stem.classCode(),
                cell.caseType().name(),
                cell.numberType().name(),
                stem.gender().name(),
                genderRequired,
                distractorCombinations,
                highlights);

        items.add(buildItem(topic, GrammarQuestItemTypes.CASE_RECOGNITION,
                prompt, correctLabel, distractorCombinations,
                promptRu, correctLabelRu, distractorCombinationsRu,
                payload, progressTag(cell, stem.gender())));
    }

    private void buildMatch(Topic topic, Stem stem, List<Cell> cells, List<QuestItem> items) {
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
                    stem.lemmaIast(), stem.classCode(), pairs,
                    List.of(new HighlightToken(stem.lemmaIast(), stem.lemmaIast())));

            String progressTag = leading.caseType() + "|" + leading.numberType() + "|" + LexemeGender.UNSPECIFIED;
            items.add(buildItem(topic, GrammarQuestItemTypes.DECLENSION_MATCH,
                    "Match each word form of " + sanskritWord(stem.lemmaIast(), stem.lemmaDevanagari())
                            + " to its case and number.",
                    null, List.of(),
                    "Сопоставьте каждую форму слова " + sanskritWord(stem.lemmaIast(), stem.lemmaDevanagari())
                            + " с её падежом и числом.",
                    null, null,
                    payload, progressTag));
        }
    }

    // ----------------------------------------------------------------------
    // Composition helpers
    // ----------------------------------------------------------------------

    private QuestItem buildItem(Topic topic, QuestItemType itemType,
                                String prompt, String correctAnswer, List<String> distractors,
                                String promptRu, String correctAnswerRu, List<String> distractorsRu,
                                Object payload, String progressTag) {
        QuestItem item = new QuestItem();
        item.setTopicId(topic.getId());
        item.setItemType(itemType.code());
        item.setAnswerMode(itemType.defaultAnswerMode());
        item.setPrompt(prompt);
        item.setPromptRu(promptRu);
        item.setCorrectAnswer(correctAnswer);
        item.setCorrectAnswerRu(correctAnswerRu);
        item.setDistractors(toJson(distractors));
        item.setDistractorsRu(distractorsRu == null ? null : toJson(distractorsRu));
        item.setPayload(toJson(payload));
        item.setProgressTag(progressTag);
        item.setQuestPattern(questPattern(itemType));
        item.setGeneratorSource(GENERATOR_SOURCE);
        return item;
    }

    /** Maps an implemented declension type to its quest_catalog_2.md pattern code. */
    private String questPattern(QuestItemType itemType) {
        if (itemType == GrammarQuestItemTypes.DECLENSION_FORM
                || itemType == GrammarQuestItemTypes.DECLENSION_FORM_CHOICE) {
            return QuestPatterns.NOM_FORM;
        }
        if (itemType == GrammarQuestItemTypes.CASE_RECOGNITION) {
            return QuestPatterns.NOM_ANAL;
        }
        if (itemType == GrammarQuestItemTypes.DECLENSION_MATCH) {
            return QuestPatterns.NOM_MATCH;
        }
        return null;
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

    private String prompt(Stem stem, Cell cell, boolean choice) {
        String verb = choice ? "Choose the correct" : "Enter the correct";
        String base = verb + " " + cell.caseType().name().toLowerCase() + " " + cell.numberType().name().toLowerCase()
                + " form of " + sanskritWord(stem.lemmaIast(), stem.lemmaDevanagari());
        return stem.glossEn() != null ? base + " — '" + stem.glossEn() + "'." : base + ".";
    }

    private String promptRu(Stem stem, Cell cell, boolean choice) {
        String verb = choice ? "Выберите" : "Введите";
        String base = verb + " правильную форму " + CASE_GENITIVE_RU.getOrDefault(
                cell.caseType().name(), cell.caseType().name().toLowerCase())
                + " падежа, " + NUMBER_GENITIVE_RU.getOrDefault(
                cell.numberType().name(), cell.numberType().name().toLowerCase())
                + " числа слова " + sanskritWord(stem.lemmaIast(), stem.lemmaDevanagari());
        return stem.glossRu() != null ? base + " — «" + stem.glossRu() + "»." : base + ".";
    }

    private static String sanskritWord(String iast, String devanagari) {
        if (devanagari == null || devanagari.isBlank()) {
            return iast;
        }
        return iast + " (" + devanagari + ")";
    }

    private DeclensionFormPayload formPayload(Stem stem, Cell cell) {
        return new DeclensionFormPayload(
                stem.lemmaIast(),
                stem.lemmaDevanagari(),
                stem.classCode(),
                stem.gender().name(),
                cell.caseType().name(),
                cell.numberType().name(),
                cell.form().iast(),
                cell.form().devanagari(),
                List.of(new HighlightToken(stem.lemmaIast(), stem.lemmaIast())));
    }

    private String progressTag(Cell cell, LexemeGender gender) {
        return cell.caseType().name() + "|" + cell.numberType().name() + "|" + gender.name();
    }

    private String label(CaseType caseType, NumberType numberType, LexemeGender gender, boolean withGender) {
        String base = titleCase(caseType.name()) + " " + titleCase(numberType.name());
        return withGender ? base + " " + titleCase(gender.name()) : base;
    }

    private String labelRu(CaseType caseType, NumberType numberType, LexemeGender gender, boolean withGender) {
        String base = CaseNumberGenderLocalizer.caseTypeRu(caseType.name()) + " "
                + CaseNumberGenderLocalizer.numberTypeRu(numberType.name());
        return withGender ? base + " " + CaseNumberGenderLocalizer.genderRu(gender.name()) : base;
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

    private List<String> caseCombinationsRu(List<Cell> cells, Cell correct,
                                             LexemeGender gender, boolean withGender) {
        List<String> combos = new ArrayList<>();
        for (Cell cell : cells) {
            if (cell.caseType() == correct.caseType() && cell.numberType() == correct.numberType()) {
                continue;
            }
            if (cell.form().iast().equals(correct.form().iast())) {
                continue; // ambiguous cell — useless as a distractor
            }
            combos.add(labelRu(cell.caseType(), cell.numberType(), gender, withGender));
            if (combos.size() == 3) {
                break;
            }
        }
        return combos;
    }

    private List<Cell> cells(VowelType vowelType, String lemmaIast) {
        return paradigmFormRepository.findByLemmaIastAndVowelType(lemmaIast, vowelType).stream()
                .map(f -> new Cell(
                        CaseType.valueOf(f.getCaseType().name()),
                        NumberType.valueOf(f.getNumberType().name()),
                        new Form(f.getFormIast(), f.getFormDevanagari())))
                .toList();
    }

    /**
     * Collects, across all stems of the topic, the set of genders that produce
     * each distinct word form. Used to decide {@code genderRequired}: a form is
     * grammatically ambiguous without gender iff more than one gender yields it.
     */
    private Map<String, Set<LexemeGender>> collectFormGenders(List<Stem> stems) {
        Map<String, Set<LexemeGender>> result = new LinkedHashMap<>();
        for (Stem stem : stems) {
            for (Cell cell : cells(stem.vowelType(), stem.lemmaIast())) {
                result.computeIfAbsent(cell.form().iast(), k -> new HashSet<>()).add(stem.gender());
            }
        }
        return result;
    }

    // ----------------------------------------------------------------------
    // Frisch (dictionary-service) integration
    // ----------------------------------------------------------------------

    private static boolean isPronoun(VowelType vowelType) {
        return vowelType != null && vowelType.name().startsWith("PRON_");
    }

    private String nominativeSingularFormIast(String lemma, VowelType vowelType) {
        return paradigmFormRepository.findByLemmaIastAndVowelType(lemma, vowelType).stream()
                .filter(f -> f.getCaseType() == CaseType.NOMINATIVE && f.getNumberType() == NumberType.SINGULAR)
                .map(ParadigmForm::getFormIast)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static LexemeGender resolveGender(List<FrischEntryDto> frischEntries, VowelType vowelType) {
        for (FrischEntryDto entry : frischEntries) {
            if (entry.genders() != null) {
                for (FrischGenderDto gender : entry.genders()) {
                    LexemeGender mapped = mapFrischGender(gender.gender());
                    if (mapped != null) {
                        return mapped;
                    }
                }
            }
        }
        // для местоимений род можно вывести из vowel_type
        if (isPronoun(vowelType)) {
            LexemeGender fromVowel = genderFromVowelType(vowelType);
            if (fromVowel != null) {
                return fromVowel;
            }
        }
        return LexemeGender.UNSPECIFIED;
    }

    private static LexemeGender mapFrischGender(String gender) {
        if (gender == null) {
            return null;
        }
        return switch (gender) {
            case "MASCULINE" -> LexemeGender.MASCULINE;
            case "FEMININE" -> LexemeGender.FEMININE;
            case "NEUTER" -> LexemeGender.NEUTER;
            default -> null;
        };
    }

    private static LexemeGender genderFromVowelType(VowelType vowelType) {
        String name = vowelType.name();
        if (name.endsWith("_MASC")) {
            return LexemeGender.MASCULINE;
        }
        if (name.endsWith("_NEUT")) {
            return LexemeGender.NEUTER;
        }
        if (name.endsWith("_FEM")) {
            return LexemeGender.FEMININE;
        }
        return null;
    }

    private static String firstGlossRu(List<FrischEntryDto> entries) {
        return entries.stream().map(FrischEntryDto::glossRu).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private static String firstGlossEn(List<FrischEntryDto> entries) {
        return entries.stream().map(FrischEntryDto::glossEn).filter(Objects::nonNull).findFirst().orElse(null);
    }
}
