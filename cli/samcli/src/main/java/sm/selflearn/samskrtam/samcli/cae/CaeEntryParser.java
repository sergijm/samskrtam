package sm.selflearn.samskrtam.samcli.cae;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсер cae.txt (Cappeller's Sanskrit-English Dictionary, TEI-подобный
 * псевдо-XML формат). Файл не является валидным XML (незакрытые теги,
 * "^d"-надстрочники, амперсанды и т.п.) — поэтому применяется потоковый
 * regex/токен-разбор построчно/по блокам, а не DOM/SAX.
 *
 * <p>Алгоритм:
 *  1. Файл разбивается на записи по маркеру {@code <LEND>}.
 *  2. Из первой строки записи (начинается с {@code <L>}) регэкспом извлекаются
 *     id, page, k1, k2, h, e — это "заголовок" записи.
 *  3. Оставшийся текст записи — "тело" — прогоняется через набор
 *     regex-экстракторов, которые:
 *       - выдёргивают все {@code <ab>...</ab>} и {@code <lex>...</lex>} -> {@link AbbreviationMapper}
 *       - выдёргивают {@code <lang n="xx">...</lang>} -> CaeGrammarInfo.foreignRefs
 *       - выдёргивают {@code <div n="p">...</div>} блоки приставочных форм
 *         -> CaeGrammarInfo.compoundForms
 *       - всё остальное (после удаления тегов) -> clean_text
 *  4. Результат — {@link CaeEntry} (готов для вставки в целевую таблицу).</p>
 *
 * <p>Грамматические enum-типы, которыми оперирует парсер, — из общего
 * модуля shared:samskrtam-dtos (аналогично mw-импортёру), см. {@link CaeGrammarInfo}.</p>
 */
public final class CaeEntryParser {

    // Заголовок записи: <L>ID<pc>PAGE<k1>K1<k2>K2[<h>H][<e>E]
    private static final Pattern HEADER_PATTERN = Pattern.compile(
        "^<L>(?<id>\\d+)<pc>(?<page>\\d+)<k1>(?<k1>[^<]*)<k2>(?<k2>[^<]*)" +
        "(?:<h>(?<h>\\d+))?(?:<e>(?<e>[a-zA-Z]+))?"
    );

    private static final Pattern AB_TAG    = Pattern.compile("<ab>([^<]*)</ab>");
    private static final Pattern LEX_TAG   = Pattern.compile("<lex>([^<]*)</lex>");
    private static final Pattern LANG_TAG  = Pattern.compile("<lang n=\"([^\"]*)\">([^<]*)</lang>");
    // <div n="p">— {#prefix#} gloss...   — заканчивается перед следующим <div или концом записи
    private static final Pattern DIV_TAG   = Pattern.compile(
        "<div n=\"p\">\\s*[—-]?\\s*\\{#([^#}]*)#}\\s*([^<]*)"
    );
    // санскритская форма в фигурных скобках: {#word#}
    private static final Pattern SANSKRIT_FORM = Pattern.compile("\\{#([^#}]*)#}");
    // прочие теги, которые надо просто снять при построении clean_text
    private static final Pattern ANY_TAG = Pattern.compile("<[^>]+>");

    private CaeEntryParser() {
    }

    /** Читает файл целиком, возвращает список распарсенных записей. */
    public static List<CaeEntry> parseFile(String path) throws IOException {
        List<CaeEntry> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {

            StringBuilder block = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("<LEND>")) {
                    if (block.length() > 0) {
                        CaeEntry e = parseEntryBlock(block.toString());
                        if (e != null) result.add(e);
                        block.setLength(0);
                    }
                } else {
                    block.append(line).append('\n');
                }
            }
        }
        return result;
    }

    /** Парсит один блок (от <L> до предшествующего <LEND>, без самого <LEND>). */
    public static CaeEntry parseEntryBlock(String rawBlock) {
        // Заголовок — всегда первая строка блока
        int firstNewline = rawBlock.indexOf('\n');
        String headerLine = firstNewline >= 0 ? rawBlock.substring(0, firstNewline) : rawBlock;
        String body = firstNewline >= 0 ? rawBlock.substring(firstNewline + 1) : "";

        Matcher hm = HEADER_PATTERN.matcher(headerLine);
        if (!hm.find()) {
            // Не удалось распознать заголовок — пропускаем/логируем как есть
            return null;
        }

        CaeEntry entry = new CaeEntry();
        entry.setId(Long.parseLong(hm.group("id")));
        entry.setPage(Integer.parseInt(hm.group("page")));
        entry.setHeadwordPlain(hm.group("k1"));
        entry.setHeadwordAccented(hm.group("k2"));
        entry.setHomonymNum(hm.group("h") != null ? Integer.parseInt(hm.group("h")) : null);
        entry.setEntryVariant(hm.group("e"));
        entry.setRawText(rawBlock);

        // Тело статьи может продолжаться и в первой строке после заголовка,
        // т.к. заголовок regex захватывает только известные поля, "хвост"
        // строки (если есть) относится к телу.
        String headerTail = headerLine.substring(hm.end());
        String fullBody = headerTail + "\n" + body;

        CaeGrammarInfo grammar = new CaeGrammarInfo();

        // 1) <ab> и <lex> -> маппинг в enum-константы
        applyAllMatches(AB_TAG, fullBody, grammar);
        applyAllMatches(LEX_TAG, fullBody, grammar);

        // 2) <lang n="..">text</lang> -> foreignRefs
        Matcher langM = LANG_TAG.matcher(fullBody);
        while (langM.find()) {
            grammar.getForeignRefs().add(
                new CaeGrammarInfo.ForeignRef(langM.group(1), langM.group(2).trim())
            );
        }

        // 3) <div n="p">—{#prefix#} gloss  -> compoundForms
        Matcher divM = DIV_TAG.matcher(fullBody);
        while (divM.find()) {
            grammar.getCompoundForms().add(
                new CaeGrammarInfo.CompoundForm(divM.group(1).trim(), divM.group(2).trim())
            );
        }

        entry.setGrammar(grammar);
        entry.setCleanText(buildCleanText(fullBody));
        entry.setGloss(extractGloss(entry.getCleanText()));

        return entry;
    }

    private static void applyAllMatches(Pattern p, String body, CaeGrammarInfo grammar) {
        Matcher m = p.matcher(body);
        while (m.find()) {
            AbbreviationMapper.apply(m.group(1), grammar);
        }
    }

    /**
     * Строит "человекочитаемый" текст:
     *  - {#word#} -> word (снимаем маркеры санскритской формы, сохраняя сам текст)
     *  - все прочие теги (<ab>, <lex>, <lang>, <div>, <vlex>, служебные символы разметки) — убираются
     *  - схлопываются переносы строк/пробелы
     */
    private static String buildCleanText(String body) {
        String noHeaderNum = body.replaceAll("^\\s*\\d+\\s+", ""); // убрать "1 ", "2 " — номер омонима в теле
        String withPlainForms = SANSKRIT_FORM.matcher(noHeaderNum).replaceAll("$1");
        String noTags = ANY_TAG.matcher(withPlainForms).replaceAll("");
        String cleaned = noTags
            .replace("¦", "")   // разделитель формы/толкования
            .replaceAll("\\s+", " ")
            .trim();
        return cleaned;
    }

    private static String extractGloss(String cleanText) {
        int idx = -1;
        for (char stop : new char[]{';', '.'}) {
            int i = cleanText.indexOf(stop);
            if (i >= 0 && (idx == -1 || i < idx)) idx = i;
        }
        return idx > 0 ? cleanText.substring(0, idx).trim() : cleanText;
    }
}
