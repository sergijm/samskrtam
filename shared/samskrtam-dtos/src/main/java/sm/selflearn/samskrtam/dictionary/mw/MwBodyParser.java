package sm.selflearn.samskrtam.dictionary.mw;

import sm.selflearn.samskrtam.morphology.PartOfSpeech;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разбирает поле {@code body} (псевдо-XML разметка словаря Monier-Williams,
 * см. mw-meta2.txt) на:
 *  1) {@link GrammarInfo} - структурированную грамматическую/справочную информацию;
 *  2) чистый удобочитаемый текст (без XML-тегов).
 *
 * Класс не хранит состояния между вызовами {@link #parse(String)} и потокобезопасен.
 */
public class MwBodyParser {

    // ---- {{Lbody=NN}} - тело статьи является ссылкой на другую подстатью ----
    private static final Pattern LBODY_REF = Pattern.compile("^\\{\\{Lbody=([0-9.]+)\\}\\}$");

    // ---- self-closing <info .../> и <listinfo .../> ----
    private static final Pattern INFO_TAG = Pattern.compile("<(info|listinfo)\\b([^>]*)/>");
    private static final Pattern ATTR = Pattern.compile("(\\w+)=\"([^\"]*)\"");

    // ---- <hom>N.</hom> ----
    private static final Pattern HOM_TAG = Pattern.compile("<hom>([^<]*)</hom>");

    // ---- <lex>G</lex> или <lex type="X">G</lex> ----
    private static final Pattern LEX_TAG = Pattern.compile("<lex(?:\\s+type=\"([^\"]*)\")?>([^<]*)</lex>");

    // ---- <s1 slp1="Z">Y</s1> (допускаем отсутствие slp1) ----
    private static final Pattern S1_TAG = Pattern.compile("<s1(?:\\s+slp1=\"([^\"]*)\")?>([^<]*)</s1>");

    // ---- <s>...</s>, внутри могут быть <srs/> и <shortlong/> ----
    private static final Pattern S_TAG = Pattern.compile("<s>((?:[^<]|<srs/>|<shortlong/>)*)</s>");
    private static final Pattern SRS_OR_SHORTLONG = Pattern.compile("<(srs|shortlong)/>");

    // ---- <ns>Y</ns>, <i>x</i> - просто разметка форматирования ----
    private static final Pattern NS_TAG = Pattern.compile("<ns>([^<]*)</ns>");
    private static final Pattern I_TAG = Pattern.compile("<i>([^<]*)</i>");

    // ---- когнаты: <lang...>X</lang> сразу за которым следует <etym>/<gk>/<ns> ----
    private static final Pattern LANG_PAIR = Pattern.compile(
            "<lang([^>]*)>([^<]*)</lang>\\s*<(etym|gk|ns)([^>]*)>([^<]*)</\\3>");
    private static final Pattern LANG_TAG = Pattern.compile("<lang([^>]*)>([^<]*)</lang>");
    private static final Pattern ETYM_TAG = Pattern.compile("<etym>([^<]*)</etym>");
    private static final Pattern GK_TAG = Pattern.compile("<gk>([^<]*)</gk>");

    // ---- <bot>X</bot>, <bio>X</bio>, <ls>X</ls> ----
    private static final Pattern BOT_TAG = Pattern.compile("<bot>([^<]*)</bot>");
    private static final Pattern BIO_TAG = Pattern.compile("<bio>([^<]*)</bio>");
    private static final Pattern LS_TAG = Pattern.compile("<ls>([^<]*)</ls>");

    // ---- <ab>X</ab>, <ab n="Y">X</ab>, <ab n="Y" slp1="Z">X</ab> ----
    private static final Pattern AB_TAG = Pattern.compile("<ab([^>]*)>([^<]*)</ab>");

    // ---- <pcol>X</pcol> ----
    private static final Pattern PCOL_TAG = Pattern.compile("<pcol>([^<]*)</pcol>");

    // ---- self-closing без текстового содержимого: <pb .../>, <div n="X"/> ----
    private static final Pattern PB_TAG = Pattern.compile("<pb\\b[^>]*/>");
    private static final Pattern DIV_TAG = Pattern.compile("<div\\s+n=\"([^\"]*)\"\\s*/>");

    // ---- любые оставшиеся теги (подчистка) ----
    private static final Pattern ANY_TAG = Pattern.compile("<[^>]+>");

    private static final Pattern MULTISPACE = Pattern.compile("[ \\t]+");
    private static final Pattern MULTINEWLINE = Pattern.compile("\\s*\\n\\s*");

    public static class Result {
        public final GrammarInfo grammar;
        public final String cleanText;

        public Result(GrammarInfo grammar, String cleanText) {
            this.grammar = grammar;
            this.cleanText = cleanText;
        }
    }

    public Result parse(String rawBody) {
        GrammarInfo g = new GrammarInfo();
        if (rawBody == null) {
            return new Result(g, "");
        }
        String text = rawBody.trim();

        // 1) Ссылка на тело другой подстатьи: {{Lbody=NN}}
        Matcher lbody = LBODY_REF.matcher(text);
        if (lbody.matches()) {
            g.setCrossReferenceToBody(lbody.group(1));
            return new Result(g, "");
        }

        // 2) <info .../> и <listinfo .../> - извлекаем и убираем из текста
        text = extractInfoTags(text, g);

        // 3) <hom>N.</hom> - берём первое вхождение как маркер омонима статьи,
        //    остальные (и это первое) вхождения просто раскрываем в тексте.
        Matcher hom = HOM_TAG.matcher(text);
        if (hom.find()) {
            g.setHomonymMarker(hom.group(1).trim());
        }
        text = replaceKeepingGroup(text, HOM_TAG, 1);

        // 4) <lex>G</lex> / <lex type="X">G</lex>
        text = extractLexTags(text, g);

        // 5) <s1 slp1="Z">Y</s1> - санскритские имена собственные (IAST)
        text = extractS1Tags(text, g);

        // 6) <s>...</s> - санскритские слова в slp1, очищаем от <srs/>/<shortlong/>
        text = extractSTags(text, g);

        // 7) <ns>Y</ns>, <i>x</i> - просто раскрываем содержимое
        text = replaceKeepingGroup(text, NS_TAG, 1);
        text = replaceKeepingGroup(text, I_TAG, 1);

        // 8) когнаты: <lang>..</lang> <etym|gk|ns>..</...>
        text = extractCognatePairs(text, g);
        // остаточные одиночные <lang>, <etym>, <gk> без пары
        text = extractLangLeftovers(text, g);
        text = replaceKeepingGroup(text, ETYM_TAG, 1);
        text = replaceKeepingGroup(text, GK_TAG, 1);

        // 9) <bot>, <bio>, <ls>
        text = extractSimpleListTag(text, BOT_TAG, g.getBotanicalNames());
        text = extractSimpleListTag(text, BIO_TAG, g.getBiologicalNames());
        text = extractSimpleListTag(text, LS_TAG, g.getLiterarySources());

        // 10) <ab ...>X</ab>
        text = extractAbTags(text, g);

        // 11) <pcol>X</pcol>
        text = extractSimpleListTag(text, PCOL_TAG, g.getPageColRefs());

        // 12) <pb/> убираем совсем; <div n="X"/> убираем, запоминаем маркер
        text = PB_TAG.matcher(text).replaceAll("");
        text = extractDivTags(text, g);

        // 13) подчистка любых оставшихся тегов (на случай нестандартных вариантов разметки)
        text = ANY_TAG.matcher(text).replaceAll("");

        // 14) финальная нормализация пробелов и разделителя ¦
        text = text.replace('\u00a6', '|'); // ¦ -> | (маркер конца заголовочной части)
        text = MULTINEWLINE.matcher(text).replaceAll("; ");
        text = MULTISPACE.matcher(text).replaceAll(" ");
        text = text.replace(" |", " |").trim();
        text = text.replaceAll("\\s+([,.;:])", "$1"); // убрать пробел перед пунктуацией
        text = text.trim();

        if (g.getVerbInfo() != null) {
            g.setPartOfSpeech(PartOfSpeech.VERB);
        }

        return new Result(g, text);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /** Заменяет все вхождения tag на содержимое его группы group (тег убирается, текст остаётся). */
    private String replaceKeepingGroup(String text, Pattern tag, int group) {
        Matcher m = tag.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String inner = m.group(group) == null ? "" : m.group(group);
            m.appendReplacement(sb, Matcher.quoteReplacement(inner));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private Map<String, String> parseAttrs(String attrString) {
        Map<String, String> map = new LinkedHashMap<>();
        if (attrString == null) {
            return map;
        }
        Matcher m = ATTR.matcher(attrString);
        while (m.find()) {
            map.put(m.group(1), m.group(2));
        }
        return map;
    }

    private String extractInfoTags(String text, GrammarInfo g) {
        Matcher m = INFO_TAG.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String tagName = m.group(1);
            Map<String, String> attrs = parseAttrs(m.group(2));
            for (Map.Entry<String, String> e : attrs.entrySet()) {
                applyInfoAttribute(g, tagName, e.getKey(), e.getValue());
            }
            m.appendReplacement(sb, "");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private void applyInfoAttribute(GrammarInfo g, String tagName, String key, String value) {
        if ("listinfo".equals(tagName)) {
            g.getOtherInfoAttributes().put("listinfo:" + key, value);
            return;
        }
        switch (key) {
            case "lex":
                g.getLexSummary().addAll(parseLexSummary(value));
                break;
            case "lexcat":
                for (String part : value.split(",")) {
                    String[] kv = part.split("=", 2);
                    if (kv.length == 2) {
                        g.getLexCategory().put(kv[0].trim(), kv[1].trim());
                    } else if (kv.length == 1 && !kv[0].isEmpty()) {
                        g.getLexCategory().put(kv[0].trim(), "");
                    }
                }
                break;
            case "verb":
                VerbInfo vi = g.getVerbInfo() != null ? g.getVerbInfo() : new VerbInfo();
                vi.setKind(value);
                g.setVerbInfo(vi);
                break;
            case "cp":
                VerbInfo vi2 = g.getVerbInfo() != null ? g.getVerbInfo() : new VerbInfo();
                vi2.setClassPada(value);
                g.setVerbInfo(vi2);
                break;
            case "parse":
                VerbInfo vi3 = g.getVerbInfo() != null ? g.getVerbInfo() : new VerbInfo();
                vi3.setParse(value);
                g.setVerbInfo(vi3);
                break;
            case "westergaard":
                for (String part : value.split(",")) {
                    g.getWestergaardRef().add(part.trim());
                }
                break;
            case "whitneyroots":
                for (String part : value.split(",")) {
                    g.getWhitneyRootsRef().add(part.trim());
                }
                break;
            case "n":
                if ("sup".equals(value)) {
                    g.setSupplement(true);
                } else if ("rev".equals(value)) {
                    g.setRevision(true);
                } else {
                    g.getOtherInfoAttributes().put("n", value);
                }
                break;
            default:
                g.getOtherInfoAttributes().put(key, value);
        }
    }

    /** "m:f#ikA:n" -> [m], [f,stem=ikA], [n] */
    private java.util.List<LexGender> parseLexSummary(String value) {
        java.util.List<LexGender> result = new java.util.ArrayList<>();
        for (String part : value.split(":")) {
            if (part.isEmpty()) {
                continue;
            }
            String[] genderAndStem = part.split("#", 2);
            String gender = genderAndStem[0];
            String stem = genderAndStem.length > 1 ? genderAndStem[1] : null;
            result.add(new LexGender(gender, stem, null));
        }
        return result;
    }

    private String extractLexTags(String text, GrammarInfo g) {
        Matcher m = LEX_TAG.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String type = m.group(1);
            String content = m.group(2);
            g.getLexTags().add(new LexGender(content.trim(), null, type));
            m.appendReplacement(sb, Matcher.quoteReplacement(content));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String extractS1Tags(String text, GrammarInfo g) {
        Matcher m = S1_TAG.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String slp1 = m.group(1);
            String iast = m.group(2);
            if (slp1 != null) {
                g.getSanskritProperNames().put(iast, slp1);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(iast));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String extractSTags(String text, GrammarInfo g) {
        Matcher m = S_TAG.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String raw = m.group(1);
            String clean = SRS_OR_SHORTLONG.matcher(raw).replaceAll("");
            g.getSanskritWords().add(clean);
            m.appendReplacement(sb, Matcher.quoteReplacement(clean));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String extractCognatePairs(String text, GrammarInfo g) {
        Matcher m = LANG_PAIR.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            Map<String, String> langAttrs = parseAttrs(m.group(1));
            String language = m.group(2);
            String word = m.group(5);
            String script = langAttrs.get("script");
            g.getCognates().add(new CognateWord(language, word, script));
            m.appendReplacement(sb, Matcher.quoteReplacement(language + " " + word));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String extractLangLeftovers(String text, GrammarInfo g) {
        Matcher m = LANG_TAG.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            Map<String, String> attrs = parseAttrs(m.group(1));
            String language = m.group(2);
            g.getCognates().add(new CognateWord(language, null, attrs.get("script")));
            m.appendReplacement(sb, Matcher.quoteReplacement(language));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String extractSimpleListTag(String text, Pattern tag, java.util.List<String> target) {
        Matcher m = tag.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String content = m.group(1);
            target.add(content);
            m.appendReplacement(sb, Matcher.quoteReplacement(content));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String extractAbTags(String text, GrammarInfo g) {
        Matcher m = AB_TAG.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            Map<String, String> attrs = parseAttrs(m.group(1));
            String content = m.group(2);
            g.getAbbreviations().add(content);
            String expansion = attrs.get("n");
            if (expansion != null) {
                g.getLocalAbbreviations().put(content, expansion);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(content));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String extractDivTags(String text, GrammarInfo g) {
        Matcher m = DIV_TAG.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            g.getDivMarkers().add(m.group(1));
            m.appendReplacement(sb, "\n");
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
