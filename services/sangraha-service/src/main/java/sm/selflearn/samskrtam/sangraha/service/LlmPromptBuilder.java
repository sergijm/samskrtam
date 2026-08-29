package sm.selflearn.samskrtam.sangraha.service;

import sm.selflearn.samskrtam.common.transliteration.TransliterationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.model.Chapter;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;
import sm.selflearn.samskrtam.sangraha.repository.ChapterRepository;
import sm.selflearn.samskrtam.sangraha.repository.WorkRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;

import java.util.List;
import java.util.UUID;

/**
 * Строит system и user промпты для LLM.
 * Извлекает секцию ## system из markdown-файла промпта.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LlmPromptBuilder {

    private final ObjectMapper objectMapper;
    private final PromptLoader promptLoader;
    private final TransliterationService transliterationService;
    private final ChapterRepository chapterRepository;
    private final WorkRepository workRepository;
    private final VerseWordRepository verseWordRepository;

    /** Маркер-плейсхолдер режима батча в промпте шага 1. */
    private static final String BATCH_CONTEXT_MODE_PLACEHOLDER = "{{BATCH_CONTEXT_MODE}}";

    /** Полный текст варианта SAME_WORK (подставляется backend-ом вместо плейсхолдера). */
    private static final String BATCH_MODE_SAME_WORK = """
            All verses in this batch are consecutive or otherwise closely related verses from a \
            single work (see the workTitle / workId given once in the user message, and the \
            optional precedingContext field on individual verses, if present). This gives you \
            legitimate additional context, which you should use as follows:
            - Terminology, proper names (deities, places, epithets), and recurring formulaic \
              phrases should be translated CONSISTENTLY across all verses in this batch — if a \
              word such as a deity's name or a technical ritual term appears in more than one \
              verse, use the same Russian/English rendering each time rather than varying it \
              for stylistic reasons.
            - If a verse is elliptical or its subject is only recoverable from a preceding verse \
              in the same batch (e.g. a pronoun whose referent was named in the previous verse, \
              or a verb elided under coordination with a neighboring verse), you MAY use that \
              surrounding context to resolve the ambiguity in translationRu/translationEn and in \
              glossRu/glossEn — but say so via analysisConfidence/ambiguityNotes on the \
              affected word if the resolution depends on that context rather than being \
              self-evident from the verse alone.
            - Despite this shared context, sandhiSplits and the internal segmentation of each \
              verse's own words[] must still be derived strictly from that verse's own textIast \
              — never import a word or a sandhi split from a neighboring verse into this verse's \
              arrays.
            - Do NOT let a genuinely uncertain reading in one verse "borrow" false confidence \
              from a superficially similar verse elsewhere in the batch — same-work context \
              helps resolve real ellipsis/anaphora, it is not license to assume verses are more \
              alike than they are.""";

    /** Полный текст варианта MIXED_WORKS (подставляется backend-ом вместо плейсхолдера). */
    private static final String BATCH_MODE_MIXED_WORKS = """
            The verses in this batch come from different, unrelated works, or their relation \
            (if any) is unknown/not asserted by the backend. Treat every verse as a fully \
            self-contained unit with NO shared context:
            - Do not assume any two verses share a deity, character, ritual setting, metre, or \
              authorial voice, even if surface vocabulary looks similar.
            - Do not carry a translation choice, terminology rendering, or proper-name gloss \
              from one verse over to another merely because the same word happens to recur — \
              re-derive the correct sense independently for each verse from its own textIast \
              alone.
            - If a verse is elliptical or a pronoun's referent is not recoverable from the verse \
              itself, do NOT resolve it using another verse in the batch — translate what is \
              actually there, and if the referent is genuinely unclear from the verse alone, \
              reflect that with analysisConfidence: MEDIUM/LOW and a note in ambiguityNotes \
              rather than importing an answer from elsewhere in the batch.
            - This is a stricter, more conservative mode than SAME_WORK: when in doubt about \
              whether two verses are related, behave as if they are not.""";

    /**
     * Извлекает содержимое секции ## system из markdown-файла промпта verse-analysis.
     */
    public String extractSystemPrompt() {
        return extractFencedSection(promptLoader.getVerseAnalysisPrompt());
    }

    /**
     * Извлекает system-промпт шага 1 (translation + external sandhi + lexical/morphology)
     * из prompts/2/step1-translation-external-sandhi.md и подставляет корректный вариант
     * BATCH_CONTEXT_MODE (SAME_WORK / MIXED_WORKS) вместо плейсхолдера. Режим задаётся
     * явно вызывающим (SAME_WORK — «Анализировать все» из главы, MIXED_WORKS — страница
     * стихов), а не выводится из содержимого батча.
     */
    public String buildStep1SystemPrompt(List<Verse> verses, boolean sameWork) {
        String section = extractFencedSection(promptLoader.getVerseAnalysisStep1Prompt());
        String variant = sameWork ? BATCH_MODE_SAME_WORK : BATCH_MODE_MIXED_WORKS;
        return section.replace(BATCH_CONTEXT_MODE_PLACEHOLDER, variant);
    }

    /**
     * Извлекает текст первого fenced-блока (``` ... ```) внутри секции «## system».
     * Учитывает вложенные code-fence (примеры внутри инструкций) — берётся
     * ПОСЛЕДНЯЯ закрывающая ограда до следующего «## »-заголовка, а не первая.
     */
    private String extractFencedSection(String fullPrompt) {
        if (fullPrompt == null) {
            return "";
        }
        int systemStart = findHeading(fullPrompt, "## system");
        if (systemStart < 0) {
            return fullPrompt.trim();
        }
        int nextSection = fullPrompt.indexOf("\n## ", systemStart + 1);
        String section = nextSection >= 0
                ? fullPrompt.substring(systemStart, nextSection)
                : fullPrompt.substring(systemStart);

        int codeStart = section.indexOf("```\n");
        if (codeStart < 0) {
            return section.trim();
        }
        int contentStart = codeStart + 4; // после "```\n"
        int codeEnd = section.lastIndexOf("\n```");
        if (codeEnd < contentStart) {
            return section.substring(contentStart).trim();
        }
        return section.substring(contentStart, codeEnd).trim();
    }

    /** Ищет заголовок «## system» именно в начале строки (не в упоминаниях внутри текста). */
    private int findHeading(String fullPrompt, String heading) {
        int from = 0;
        while (true) {
            int idx = fullPrompt.indexOf(heading, from);
            if (idx < 0) {
                return -1;
            }
            boolean lineStart = idx == 0 || fullPrompt.charAt(idx - 1) == '\n';
            boolean lineEnd = idx + heading.length() >= fullPrompt.length()
                    || fullPrompt.charAt(idx + heading.length()) == '\n'
                    || fullPrompt.charAt(idx + heading.length()) == '\r';
            if (lineStart && lineEnd) {
                return idx;
            }
            from = idx + 1;
        }
    }

        /**
     * Строит user-промпт с текстом стиха и контекстом правил сандхи (batch-формат).
     * В LLM всегда передаётся только IAST: если исходный текст задан в деванагари,
     * он детектируется и конвертируется в IAST до отправки (деванагари не передаётся).
     */
    public String buildUserPrompt(Verse verse) {
        var sb = new StringBuilder();
        String iast = resolveIastInput(verse);
        sb.append("verseIndex: 0\n");
        if (iast != null) {
            sb.append("textIast: ").append(iast).append("\n");
        } else {
            sb.append("textIast: null\n");
        }

        appendSandhiRules(sb);

        return sb.toString();
    }

    /**
     * Строит batch user-промпт для нескольких стихов.
     * Для каждого стиха выводит блок verseIndex/textIast (только IAST).
     * verseIndex = позиция в списке. Деванагари не передаётся — конвертируется в IAST.
     */
    public String buildBatchUserPrompt(List<Verse> verses) {
        var sb = new StringBuilder("Analyze the following Sanskrit verses:\n\n");
        for (int i = 0; i < verses.size(); i++) {
            Verse verse = verses.get(i);
            sb.append("verseIndex: ").append(i).append("\n");
            String iast = resolveIastInput(verse);
            if (iast != null) {
                sb.append("textIast: ").append(iast).append("\n");
            } else {
                sb.append("textIast: null\n");
            }
            sb.append("\n");
        }

        appendSandhiRules(sb);

        return sb.toString();
    }

    /**
     * Возвращает текст стиха в IAST для передачи в LLM.
     * Источник (в порядке приоритета): rawText → textIast → textDevanagari.
     * Если источник обнаружен как деванагари — конвертируется в IAST.
     * Возвращает null, если ни один источник не задан.
     */
    private String resolveIastInput(Verse verse) {
        String source = verse.getRawText();
        if (source == null || source.isBlank()) {
            source = verse.getTextIast();
        }
        if (source == null || source.isBlank()) {
            source = verse.getTextDevanagari();
        }
        if (source == null || source.isBlank()) {
            return null;
        }
        if ("devanagari".equals(transliterationService.detectScript(source))) {
            return transliterationService.devanagariToIast(source);
        }
        return source;
    }

    private void appendSandhiRules(StringBuilder sb) {
        JsonNode sandhiRulesNode = promptLoader.getEmenauSandhiRules();
        if (sandhiRulesNode != null) {
            try {
                String sandhiRules = objectMapper.writeValueAsString(sandhiRulesNode);
                if (!sandhiRules.isEmpty()) {
                    sb.append("\n---\n");
                    sb.append("External sandhi rules (rules 41–71) for sandhi split reference:\n");
                    sb.append(sandhiRules);
                    sb.append("\n\nIMPORTANT: Only use these external rules (41–71) for sandhi split analysis. ");
                    sb.append("Internal rules (1–40) explain word-internal form changes and must NOT be cited in sandhi splits.");
                }
            } catch (Exception e) {
                log.warn("Failed to serialize sandhi rules", e);
            }
        }
    }

    /**
     * Строит batch user-промпт шага 1 для нескольких стихов.
     * Для SAME_WORK-батча (все стихи одного произведения) добавляет заголовок
     * workTitle; для MIXED_WORKS — не добавляет. Всегда передаёт только IAST.
     * К промпту прикладываются внешние правила сандхи (41–71) из emenau-sandhi-rules-external.json.
     *
     * @param sameWork true для SAME_WORK-батча (заголовок workTitle добавляется)
     */
    public String buildStep1BatchUserPrompt(List<Verse> verses, boolean sameWork) {
        var sb = new StringBuilder("Analyze the following Sanskrit verses:\n\n");
        if (sameWork) {
            String workTitle = resolveWorkTitle(verses);
            if (workTitle != null) {
                sb.append("workTitle: ").append(workTitle).append("\n\n");
            }
        }
        for (int i = 0; i < verses.size(); i++) {
            Verse verse = verses.get(i);
            sb.append("verseIndex: ").append(i).append("\n");
            String iast = resolveIastInput(verse);
            if (iast != null) {
                sb.append("textIast: ").append(iast).append("\n");
            } else {
                sb.append("textIast: null\n");
            }
            sb.append("\n");
        }

        appendExternalSandhiRules(sb);

        return sb.toString();
    }

    private void appendExternalSandhiRules(StringBuilder sb) {
        JsonNode sandhiRulesNode = promptLoader.getEmenauSandhiRulesExternal();
        if (sandhiRulesNode != null) {
            try {
                String sandhiRules = objectMapper.writeValueAsString(sandhiRulesNode);
                if (!sandhiRules.isEmpty()) {
                    sb.append("\n---\n");
                    sb.append("External sandhi rules (rules 41–71) for sandhi split reference:\n");
                    sb.append(sandhiRules);
                    sb.append("\n\nIMPORTANT: Only use these external rules (41–71) for sandhi split analysis. ");
                    sb.append("Internal rules (1–40) explain word-internal form changes and must NOT be cited in sandhi splits.");
                }
            } catch (Exception e) {
                log.warn("Failed to serialize external sandhi rules", e);
            }
        }
    }

    private UUID resolveWorkId(Verse verse) {
        if (verse.getChapterId() == null) {
            return null;
        }
        Chapter chapter = chapterRepository.findByIdAndDeletedAtIsNull(verse.getChapterId()).orElse(null);
        if (chapter == null || chapter.getWorkId() == null) {
            return null;
        }
        return chapter.getWorkId();
    }

    private String resolveWorkTitle(List<Verse> verses) {
        UUID workId = resolveWorkId(verses.get(0));
        if (workId == null) {
            return null;
        }
        Work work = workRepository.findById(workId).orElse(null);
        if (work == null) {
            return null;
        }
        if (work.getTitleSaIast() != null && !work.getTitleSaIast().isBlank()) {
            return work.getTitleSaIast();
        }
        if (work.getTitleRu() != null && !work.getTitleRu().isBlank()) {
            return work.getTitleRu();
        }
        return work.getTitleEn();
    }

    /**
     * Извлекает system-промпт шага 2 (внутренние сандхи / словообразование,
     * {@code prompts/2/step2-internal-sandhi.md}). Шаг 2 имеет статический system-блок
     * без плейсхолдера BATCH_CONTEXT_MODE (в отличие от шага 1). Файл шага 2 использует
     * незакрытые заголовки {@code system} / {@code user (template ...)} (без {@code ## } и
     * code-fence), поэтому извлечение — от строки {@code system} до строки {@code user}.
     */
    public String buildStep2SystemPrompt() {
        return extractHeadingSection(promptLoader.getVerseAnalysisStep2Prompt(), "system", "user");
    }

    /**
     * Извлекает подсекцию файла промпта между строкой-заголовком startHeading и
     * следующей строкой, начинающейся с endHeading (без {@code ## } и code-fence).
     */
    private String extractHeadingSection(String fullPrompt, String startHeading, String endHeading) {
        if (fullPrompt == null) {
            return "";
        }
        String[] lines = fullPrompt.split("\n", -1);
        int start = -1;
        for (int i = 0; i < lines.length; i++) {
            String t = lines[i].trim();
            if (t.equalsIgnoreCase(startHeading) || t.toLowerCase().startsWith(startHeading.toLowerCase() + " ")) {
                start = i;
                break;
            }
        }
        if (start < 0) {
            return fullPrompt.trim();
        }
        int end = lines.length;
        String endLower = endHeading.toLowerCase();
        for (int i = start + 1; i < lines.length; i++) {
            if (lines[i].trim().toLowerCase().startsWith(endLower)) {
                end = i;
                break;
            }
        }
        var sb = new StringBuilder();
        for (int i = start + 1; i < end; i++) {
            sb.append(lines[i]).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Строит batch user-промпт шага 2 из уже сохранённых слов стиха (результат шага 1).
     * Для каждого стиха батча (verseIndex = позиция в списке) выводит блоки слов
     * (verseIndex/position/surfaceIast/lemmaIast/root/derivationalBase/derivationalSuffix),
     * затем прикладывает внутренние правила сандхи (1–40) из
     * emenau-sandhi-rules-internal.json. Модель возвращает те же verseIndex/position
     * (submit_word_formations), по которым backend джойнит результаты к VerseWord.
     */
    public String buildStep2BatchUserPrompt(List<Verse> verses) {
        var sb = new StringBuilder("Internal sandhi analysis. The following words were already "
                + "segmented and lemmatized by STEP 1; analyze each word's internal formation:\n\n");

        for (int i = 0; i < verses.size(); i++) {
            Verse verse = verses.get(i);
            List<VerseWord> words =
                    verseWordRepository.findAllByVerse_IdOrderByPositionAsc(verse.getId());
            for (VerseWord w : words) {
                sb.append("verseIndex: ").append(i).append("\n");
                sb.append("position: ").append(w.getPosition()).append("\n");
                sb.append("surfaceIast: ").append(nullable(w.getSurfaceIast())).append("\n");
                sb.append("lemmaIast: ").append(nullable(w.getLemmaIast())).append("\n");
                sb.append("root: ").append(nullable(w.getRoot())).append("\n");
                if (w.getDerivation() != null) {
                    sb.append("derivationalBase: ")
                            .append(nullable(w.getDerivation().getDerivationalBase())).append("\n");
                    sb.append("derivationalSuffix: ")
                            .append(nullable(w.getDerivation().getDerivationalSuffix())).append("\n");
                } else {
                    sb.append("derivationalBase: null\n");
                    sb.append("derivationalSuffix: null\n");
                }
                sb.append("\n");
            }
        }

        appendInternalSandhiRules(sb);

        return sb.toString();
    }

    private void appendInternalSandhiRules(StringBuilder sb) {
        JsonNode sandhiRulesNode = promptLoader.getEmenauSandhiRulesInternal();
        if (sandhiRulesNode != null) {
            try {
                String sandhiRules = objectMapper.writeValueAsString(sandhiRulesNode);
                if (!sandhiRules.isEmpty()) {
                    sb.append("\n---\n");
                    sb.append("Internal sandhi rules (rules 1–40) for word formation reference:\n");
                    sb.append(sandhiRules);
                    sb.append("\n\nIMPORTANT: Only use these internal rules (1–40) for "
                            + "formationRuleNumbers. External rules (41–71) from STEP 1 must NOT be cited here.");
                }
            } catch (Exception e) {
                log.warn("Failed to serialize internal sandhi rules", e);
            }
        }
    }

    private static String nullable(String value) {
        return (value == null || value.isBlank()) ? "null" : value;
    }

}