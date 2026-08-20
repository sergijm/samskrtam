package sm.selflearn.samskrtam.curriculum.questgen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.content.model.NumberType;
import sm.selflearn.samskrtam.content.model.Voice;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.paradigm.ConjugationForm;
import sm.selflearn.samskrtam.curriculum.paradigm.ConjugationFormRepository;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.quest.GrammarQuestItemTypes;
import sm.selflearn.samskrtam.quest.HighlightToken;
import sm.selflearn.samskrtam.quest.QuestItemType;
import sm.selflearn.samskrtam.quest.QuestPatterns;
import sm.selflearn.samskrtam.quest.conjugation.ConjugationAnalysisPayload;
import sm.selflearn.samskrtam.quest.conjugation.ConjugationBuildPayload;
import sm.selflearn.samskrtam.quest.conjugation.ConjugationFormPayload;
import sm.selflearn.samskrtam.quest.conjugation.ConjugationMatchPayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generates all CONJUGATION quest types (ver-form, ver-anal, ver-match,
 * ver-fix, ver-odd, ver-fill, ver-tran, ver-rev, ver-build) for the
 * VERBAL_MORPHOLOGY topics of the curriculum.
 *
 * <p>Reads {@code curriculum.conjugation_forms} keyed by topic_code.
 * The verb form is extracted as the last word of {@code sentence_iast}
 * (stripped of trailing punctuation). Every row carries a
 * {@code progress_tag} in the form {@code voice|person|numberType}.
 */
@Service
@RequiredArgsConstructor
public class ConjugationQuizItemGenerator extends QuizItemGenerator {

    public static final String GENERATOR_SOURCE = "CONJUGATION_BATCH";

    private static final Random RANDOM = new Random();

    private static final Map<String, String> TENSE_EN = Map.ofEntries(
            Map.entry("presence-indicativus", "present indicative"),
            Map.entry("imperfectum", "imperfect"),
            Map.entry("optativus", "optative"),
            Map.entry("imperativus", "imperative"),
            Map.entry("future", "future"),
            Map.entry("perfect", "perfect"),
            Map.entry("aorist", "aorist"));

    private static final Map<String, String> TENSE_RU = Map.ofEntries(
            Map.entry("presence-indicativus", "настоящее время"),
            Map.entry("imperfectum", "прошедшее незавершенное время"),
            Map.entry("optativus", "желательное наклонение"),
            Map.entry("imperativus", "повелительное наклонение"),
            Map.entry("future", "будущее время"),
            Map.entry("perfect", "перфект"),
            Map.entry("aorist", "аорист"));

    private static final Map<Integer, String> PERSON_EN = Map.of(
            1, "1st", 2, "2nd", 3, "3rd");

    private static final Map<Integer, String> PERSON_RU = Map.of(
            1, "первое", 2, "второе", 3, "третье");

    private static final Map<Integer, String> PERSON_LABEL_RU = Map.of(
            1, "1", 2, "2", 3, "3");

    private static final Map<String, String> VOICE_RU = Map.of(
            "PARASMAIPADA", "активный",
            "ATMANEPADA", "средний");

    private static final Map<String, String> VOICE_LABEL_RU = Map.of(
            "PARASMAIPADA", "P",
            "ATMANEPADA", "Ā");

    /** Known endings for Present P (class 1/4/6/10) person/number → last chars. */
    private static final Map<String, String> PRESENT_P_ENDINGS = Map.ofEntries(
            Map.entry("1|SINGULAR", "mi"),
            Map.entry("2|SINGULAR", "si"),
            Map.entry("3|SINGULAR", "ti"),
            Map.entry("1|DUAL", "vaḥ"),
            Map.entry("2|DUAL", "thaḥ"),
            Map.entry("3|DUAL", "taḥ"),
            Map.entry("1|PLURAL", "maḥ"),
            Map.entry("2|PLURAL", "tha"),
            Map.entry("3|PLURAL", "nti"));

    /** Known endings for Present Ā (class 1/4/6/10) person/number → last chars. */
    private static final Map<String, String> PRESENT_A_ENDINGS = Map.ofEntries(
            Map.entry("1|SINGULAR", "e"),
            Map.entry("2|SINGULAR", "se"),
            Map.entry("3|SINGULAR", "te"),
            Map.entry("1|DUAL", "vahe"),
            Map.entry("2|DUAL", "ethe"),
            Map.entry("3|DUAL", "ete"),
            Map.entry("1|PLURAL", "mahe"),
            Map.entry("2|PLURAL", "dhve"),
            Map.entry("3|PLURAL", "nte"));

    private static final int MAX_FORMS_PER_VERB = 9;
    /** Max pairs in one ver-match item. */
    private static final int MATCH_PAIRS_PER_ITEM = 3;

    private final ConjugationFormRepository conjugationFormRepository;
    private final QuestItemRepository questItemRepository;
    private final ObjectMapper objectMapper;

    private record VerbForm(
            String iast,
            String devanagari,
            String voice,
            int person,
            String numberType,
            String sentenceIast,
            String sentenceDevanagari,
            String translationRu
    ) {
    }

    private record LemmaForms(
            String lemmaIast,
            String lemmaDevanagari,
            String meaningRu,
            List<VerbForm> forms
    ) {
    }

    @Override
    public boolean isDomainSupported(TopicDomain domain) {
        return domain == TopicDomain.VERBAL_MORPHOLOGY;
    }

    @Override
    @Transactional
    public int generate(Topic topic) {
        String topicCode = topic.getCode();
        List<ConjugationForm> rows = conjugationFormRepository
                .findByTopicCodeOrderByLemmaIastAscVoiceAscPersonDescNumberTypeAsc(topicCode);
        if (rows.isEmpty()) {
            return 0;
        }

        Map<String, List<ConjugationForm>> byLemma = rows.stream()
                .collect(Collectors.groupingBy(ConjugationForm::getLemmaIast,
                        LinkedHashMap::new, Collectors.toList()));

        List<QuestItem> items = new ArrayList<>();
        for (Map.Entry<String, List<ConjugationForm>> entry : byLemma.entrySet()) {
            LemmaForms lemmaForms = toLemmaForms(entry.getKey(), entry.getValue());
            if (lemmaForms == null || lemmaForms.forms().isEmpty()) {
                continue;
            }
            generateForLemma(topic, topicCode, lemmaForms, items);
        }

        return persist(items);
    }

    private void generateForLemma(Topic topic, String topicCode,
                                  LemmaForms lemma, List<QuestItem> items) {
        List<VerbForm> forms = lemma.forms();

        for (VerbForm form : forms) {
            buildForm(topic, topicCode, lemma, form, items);
            buildFormChoice(topic, topicCode, lemma, form, forms, items);
            buildAnalysis(topic, topicCode, lemma, form, forms, items);
            buildCorrection(topic, topicCode, lemma, form, forms, items);
            buildFill(topic, topicCode, lemma, form, forms, items);
            buildBuild(topic, topicCode, lemma, form, forms, items);
            buildTranslate(topic, topicCode, lemma, form, forms, items);
            buildRecall(topic, topicCode, lemma, form, forms, items);
        }

        buildMatch(topic, topicCode, lemma, forms, items);
        buildOdd(topic, topicCode, lemma, forms, items);
    }

    // ------------------------------------------------------------------
    // ver-form (CONJUGATION_FORM — free text)
    // ------------------------------------------------------------------

    private void buildForm(Topic topic, String topicCode, LemmaForms lemma,
                           VerbForm form, List<QuestItem> items) {
        ConjugationFormPayload payload = formPayload(lemma, form, topicCode);
        items.add(buildItem(topic, GrammarQuestItemTypes.CONJUGATION_FORM,
                promptForm(lemma, form, topicCode, false),
                form.iast(), List.of(),
                promptFormRu(lemma, form, topicCode, false), null, null,
                payload, progressTag(form)));
    }

    // ------------------------------------------------------------------
    // ver-form (CONJUGATION_FORM_CHOICE — single choice)
    // ------------------------------------------------------------------

    private void buildFormChoice(Topic topic, String topicCode, LemmaForms lemma,
                                 VerbForm form, List<VerbForm> allForms, List<QuestItem> items) {
        List<String> distractors = choiceDistractors(allForms, form);
        ConjugationFormPayload payload = formPayload(lemma, form, topicCode);
        items.add(buildItem(topic, GrammarQuestItemTypes.CONJUGATION_FORM_CHOICE,
                promptForm(lemma, form, topicCode, true),
                form.iast(), distractors,
                promptFormRu(lemma, form, topicCode, true), null, null,
                payload, progressTag(form)));
    }

    // ------------------------------------------------------------------
    // ver-anal (CONJUGATION_ANALYSIS)
    // ------------------------------------------------------------------

    private void buildAnalysis(Topic topic, String topicCode, LemmaForms lemma,
                               VerbForm form, List<VerbForm> allForms, List<QuestItem> items) {
        String correctLabel = labelEn(form, topicCode);
        String correctLabelRu = labelRu(form, topicCode);
        List<String> distractorCombos = analysisDistractors(allForms, form, topicCode);
        List<String> distractorCombosRu = analysisDistractorsRu(allForms, form, topicCode);

        String word = sanskritWord(form.iast(), form.devanagari());
        String prompt = "Identify the person, number, voice and tense of the verb form " + word + ".";
        String promptRu = "Определите лицо, число, залог и время глагольной формы " + word + ".";

        ConjugationAnalysisPayload payload = new ConjugationAnalysisPayload(
                form.iast(), form.devanagari(),
                lemma.lemmaIast(), lemma.meaningRu(),
                form.person(), form.numberType(), form.voice(), topicCode,
                distractorCombos,
                List.of(new HighlightToken(form.iast(), form.iast())));

        items.add(buildItem(topic, GrammarQuestItemTypes.CONJUGATION_ANALYSIS,
                prompt, correctLabel, distractorCombos,
                promptRu, correctLabelRu, distractorCombosRu,
                payload, progressTag(form)));
    }

    // ------------------------------------------------------------------
    // ver-match (CONJUGATION_MATCH)
    // ------------------------------------------------------------------

    private void buildMatch(Topic topic, String topicCode, LemmaForms lemma,
                            List<VerbForm> forms, List<QuestItem> items) {
        for (int from = 0; from < forms.size(); from += MATCH_PAIRS_PER_ITEM) {
            List<VerbForm> block = forms.subList(from,
                    Math.min(from + MATCH_PAIRS_PER_ITEM, forms.size()));
            if (block.size() < 2) {
                continue;
            }
            List<ConjugationMatchPayload.ConjugationMatchPair> pairs = new ArrayList<>(block.size());
            for (VerbForm f : block) {
                pairs.add(new ConjugationMatchPayload.ConjugationMatchPair(
                        UUID.randomUUID().toString(),
                        f.iast(), f.devanagari(),
                        f.person(), f.numberType(), f.voice()));
            }
            ConjugationMatchPayload payload = new ConjugationMatchPayload(
                    lemma.lemmaIast(), lemma.meaningRu(), topicCode, pairs,
                    List.of(new HighlightToken(lemma.lemmaIast(), lemma.lemmaIast())));

            VerbForm leading = block.get(0);
            String progressTag = progressTag(leading);

            items.add(buildItem(topic, GrammarQuestItemTypes.CONJUGATION_MATCH,
                    "Match each verb form of " + sanskritWord(lemma.lemmaIast(), lemma.lemmaDevanagari())
                            + " to its person, number and voice.",
                    null, List.of(),
                    "Сопоставьте каждую форму глагола "
                            + sanskritWord(lemma.lemmaIast(), lemma.lemmaDevanagari())
                            + " с её лицом, числом и залогом.",
                    null, null,
                    payload, progressTag));
        }
    }

    // ------------------------------------------------------------------
    // ver-fix (CONJUGATION_CORRECTION)
    // ------------------------------------------------------------------

    private void buildCorrection(Topic topic, String topicCode, LemmaForms lemma,
                                 VerbForm correct, List<VerbForm> allForms, List<QuestItem> items) {
        List<VerbForm> wrongPool = new ArrayList<>(allForms);
        wrongPool.remove(correct);
        if (wrongPool.isEmpty()) {
            return;
        }
        VerbForm wrong = wrongPool.get(RANDOM.nextInt(wrongPool.size()));

        List<String> distractors = choiceDistractors(allForms, correct);
        String prompt = "The form " + wrong.iast() + " is incorrect for "
                + labelEn(correct, topicCode) + ". Choose the correct form.";
        String promptRu = "Форма " + wrong.iast() + " неверна для "
                + labelRu(correct, topicCode) + ". Выберите правильную форму.";

        ConjugationFormPayload payload = formPayload(lemma, correct, topicCode);
        items.add(buildItem(topic, GrammarQuestItemTypes.CONJUGATION_CORRECTION,
                prompt, correct.iast(), distractors,
                promptRu, null, null,
                payload, progressTag(correct)));
    }

    // ------------------------------------------------------------------
    // ver-odd (CONJUGATION_ODD)
    // ------------------------------------------------------------------

    private void buildOdd(Topic topic, String topicCode, LemmaForms lemma,
                          List<VerbForm> forms, List<QuestItem> items) {
        if (forms.size() < 4) {
            return;
        }
        int oddIdx = RANDOM.nextInt(forms.size());
        VerbForm odd = forms.get(oddIdx);

        List<VerbForm> pool = new ArrayList<>(forms);
        pool.remove(oddIdx);
        Collections.shuffle(pool, RANDOM);
        List<VerbForm> selected = pool.subList(0, Math.min(3, pool.size()));

        List<String> options = new ArrayList<>();
        options.add(odd.iast());
        for (VerbForm f : selected) {
            options.add(f.iast());
        }
        Collections.shuffle(options, RANDOM);

        String prompt = "Find the verb form that does not belong.";
        String promptRu = "Найдите глагольную форму, которая не подходит к остальным.";
        String correctAnswer = odd.iast();

        List<String> distractors = new ArrayList<>(options);
        distractors.remove(correctAnswer);
        // Move correct answer to correctAnswer field, rest go to distractors
        options.remove(correctAnswer);
        // But SINGLE_CHOICE stores correct separately
        List<String> finalDistractors = new ArrayList<>(options);

        ConjugationFormPayload payload = formPayload(lemma, odd, topicCode);
        items.add(buildItem(topic, GrammarQuestItemTypes.CONJUGATION_ODD,
                prompt, correctAnswer, finalDistractors,
                promptRu, null, null,
                payload, progressTag(odd)));
    }

    // ------------------------------------------------------------------
    // ver-fill (CONJUGATION_FIT)
    // ------------------------------------------------------------------

    private void buildFill(Topic topic, String topicCode, LemmaForms lemma,
                           VerbForm form, List<VerbForm> allForms, List<QuestItem> items) {
        String sentenceBlank = form.sentenceIast()
                .replace(form.iast(), "______");
        String sentenceBlankRu = form.sentenceDevanagari()
                .replace(form.devanagari(), "______");
        // If replace didn't work, use last-word blanking
        if (!sentenceBlank.contains("______")) {
            int lastSpace = form.sentenceIast().lastIndexOf(' ');
            sentenceBlank = form.sentenceIast().substring(0, lastSpace + 1) + "______";
        }

        List<String> distractors = choiceDistractors(allForms, form);

        String prompt = "Fill in the blank: " + sentenceBlank;
        String promptRu = "Вставьте пропуск: " + sentenceBlankRu;

        ConjugationFormPayload payload = formPayload(lemma, form, topicCode);
        items.add(buildItem(topic, GrammarQuestItemTypes.CONJUGATION_FIT,
                prompt, form.iast(), distractors,
                promptRu, null, null,
                payload, progressTag(form)));
    }

    // ------------------------------------------------------------------
    // ver-tran (CONJUGATION_TRANSLATE — RU → SA)
    // ------------------------------------------------------------------

    private void buildTranslate(Topic topic, String topicCode, LemmaForms lemma,
                                VerbForm form, List<VerbForm> allForms, List<QuestItem> items) {
        List<String> distractors = choiceDistractors(allForms, form);

        String prompt = "Translate the following into Sanskrit: " + form.translationRu();
        String promptRu = "Переведите на санскрит: " + form.translationRu();

        ConjugationFormPayload payload = formPayload(lemma, form, topicCode);
        items.add(buildItem(topic, GrammarQuestItemTypes.CONJUGATION_TRANSLATE,
                prompt, form.iast(), distractors,
                promptRu, null, null,
                payload, progressTag(form)));
    }

    // ------------------------------------------------------------------
    // ver-rev (CONJUGATION_RECALL — SA → RU meaning)
    // ------------------------------------------------------------------

    private void buildRecall(Topic topic, String topicCode, LemmaForms lemma,
                             VerbForm form, List<VerbForm> allForms, List<QuestItem> items) {
        Set<String> distractorMeanings = new LinkedHashSet<>();
        for (VerbForm f : allForms) {
            if (!f.translationRu().equals(form.translationRu())) {
                distractorMeanings.add(f.translationRu());
                if (distractorMeanings.size() >= 3) {
                    break;
                }
            }
        }
        if (distractorMeanings.size() < 1) {
            return; // not enough distractor meanings
        }

        String word = sanskritWord(form.iast(), form.devanagari());
        String prompt = "What is the meaning of the verb form " + word + "?";
        String promptRu = "Каково значение глагольной формы " + word + "?";

        List<String> distractors = new ArrayList<>(distractorMeanings);
        String correctAnswer = form.translationRu();

        ConjugationFormPayload payload = formPayload(lemma, form, topicCode);
        items.add(buildItem(topic, GrammarQuestItemTypes.CONJUGATION_RECALL,
                prompt, correctAnswer, distractors,
                promptRu, null, null,
                payload, progressTag(form)));
    }

    // ------------------------------------------------------------------
    // ver-build (CONJUGATION_BUILD — MATCHING)
    // ------------------------------------------------------------------

    private void buildBuild(Topic topic, String topicCode, LemmaForms lemma,
                            VerbForm form, List<VerbForm> allForms, List<QuestItem> items) {
        String ending = extractEnding(form, topicCode);
        if (ending == null) {
            return;
        }

        List<ConjugationMatchPayload.ConjugationMatchPair> pairs = new ArrayList<>(2);
        pairs.add(new ConjugationMatchPayload.ConjugationMatchPair(
                UUID.randomUUID().toString(),
                lemma.lemmaIast(), lemma.lemmaDevanagari(),
                form.person(), form.numberType(), form.voice()));
        pairs.add(new ConjugationMatchPayload.ConjugationMatchPair(
                UUID.randomUUID().toString(),
                ending, ending,
                form.person(), form.numberType(), form.voice()));

        ConjugationMatchPayload payload = new ConjugationMatchPayload(
                lemma.lemmaIast(), lemma.meaningRu(), topicCode, pairs,
                List.of(new HighlightToken(lemma.lemmaIast(), lemma.lemmaIast())));

        String prompt = "Assemble the " + labelEn(form, topicCode)
                + " form of " + sanskritWord(lemma.lemmaIast(), lemma.lemmaDevanagari())
                + ": match the root and ending to their slots.";
        String promptRu = "Соберите форму " + labelRu(form, topicCode)
                + " глагола " + sanskritWord(lemma.lemmaIast(), lemma.lemmaDevanagari())
                + ": сопоставьте корень и окончание.";

        items.add(buildItem(topic, GrammarQuestItemTypes.CONJUGATION_BUILD,
                prompt, null, List.of(),
                promptRu, null, null,
                payload, progressTag(form)));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private ConjugationFormPayload formPayload(LemmaForms lemma, VerbForm form, String topicCode) {
        return new ConjugationFormPayload(
                lemma.lemmaIast(), lemma.lemmaDevanagari(), lemma.meaningRu(),
                form.voice(), form.person(), form.numberType(), topicCode,
                form.iast(), form.devanagari(),
                List.of(new HighlightToken(lemma.lemmaIast(), lemma.lemmaIast())));
    }

    private String promptForm(LemmaForms lemma, VerbForm form, String topicCode, boolean choice) {
        String verb = choice ? "Choose the correct" : "Enter the correct";
        return verb + " " + PERSON_EN.getOrDefault(form.person(), String.valueOf(form.person()))
                + " person " + numberEn(form.numberType()) + " " + voiceEn(form.voice())
                + " form of " + sanskritWord(lemma.lemmaIast(), lemma.lemmaDevanagari())
                + " in the " + tenseEn(topicCode) + ".";
    }

    private String promptFormRu(LemmaForms lemma, VerbForm form, String topicCode, boolean choice) {
        String verb = choice ? "Выберите" : "Введите";
        return verb + " правильную форму " + PERSON_LABEL_RU.getOrDefault(
                form.person(), String.valueOf(form.person()))
                + " лица, " + numberRu(form.numberType()) + " числа, "
                + VOICE_LABEL_RU.getOrDefault(form.voice(), form.voice())
                + " залога глагола " + sanskritWord(lemma.lemmaIast(), lemma.lemmaDevanagari())
                + " в " + tenseRu(topicCode) + ".";
    }

    private String labelEn(VerbForm form, String topicCode) {
        return PERSON_EN.getOrDefault(form.person(), String.valueOf(form.person()))
                + " person " + numberEn(form.numberType()) + " "
                + voiceEn(form.voice()) + " " + tenseEn(topicCode);
    }

    private String labelRu(VerbForm form, String topicCode) {
        return PERSON_LABEL_RU.getOrDefault(form.person(), String.valueOf(form.person()))
                + " лицо " + numberRu(form.numberType()) + " число "
                + voiceRu(form.voice()) + " залог " + tenseRu(topicCode);
    }

    private List<String> choiceDistractors(List<VerbForm> allForms, VerbForm correct) {
        Set<String> others = new LinkedHashSet<>();
        for (VerbForm f : allForms) {
            if (!f.iast().equals(correct.iast())) {
                others.add(f.iast());
            }
        }
        List<String> candidates = new ArrayList<>(others);
        Collections.shuffle(candidates, RANDOM);
        return candidates.subList(0, Math.min(3, candidates.size()));
    }

    private List<String> analysisDistractors(List<VerbForm> allForms, VerbForm correct, String topicCode) {
        List<String> combos = new ArrayList<>();
        for (VerbForm f : allForms) {
            if (f.person() == correct.person()
                    && f.numberType().equals(correct.numberType())
                    && f.voice().equals(correct.voice())) {
                continue;
            }
            if (f.iast().equals(correct.iast())) {
                continue;
            }
            combos.add(labelEn(f, topicCode));
            if (combos.size() == 3) {
                break;
            }
        }
        return combos;
    }

    private List<String> analysisDistractorsRu(List<VerbForm> allForms, VerbForm correct, String topicCode) {
        List<String> combos = new ArrayList<>();
        for (VerbForm f : allForms) {
            if (f.person() == correct.person()
                    && f.numberType().equals(correct.numberType())
                    && f.voice().equals(correct.voice())) {
                continue;
            }
            if (f.iast().equals(correct.iast())) {
                continue;
            }
            combos.add(labelRu(f, topicCode));
            if (combos.size() == 3) {
                break;
            }
        }
        return combos;
    }

    private String extractEnding(VerbForm form, String topicCode) {
        // Use a simple heuristic: try known present endings
        String key = form.person() + "|" + form.numberType();
        Map<String, String> endings;
        if (form.voice().equals("PARASMAIPADA")) {
            endings = PRESENT_P_ENDINGS;
        } else {
            endings = PRESENT_A_ENDINGS;
        }
        String expected = endings.get(key);
        if (expected == null) {
            return null;
        }
        // The verb form should end with the expected ending
        if (form.iast().endsWith(expected)) {
            return expected;
        }
        // Try a fallback: extract last 2-5 chars as the ending
        for (int len = expected.length(); len >= 2; len--) {
            String candidate = form.iast().substring(Math.max(0, form.iast().length() - len));
            if (candidate.length() >= 2) {
                return candidate;
            }
        }
        return null;
    }

    private String progressTag(VerbForm form) {
        return form.voice() + "|" + form.person() + "|" + form.numberType();
    }

    // ------------------------------------------------------------------
    // Data extraction
    // ------------------------------------------------------------------

    private LemmaForms toLemmaForms(String lemmaIast, List<ConjugationForm> rows) {
        if (rows.isEmpty()) {
            return null;
        }
        ConjugationForm first = rows.get(0);
        String devanagari = first.getLemmaDevanagari();
        String meaning = first.getMeaningRu();
        List<VerbForm> forms = rows.stream()
                .map(this::toVerbForm)
                .filter(f -> f != null)
                .toList();
        return new LemmaForms(lemmaIast, devanagari, meaning, forms);
    }

    private VerbForm toVerbForm(ConjugationForm row) {
        String formIast = extractVerbForm(row.getSentenceIast());
        String formDevanagari = extractVerbForm(row.getSentenceDevanagari());
        if (formIast == null || formDevanagari == null) {
            return null;
        }
        return new VerbForm(
                formIast, formDevanagari,
                row.getVoice().name(),
                row.getPerson(),
                row.getNumberType().name(),
                row.getSentenceIast(),
                row.getSentenceDevanagari(),
                row.getTranslationRu());
    }

    /** Extracts the verb form as the last word of the sentence, stripped of punctuation. */
    private String extractVerbForm(String sentence) {
        if (sentence == null || sentence.isBlank()) {
            return null;
        }
        String trimmed = sentence.strip();
        int lastSpace = trimmed.lastIndexOf(' ');
        String lastWord = lastSpace >= 0 ? trimmed.substring(lastSpace + 1) : trimmed;
        // Strip trailing punctuation .!?।,
        lastWord = lastWord.replaceAll("[.!?।,]+$", "");
        return lastWord.isBlank() ? null : lastWord;
    }

    // ------------------------------------------------------------------
    // Tense / voice / number localization
    // ------------------------------------------------------------------

    private String tenseEn(String topicCode) {
        return TENSE_EN.getOrDefault(topicCode, topicCode);
    }

    private String tenseRu(String topicCode) {
        return TENSE_RU.getOrDefault(topicCode, topicCode);
    }

    private String voiceEn(String voice) {
        if ("PARASMAIPADA".equals(voice)) return "active";
        if ("ATMANEPADA".equals(voice)) return "middle";
        return voice;
    }

    private String voiceRu(String voice) {
        return VOICE_RU.getOrDefault(voice, voice);
    }

    private String numberEn(String numberType) {
        try {
            return NumberType.valueOf(numberType).getEnName();
        } catch (IllegalArgumentException e) {
            return numberType;
        }
    }

    private String numberRu(String numberType) {
        try {
            return NumberType.valueOf(numberType).getRuName();
        } catch (IllegalArgumentException e) {
            return numberType;
        }
    }

    private static String sanskritWord(String iast, String devanagari) {
        if (devanagari == null || devanagari.isBlank()) {
            return iast;
        }
        return iast + " (" + devanagari + ")";
    }

    // ------------------------------------------------------------------
    // Item construction & persistence
    // ------------------------------------------------------------------

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

    private String questPattern(QuestItemType itemType) {
        if (itemType == GrammarQuestItemTypes.CONJUGATION_FORM
                || itemType == GrammarQuestItemTypes.CONJUGATION_FORM_CHOICE) {
            return QuestPatterns.VERB_FORM;
        }
        if (itemType == GrammarQuestItemTypes.CONJUGATION_ANALYSIS) {
            return QuestPatterns.VERB_ANAL;
        }
        if (itemType == GrammarQuestItemTypes.CONJUGATION_MATCH) {
            return QuestPatterns.VERB_MATCH;
        }
        if (itemType == GrammarQuestItemTypes.CONJUGATION_CLASSIFY) {
            return QuestPatterns.VERB_CLASS;
        }
        if (itemType == GrammarQuestItemTypes.CONJUGATION_CORRECTION) {
            return QuestPatterns.VERB_FIX;
        }
        if (itemType == GrammarQuestItemTypes.CONJUGATION_FIT) {
            return QuestPatterns.VERB_FILL;
        }
        if (itemType == GrammarQuestItemTypes.CONJUGATION_TRANSLATE) {
            return QuestPatterns.VERB_TRAN;
        }
        if (itemType == GrammarQuestItemTypes.CONJUGATION_RECALL) {
            return QuestPatterns.VERB_REV;
        }
        if (itemType == GrammarQuestItemTypes.CONJUGATION_ODD) {
            return QuestPatterns.VERB_ODD;
        }
        if (itemType == GrammarQuestItemTypes.CONJUGATION_BUILD) {
            return QuestPatterns.VERB_BUILD;
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
}