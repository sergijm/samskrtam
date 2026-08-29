package sm.selflearn.samskrtam.apte;

import java.util.*;
import java.util.regex.*;

/**
 * Formats a single Apte dictionary entry's rawMarkup field into the
 * DDSA-style HTML block shown in the reference example.
 *
 * Usage:
 *   String html = ApteHtmlFormatter.format(rawMarkup);
 *
 * Notes / simplifications vs. the "real" DDSA renderer:
 *  - <ls>WORK REF</ls> citations are linked using a small built-in
 *    abbreviation -> (title, url-template) table. Unknown abbreviations
 *    are rendered as plain text (no link) instead of failing.
 *  - SLP1 -> IAST transliteration covers the full SLP1 consonant/vowel
 *    inventory used in this dictionary.
 */
public class ApteHtmlFormatter {

    // ------------------------------------------------------------------
    // 1. SLP1 -> IAST transliteration
    // ------------------------------------------------------------------
    private static final Map<Character, String> SLP1_TO_IAST = new HashMap<>();
    static {
        // vowels
        put('a', "a"); put('A', "ā"); put('i', "i"); put('I', "ī");
        put('u', "u"); put('U', "ū"); put('f', "ṛ"); put('F', "ṝ");
        put('x', "ḷ"); put('X', "ḹ"); put('e', "e"); put('E', "ai");
        put('o', "o"); put('O', "au");
        // anusvara / visarga / candrabindu
        put('M', "ṃ"); put('H', "ḥ"); put('~', "m̐");
        // velars
        put('k', "k"); put('K', "kh"); put('g', "g"); put('G', "gh"); put('N', "ṅ");
        // palatals
        put('c', "c"); put('C', "ch"); put('j', "j"); put('J', "jh"); put('Y', "ñ");
        // retroflex
        put('w', "ṭ"); put('W', "ṭh"); put('q', "ḍ"); put('Q', "ḍh"); put('R', "ṇ");
        // dentals
        put('t', "t"); put('T', "th"); put('d', "d"); put('D', "dh"); put('n', "n");
        // labials
        put('p', "p"); put('P', "ph"); put('b', "b"); put('B', "bh"); put('m', "m");
        // semivowels / sibilants / h
        put('y', "y"); put('r', "r"); put('l', "l"); put('v', "v");
        put('S', "ś"); put('z', "ṣ"); put('s', "s"); put('h', "h");
        // avagraha
        put('\'', "’"); // ’
    }
    private static void put(char c, String s) { SLP1_TO_IAST.put(c, s); }

    /** Transliterate a chunk of SLP1 Sanskrit text to IAST, leaving
     *  non-SLP1 characters (spaces, punctuation, digits, Latin gloss
     *  words that slipped in) untouched. */
    public static String slp1ToIast(String slp1) {
        if (slp1 == null) return null;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < slp1.length(); i++) {
            char c = slp1.charAt(i);
            String mapped = SLP1_TO_IAST.get(c);
            out.append(mapped != null ? mapped : c);
        }
        return out.toString();
    }

    // ------------------------------------------------------------------
    // 2. Citation abbreviation -> (title, url pattern) table
    //    url pattern uses {A},{B},{C}... for the numeric reference groups
    //    found after the abbreviation, e.g. "12. 158. 35" -> A=12,B=158,C=35
    // ------------------------------------------------------------------
    private static final class WorkInfo {
        final String title;
        final String urlTemplate; // may be null -> no link
        WorkInfo(String title, String urlTemplate) { this.title = title; this.urlTemplate = urlTemplate; }
    }
    private static final Map<String, WorkInfo> WORKS = new HashMap<>();
    static {
        WORKS.put("Y.", new WorkInfo("Yājñavalkya Smṛti, (Nirṇaya Sāgara, 1926).",
                "https://sanskrit-lexicon-scans.github.io/yajnavalkya/app1?{A},{B}"));
        WORKS.put("Ms.", new WorkInfo("Manusmṛti, (J. M. Gurjar, Bombay, 1894).",
                "https://sanskrit-lexicon-scans.github.io/manu/index.html?{A},{B}"));
        WORKS.put("Mb.", new WorkInfo("Mahābhārata, (चित्रशाळा, पुणें, १९२९-३३).",
                "https://sanskrit-lexicon-scans.github.io/mbhbomb/app1?{A},{B},{C}"));
        WORKS.put("Ku.", new WorkInfo("Kumārasambhava.", null));
        WORKS.put("Bhāg.", new WorkInfo("Bhāgavata Purāṇa.", null));
        WORKS.put("Mv.", new WorkInfo("Mahāvīracarita.", null));
        WORKS.put("P.", new WorkInfo("Pāṇini, Aṣṭādhyāyī.", null));
        WORKS.put("Rv.", new WorkInfo("Ṛgveda.", null));
        WORKS.put("Rām.", new WorkInfo("Rāmāyaṇa.", null));
        WORKS.put("U.", new WorkInfo("Uttararāmacarita.", null));
        WORKS.put("Ki.", new WorkInfo("Kirātārjunīya.", null));
        WORKS.put("Śik.", new WorkInfo("Śikṣā.", null));
        // extend as needed...
    }

    // ------------------------------------------------------------------
    // 3. Header field extraction: <L>lnum<pc>page-col<k1>..<k2>..<e>N
    // ------------------------------------------------------------------
    private static final Pattern HEADER = Pattern.compile(
        "<L>([^<]*)<pc>([^<]*)<k1>([^<]*)<k2>([^<]*)(?:<hom>([^<]*))?<e>(\\d*)",
        Pattern.DOTALL);

    private static final class Header {
        String lnum, pc, k1, k2, hom;
    }

    private static Header parseHeader(String rawMarkup) {
        Matcher m = HEADER.matcher(rawMarkup);
        Header h = new Header();
        if (m.find()) {
            h.lnum = m.group(1);
            h.pc = m.group(2);
            h.k1 = m.group(3);
            h.k2 = m.group(4);
            h.hom = m.group(5) != null ? m.group(5) : m.group(6);
        }
        return h;
    }

    // ------------------------------------------------------------------
    // 4. Inline markup renderer (operates on the *body*, after the header
    //    and before <LEND>)
    // ------------------------------------------------------------------

    // Recognizes: {#..#} devanagari/slp1, {@..@} bold, {%..%} italic,
    // <ls>..</ls> citation, <ab>..</ab> abbreviation, ∙²N / ∙¹N sense marker.
    // Processed with a small hand-rolled scanner rather than one giant
    // regex, since these constructs can nest (e.g. {@{#-vaH#}@}).

    public static String format(String rawMarkup) {
        if (rawMarkup == null) return "";

        Header h = parseHeader(rawMarkup);

        // isolate the body: after the header match, before <LEND>
        String body = rawMarkup;
        Matcher hm = HEADER.matcher(rawMarkup);
        if (hm.find()) body = rawMarkup.substring(hm.end());
        body = body.replaceAll("<LEND>\\s*$", "");
        body = body.trim();

        String bodyHtml = renderBody(body);

        String headwordIast = slp1ToIast(h.k1 != null ? h.k1 : "");
        String pcLink = h.pc != null
            ? "<a href=\"//www.sanskrit-lexicon.uni-koeln.de/scans/csl-apidev/servepdf.php?dict=AP&amp;page="
              + h.pc + "\" target=\"_AP\">" + h.pc + "</a>"
            : "";

        StringBuilder html = new StringBuilder();
        html.append("<div id=\"CologneBasic\">\n");
        html.append("<h1>&nbsp;<span class=\"sdata\">").append(headwordIast).append("</span></h1>\n");
        html.append("<table class=\"display\">\n<tbody><tr><td class=\"display\" valign=\"top\"> ");
        html.append("<span style=\"font-weight:bold\"><span class=\"sdata\">").append(headwordIast).append("</span> ");
        html.append("<span class=\"hrefdata\"><span style=\"font-weight:normal; color:rgb(160,160,160);\"> ");
        html.append("[Printed book page ").append(pcLink).append("]</span></span></span><br>\n");
        html.append(bodyHtml);
        html.append(" <span class=\"lnum\"> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;&nbsp; &nbsp;&nbsp; &nbsp;");
        html.append("[<span title=\"Cologne record ID\" style=\"font-size:normal; color:rgb(160,160,160);\">ID=");
        html.append(h.lnum != null ? h.lnum : "").append("</span> ]</span></td></tr>\n");
        html.append("</tbody></table>\n</div>");

        return html.toString();
    }

    /** Renders the entry body: headword-repeat prefix, sense blocks, inline markup. */
    private static String renderBody(String body) {
        // Split on sense markers ∙²N / ∙¹N / ∙N, keeping the marker digit.
        // Everything before the first marker is the "preamble" (compound
        // parts, single-sense gloss, etc.) rendered without a numbered box.
        Pattern sensePat = Pattern.compile("∙[²¹]?(\\d+)");
        Matcher sm = sensePat.matcher(body);

        List<int[]> markerPositions = new ArrayList<>(); // [start, end, number]
        List<String> numbers = new ArrayList<>();
        while (sm.find()) {
            markerPositions.add(new int[]{sm.start(), sm.end()});
            numbers.add(sm.group(1));
        }

        StringBuilder out = new StringBuilder();
        if (markerPositions.isEmpty()) {
            out.append(renderInline(body.trim()));
            return out.toString();
        }

        String preamble = body.substring(0, markerPositions.get(0)[0]).trim();
        if (!preamble.isEmpty()) {
            out.append(renderInline(preamble)).append(" ");
        }

        for (int i = 0; i < markerPositions.size(); i++) {
            int contentStart = markerPositions.get(i)[1];
            int contentEnd = (i + 1 < markerPositions.size())
                ? markerPositions.get(i + 1)[0]
                : body.length();
            String senseText = body.substring(contentStart, contentEnd).trim();
            out.append("<div style=\"padding-left:1.0em;\"><strong>")
               .append(numbers.get(i))
               .append("</strong> ")
               .append(renderInline(senseText))
               .append("</div>");
        }
        return out.toString();
    }

    /** Renders inline markup: {#..#}, {@..@}, {%..%}, <ls>..</ls>, <ab>..</ab>. */
    private static String renderInline(String text) {
        StringBuilder out = new StringBuilder();
        int i = 0, n = text.length();
        while (i < n) {
            char c = text.charAt(i);

            if (c == '{' && i + 1 < n && text.charAt(i + 1) == '#') {
                int close = text.indexOf("#}", i + 2);
                if (close < 0) { out.append(c); i++; continue; }
                String slp1 = text.substring(i + 2, close);
                out.append("<span class=\"sdata\">").append(slp1ToIast(slp1)).append("</span>");
                i = close + 2;

            } else if (c == '{' && i + 1 < n && text.charAt(i + 1) == '@') {
                int close = findMatchingClose(text, i + 2, "@}");
                if (close < 0) { out.append(c); i++; continue; }
                String inner = text.substring(i + 2, close);
                out.append("<strong>").append(renderInline(inner)).append("</strong>");
                i = close + 2;

            } else if (c == '{' && i + 1 < n && text.charAt(i + 1) == '%') {
                int close = findMatchingClose(text, i + 2, "%}");
                if (close < 0) { out.append(c); i++; continue; }
                String inner = text.substring(i + 2, close);
                out.append("<i>").append(renderInline(inner)).append("</i>");
                i = close + 2;

            } else if (text.startsWith("<ls>", i)) {
                int close = text.indexOf("</ls>", i);
                if (close < 0) { out.append(c); i++; continue; }
                String ref = text.substring(i + 4, close);
                out.append(renderCitation(ref));
                i = close + 5;

            } else if (text.startsWith("<ab>", i)) {
                int close = text.indexOf("</ab>", i);
                if (close < 0) { out.append(c); i++; continue; }
                String abbr = text.substring(i + 4, close);
                out.append(renderAbbrev(abbr));
                i = close + 5;

            } else if (text.startsWith("<lex>", i)) {
                int close = text.indexOf("</lex>", i);
                if (close < 0) { out.append(c); i++; continue; }
                // <lex> tags are always inside {%..%} which already applies
                // italics; just pass the content through.
                out.append(text, i + 5, close);
                i = close + 6;

            } else if (text.startsWith("{sic}", i)) {
                i += 5; // drop marker

            } else if (c == '\n') {
                out.append(' ');
                i++;

            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** Finds the index of `closeSeq`, respecting simple nested `{...}` groups. */
    private static int findMatchingClose(String text, int from, String closeSeq) {
        int depth = 0;
        int i = from;
        while (i < text.length()) {
            if (text.startsWith(closeSeq, i) && depth == 0) return i;
            if (text.charAt(i) == '{') depth++;
            if (text.startsWith("}", i) && depth > 0
                && !text.startsWith(closeSeq, i)) depth--;
            i++;
        }
        return -1;
    }

    private static String renderAbbrev(String abbr) {
        String title = expandAbbrevTitle(abbr);
        if (title != null) {
            return "<span title=\"" + escape(title) + "\" "
                + "style=\"border-bottom: 1px dotted #000; text-decoration: none;\">"
                + escape(abbr) + "</span>";
        }
        return escape(abbr);
    }

    private static String expandAbbrevTitle(String abbr) {
        switch (abbr.trim()) {
            case "i. e.": return "id est, that is.";
            case "q. v.": return "quod vide, which see.";
            case "&c.":   return "et cetera.";
            case "Ved.":  return "Vedic.";
            default: return null;
        }
    }

    /** Renders a citation like "Y. 1. 59" or "Mb. 12. 158. 35" as a linked
     *  work abbreviation (if known) plus the reference numbers. */
    private static String renderCitation(String ref) {
        ref = ref.trim();
        // split leading abbreviation (letters + optional trailing '.') from
        // the trailing "N. N. N" numeric reference
        Matcher m = Pattern.compile("^([\\p{L}.]+)\\s*(.*)$").matcher(ref);
        if (!m.matches()) return "<span class=\"ls\">" + escape(ref) + "</span>";

        String abbr = m.group(1);
        String rest = m.group(2).trim(); // e.g. "1. 59" or "12. 158. 35"

        WorkInfo info = WORKS.get(abbr);
        String abbrSpan = "<span style=\"color:blue;\" class=\"ls\">" + escape(abbr) + "</span>";
        String restSpan = "<span class=\"ls\"> " + escape(rest) + "</span>";
        String inner = abbrSpan + restSpan;

        if (info == null) {
            return "<span class=\"ls\">" + inner + "</span>";
        }

        String href = null;
        if (info.urlTemplate != null) {
            String[] nums = rest.replaceAll("[.]", "").trim().split("\\s+");
            String url = info.urlTemplate;
            char[] letters = {'A', 'B', 'C', 'D'};
            for (int k = 0; k < nums.length && k < letters.length; k++) {
                url = url.replace("{" + letters[k] + "}", nums[k]);
            }
            href = url;
        }

        if (href == null) {
            return "<span class=\"ls\">" + inner + "</span>";
        }

        return "<a href=\"" + href + "\" title=\"" + escape(info.title) + "\" "
            + "style=\"text-decoration: none; border-bottom: 1px dotted #000;\" "
            + "target=\"_rvlink\"><span class=\"ls\">" + inner + "</span></a>";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("\"", "&quot;")
                .replace("<", "&lt;").replace(">", "&gt;");
    }

    // ------------------------------------------------------------------
    // Demo
    // ------------------------------------------------------------------
    public static void main(String[] args) {
        String rawMarkup =
            "<L>17575.002<pc>0838-1<k1>dEvaH<k2>dEvaH<e>1\n" +
            "{#dEva#} + .{@{#-vaH#}@}¦\n" +
            "∙²1 ({%<ab>i. e.</ab>%} {#vivAhaH#}) One of the eight forms of marriage, that in\n" +
            " which the daughter is given away at a sacrifice to the officiating priest;\n" +
            " {#yajYasya ftvije dEvaH#} <ls>Y. 1. 59</ls> (for the eight forms of marriage see\n" +
            " {#udvAha#} or <ls>Ms. 3. 21</ls>).\n" +
            "∙²2 A worshipper of god ({#devaBakta#}); {#dEvAn sarve guRavanto Bavanti#}\n" +
            " <ls>Mb. 12. 158. 35</ls>.\n" +
            "<LEND>";

        System.out.println(format(rawMarkup));
    }
}
