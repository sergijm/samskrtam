package sm.selflearn.samskrtam.mw;

import java.util.*;
import java.util.regex.*;

import sm.selflearn.samskrtam.common.transliteration.TransliterationService;
import sm.selflearn.samskrtam.mw.dto.MwEntryDto;

/**
 * Converts the "cologne_mw"."entries" body payload (Monier-Williams,
 * Cologne digitization) into HTML that looks like the reference markup
 * used on sanskrit-lexicon.uni-koeln.de inside <div id="CologneBasic">.
 *
 * Pipeline
 * --------
 *  1. MwRow           – one DB row (key1, entry_id, page_col, entry_no, body).
 *  2. MiniXmlParser    – turns the pseudo-xml in "body" into a small AST
 *                        (TextNode / ElementNode), tolerant of the
 *                        self-closing tags used in this format
 *                        (<info .../>, <shortlong/>, <srs/>, <pb .../>, <div .../>).
 *  3. BodyRenderer     – walks the AST and emits the same span/class
 *                        structure as the reference HTML (sdata_siddhanta
 *                        spans for Sanskrit, dotted-underline tooltip spans
 *                        for abbreviations/gender/literary sources, etc.)
 *  4. TransliterationService.slp1ToIast (из samskrtam-commons) –
 *                        транслитерирует SLP1-санскрит из <s>, <s1>,
 *                        <ab n=".." slp1="..">, <bot>, <bio> в IAST
 *                        для sdata_siddhanta-спанов.
 *  5. ArticleRenderer  – emits one <div id="CologneBasic"> ... </div> block
 *                        per entry, one <tr> per sub-entry, exactly like the sample.
 *
 * NOTE on completeness
 * ---------------------
 * The real Cologne site additionally consults several *external* lookup
 * tables that are not part of the "entries" table itself:
 *   - mwab       : full text of general abbreviations ("cf." -> "confer, compare")
 *   - mwauth     : literary-source / author expansions used as <ls> tooltips
 *   - per-work hyperlink tables (MBh., R., Hariv., ... -> sanskrit-lexicon-scans.github.io links)
 * These are represented below as small, easily-extensible Maps
 * (ABBREVIATIONS, LEX_TOOLTIPS, LIT_SOURCES). Populate them from the real
 * mwab/mwauth tables to get 1:1 output; the algorithm/structure itself is complete.
 */
public class MwHtmlRenderer {

    // ------------------------------------------------------------------
    // 1. Row model
    // ------------------------------------------------------------------

    /** One row from cologne_mw.entries. */
    public static class MwRow {
        String id;
        String entryId;
        String pageCol;   // e.g. "529,1"
        String key1;
        String key2;
        String homonym;   // may be null
        String entryNo;   // "1", "1A", "1B", "3", ...
        String body;      // the pseudo-xml payload

        public MwRow(String id, String entryId, String pageCol, String key1,
                     String key2, String homonym, String entryNo, String body) {
            this.id = id;
            this.entryId = entryId;
            this.pageCol = pageCol;
            this.key1 = key1;
            this.key2 = key2;
            this.homonym = homonym;
            this.entryNo = entryNo;
            this.body = body;
        }

        /** "H" + homonym-independent primary number, e.g. "1", "1A", "3" -> shown as (H1)/(H1A)/(H3) */
        String hLabel() {
            return "H" + entryNo;
        }
    }

    // ------------------------------------------------------------------
    // 2. Minimal XML-ish parser for the <body> payload
    // ------------------------------------------------------------------

    interface Node {}

    static class TextNode implements Node {
        final String text;
        TextNode(String t) { this.text = t; }
    }

    static class ElementNode implements Node {
        final String name;
        final Map<String, String> attrs;
        final List<Node> children = new ArrayList<>();
        ElementNode(String name, Map<String, String> attrs) {
            this.name = name;
            this.attrs = attrs;
        }
    }

    /**
     * Very small recursive-descent parser. Handles:
     *   - plain text (including entity-like sequences, left as-is)
     *   - self-closing tags:  <tag attr="v" .../>
     *   - paired tags:        <tag attr="v">...</tag>
     * Tags are NOT required to be well-formed XML (e.g. attribute values
     * are taken verbatim between the surrounding quotes); this matches the
     * loose pseudo-xml described in mw-meta2.txt.
     */
    static class MiniXmlParser {
        private static final Pattern TAG =
            Pattern.compile("<(/?)([a-zA-Z][\\w]*)((?:\\s+[\\w:-]+=\"[^\"]*\")*)\\s*(/?)>");
        private static final Pattern ATTR =
            Pattern.compile("([\\w:-]+)=\"([^\"]*)\"");

        private final String src;
        private int pos = 0;

        MiniXmlParser(String src) { this.src = src; }

        List<Node> parse() {
            return parseUntil(null);
        }

        private List<Node> parseUntil(String closingTag) {
            List<Node> nodes = new ArrayList<>();
            Matcher m = TAG.matcher(src);
            while (pos < src.length()) {
                if (!m.find(pos)) {
                    if (pos < src.length()) nodes.add(new TextNode(src.substring(pos)));
                    pos = src.length();
                    break;
                }
                if (m.start() > pos) {
                    nodes.add(new TextNode(src.substring(pos, m.start())));
                }
                boolean isClosing = !m.group(1).isEmpty();
                String tagName = m.group(2);
                String attrStr = m.group(3);
                boolean selfClosing = !m.group(4).isEmpty();
                pos = m.end();

                if (isClosing) {
                    // closing tag: if it matches what caller is waiting for, stop.
                    if (closingTag != null && tagName.equals(closingTag)) {
                        return nodes;
                    }
                    // stray/unmatched closing tag: ignore.
                    continue;
                }

                Map<String, String> attrs = new LinkedHashMap<>();
                Matcher am = ATTR.matcher(attrStr);
                while (am.find()) attrs.put(am.group(1), am.group(2));

                if (selfClosing) {
                    nodes.add(new ElementNode(tagName, attrs));
                } else {
                    ElementNode el = new ElementNode(tagName, attrs);
                    el.children.addAll(parseUntil(tagName));
                    nodes.add(el);
                }
            }
            return nodes;
        }
    }

    // ------------------------------------------------------------------
    // 3. SLP1 -> IAST transliteration (общая реализация из samskrtam-commons)
    // ------------------------------------------------------------------

    private static final TransliterationService TRANSLITERATION = new TransliterationService();

    // ------------------------------------------------------------------
    // 4. Lookup tables (stand-ins for mwab / mwauth external tables)
    // ------------------------------------------------------------------

    /** general abbreviation -> tooltip text, populate fully from mwab. */
    static final Map<String, String> ABBREVIATIONS = new HashMap<>();
    static {
        ABBREVIATIONS.put("N.", "Name (also = title or epithet)");
        ABBREVIATIONS.put("cf.", "confer, compare");
        ABBREVIATIONS.put("pl.", "plural number");
        ABBREVIATIONS.put("g.", "gaṇa");
        ABBREVIATIONS.put("prob.", "probably");
        ABBREVIATIONS.put("q.v.", "quod vide (which see)");
        ABBREVIATIONS.put("&c.", "et cetera");
    }

    /** gender code (as it appears inside <lex>) -> tooltip text. */
    static final Map<String, String> GENDER_TOOLTIPS = new HashMap<>();
    static {
        GENDER_TOOLTIPS.put("m.", "masculine gender");
        GENDER_TOOLTIPS.put("m", "masculine gender");
        GENDER_TOOLTIPS.put("f.", "feminine");
        GENDER_TOOLTIPS.put("f", "feminine");
        GENDER_TOOLTIPS.put("n.", "neuter gender");
        GENDER_TOOLTIPS.put("n", "neuter gender");
        GENDER_TOOLTIPS.put("ind.", "indeclinable");
    }

    /** literary-source abbreviation -> full title, populate fully from mwauth. */
    static final Map<String, String> LIT_SOURCES = new HashMap<>();
    static {
        LIT_SOURCES.put("TS.", "Taittirīya-saṃhitā (Title)");
        LIT_SOURCES.put("Mn.", "Manu-smṛti (Title)");
        LIT_SOURCES.put("Lalit.", "Lalita-vistara (Title)");
        LIT_SOURCES.put("Kaś.", "Kāśikā-vṛtti (Title)");
        LIT_SOURCES.put("MBh.", "Mahābhārata (Title)");
        LIT_SOURCES.put("W.", "Horace H. Wilson, author of first Sanskrit-English dictionary in 1819 (Author)");
        LIT_SOURCES.put("L.", "Lexicographers, esp. such as Amara-siṃha, Halāyudha, Hemacandra, etc. (Author)");
    }

    // ------------------------------------------------------------------
    // 5. Body -> HTML
    // ------------------------------------------------------------------

    static class BodyRenderer {

        String render(List<Node> nodes) {
            stripLeadingSeparator(nodes);
            StringBuilder sb = new StringBuilder();
            for (Node n : nodes) sb.append(renderNode(n));
            return sb.toString();
        }

        /**
         * Removes a "¦" sense-separator (with surrounding whitespace) when it
         * stands at the very beginning of the entry text, i.e. the first
         * top-level text node. Only the leading occurrence is affected.
         */
        private void stripLeadingSeparator(List<Node> nodes) {
            for (int i = 0; i < nodes.size(); i++) {
                Node n = nodes.get(i);
                if (n instanceof TextNode) {
                    String t = ((TextNode) n).text;
                    if (t.trim().isEmpty()) continue; // skip blank leading text gaps
                    String stripped = t.replaceFirst("^\\s*¦\\s*", "");
                    if (!stripped.equals(t)) {
                        nodes.set(i, new TextNode(stripped));
                    }
                    return; // only the first non-blank text node
                }
                // leading elements (e.g. <s> headword) are skipped; keep scanning
            }
        }

        private String renderNode(Node node) {
            if (node instanceof TextNode) {
                return escapeButKeepEntities(((TextNode) node).text);
            }
            ElementNode el = (ElementNode) node;
            switch (el.name) {
                case "s":       return sanskritSpans(plainText(el));
                case "s1":      return sanskritSpans(el.attrs.getOrDefault("slp1", plainText(el)));
                case "ns":      return plainText(el); // non-Sanskrit IAST word, printed as-is
                case "hom":     return "<span class=\"hom\" title=\"Homonym\">" + plainText(el) + "</span>";
                case "ab":      return renderAbbrev(el);
                case "lex":     return renderLex(el);
                case "ls":      return renderLitSource(el);
                case "bot":
                case "bio":     return sanskritSpans(plainText(el)); // Linnaean names, printed like Sanskrit
                case "i":       return "<i>" + render(el.children) + "</i>";
                case "lang":    return render(el.children); // cognate/etymology word, printed plain
                case "pcol":    return plainText(el); // internal page-col reference (kept as plain text)
                case "pb":      return ""; // page break marker inside running text, not shown inline
                case "div":     return ""; // logical break marker (to/vp/p), no direct visual output here
                case "info":    return ""; // meta-only, never rendered
                case "shortlong":
                case "srs":     return ""; // diacritic markers, consumed by sanskritSpans() already
                default:        return render(el.children);
            }
        }

        /** Concatenate all text (and inline shortlong/srs markers) under an element, ignoring nested tags' own markup. */
        private String plainText(ElementNode el) {
            StringBuilder sb = new StringBuilder();
            collectText(el.children, sb);
            return sb.toString();
        }

        private void collectText(List<Node> nodes, StringBuilder sb) {
            for (Node n : nodes) {
                if (n instanceof TextNode) {
                    sb.append(((TextNode) n).text);
                } else {
                    ElementNode e = (ElementNode) n;
                    if (e.name.equals("shortlong") || e.name.equals("srs")) {
                        // diacritic hint only, no character of its own in SLP1
                        continue;
                    }
                    collectText(e.children, sb);
                }
            }
        }

        /**
         * Renders one or more SLP1 words as Devanagari sdata_siddhanta spans.
         * Compound headwords using the em-dash separator (e.g. "nara—da")
         * are split into one span per segment, exactly like the reference
         * HTML style.
         */
        private String sanskritSpans(String slp1raw) {
            String slp1 = slp1raw.trim();
            if (slp1.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            // split on the em-dash separator used for compound headwords (e.g. "nara—da")
            String[] segs = slp1.split("—", -1);
            for (int i = 0; i < segs.length; i++) {
                if (i > 0) sb.append("<span class=\"sdata_siddhanta\">—")
                             .append(TRANSLITERATION.slp1ToIast(segs[i]))
                             .append("</span>");
                else
                    sb.append("<span class=\"sdata_siddhanta\">")
                       .append(TRANSLITERATION.slp1ToIast(segs[i]))
                       .append("</span>");
            }
            return sb.toString();
        }

        private String renderAbbrev(ElementNode el) {
            String text = plainText(el);
            String tooltip = el.attrs.containsKey("n")
                    ? el.attrs.get("n")
                    : ABBREVIATIONS.getOrDefault(text, text);
            return "<span title=\"" + escapeAttr(tooltip) + "\" " +
                   "style=\"border-bottom: 1px dotted #000; text-decoration: none;\">" +
                   escapeButKeepEntities(text) + "</span>";
        }

        private String renderLex(ElementNode el) {
            String text = plainText(el);
            String key = text.trim();
            String tooltip = GENDER_TOOLTIPS.getOrDefault(key, key);
            return "<span title=\"" + escapeAttr(tooltip) + "\" " +
                   "style=\"border-bottom: 1px dotted #000; text-decoration: none;\">" +
                   escapeButKeepEntities(text) + "</span>";
        }

        private String renderLitSource(ElementNode el) {
            String text = plainText(el).trim();
            // literary-source abbreviations are keyed by their leading token,
            // e.g. "Pāṇ. iii, 1, 85" -> key "Pāṇ."
            String key = text.split("\\s+", 2)[0];
            String tooltip = LIT_SOURCES.getOrDefault(key, key + " (Title)");
            // NOTE: the live site additionally hyperlinks some sources to
            // sanskrit-lexicon-scans.github.io using a per-work numeric
            // lookup table; that table is external to the entries row and
            // is intentionally left as an extension point here.
            return "<span style=\"border-bottom: 1px dotted #000; color:#8080ff;\" title=\"" +
                   escapeAttr(tooltip) + "\">" + escapeButKeepEntities(text) + "</span>";
        }

        private String escapeAttr(String s) {
            return s.replace("&", "&amp;").replace("\"", "&quot;");
        }

        private String escapeButKeepEntities(String s) {
            // body text already contains literal unicode punctuation (¦, —, etc.)
            // no HTML-escaping needed beyond raw text since the source has no < or & of its own here.
            return s;
        }
    }

    // ------------------------------------------------------------------
    // 6. Whole-article assembly (one headword entry, possibly several sub-entries)
    // ------------------------------------------------------------------

    /** Result of rendering one headword article: header bits are kept separate
     *  from the body so the frontend can lay them out (headword left, page
     *  references right-aligned) with its own components. */
    public static class RenderedArticle {
         public final String headwordIast;
        public final String pageRefsHtml;
        public final String bodyHtml;
        public RenderedArticle(String headwordIast, String pageRefsHtml, String bodyHtml) {
            this.headwordIast = headwordIast;
            this.pageRefsHtml = pageRefsHtml;
            this.bodyHtml = bodyHtml;
        }
    }

    static class ArticleRenderer {
        private final BodyRenderer bodyRenderer = new BodyRenderer();

        /** rows must already share the same key1/devanagari headword and be in entry_no order. */
        RenderedArticle renderArticle(List<MwRow> rows) {
            if (rows.isEmpty()) return new RenderedArticle("", "", "");
            String headwordIast = TRANSLITERATION.slp1ToIast(rows.get(0).key1);

            // All unique page_col values across the whole article, merged into a
            // single "[Printed book page a, b, c]" line (one link per page).
            LinkedHashSet<String> allPages = new LinkedHashSet<>();
            for (MwRow r : rows) {
                if (r.pageCol != null && !r.pageCol.isBlank()) allPages.add(r.pageCol.trim());
            }
            String pageRefsHtml = renderPageRefsLine(new ArrayList<>(allPages));

            // subgroup by (entry_no, homonym): each group becomes a labelled block.
            // Within a group every database record starts on its own line
            // (separated by <br>); the records are NOT merged into one line.
            Map<String, List<MwRow>> groups = new LinkedHashMap<>();
            for (MwRow r : rows) {
                String gk = (r.entryNo == null ? "" : r.entryNo) + "|"
                        + (r.homonym == null ? "" : r.homonym);
                groups.computeIfAbsent(gk, k -> new ArrayList<>()).add(r);
            }

            StringBuilder body = new StringBuilder();
            boolean firstGroup = true;
            for (List<MwRow> group : groups.values()) {
                if (!firstGroup) body.append("<br>\n<br>\n");
                firstGroup = false;

                MwRow first = group.get(0);
                if (isPrimarySubentry(first.entryNo)) {
                    body.append(" <span class=\"mw-group-label\">(")
                        .append(first.hLabel()).append(")</span> ");
                }

                boolean firstRecord = true;
                for (MwRow row : group) {
                    if (!firstRecord) body.append("<br>\n");
                    firstRecord = false;

                    MiniXmlParser parser = new MiniXmlParser(row.body);
                    body.append(bodyRenderer.render(parser.parse()));
                }
            }

            return new RenderedArticle(headwordIast, pageRefsHtml, body.toString());
        }

        /** Renders the merged "[Printed book page a, b, c]" line with one link per page. */
        private String renderPageRefsLine(List<String> pageCols) {
            if (pageCols.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            sb.append(" [Printed book page ");
            for (int i = 0; i < pageCols.size(); i++) {
                if (i > 0) sb.append(", ");
                String pc = pageCols.get(i);
                int comma = pc.indexOf(',');
                String page = comma < 0 ? pc : pc.substring(0, comma);
                String col = comma < 0 ? "" : pc.substring(comma + 1);
                String label = comma < 0 ? pc : page + "." + col;
                sb.append("<a href=\"//www.sanskrit-lexicon.uni-koeln.de/scans/csl-apidev/servepdf.php?dict=MW&amp;page=")
                  .append(page).append("\" target=\"_MW\">").append(label).append("</a>");
            }
            sb.append("]");
            return sb.toString();
        }

        /** entry_no of "1", "2", "3", "1B", "2B" (a *new* H-block) vs "1A" (continuation, no own H-label). */
        private boolean isPrimarySubentry(String entryNo) {
            // Convention observed in the sample data: "…A" suffix = continuation
            // of the previous H-block (rendered without its own "(H..)" header);
            // everything else ("1", "1B", "3", ...) starts a new labelled block.
            return !entryNo.endsWith("A");
        }
    }

    // ------------------------------------------------------------------
    // 7. Public API
    // ------------------------------------------------------------------

    /**
     * Renders one Monier-Williams entry (the body pseudo-xml plus its row
     * metadata) into a self-contained <div id="CologneBasic"> HTML block
     * (headword + merged page references in the header, then the body table).
     *
     * @param key1     headword in SLP1 (column key1)
     * @param entryId  Cologne record id (column entry_id)
     * @param pageCol  page,column reference (column page_col), e.g. "529,1"
     * @param entryNo  sub-entry number (column entry_no), e.g. "1", "1A", "3"
     * @param homonym  homonym marker (column homonym), may be null
     * @param body     the pseudo-xml body payload (column body)
     */
    public static String renderEntry(String key1, String entryId, String pageCol,
                                     String entryNo, String homonym, String body) {
        if (body == null) body = "";
        if (key1 == null) key1 = "";
        if (entryNo == null) entryNo = "1";
        if (pageCol == null) pageCol = "";
        MwRow row = new MwRow(null, entryId, pageCol, key1, null, homonym, entryNo, body);
        RenderedArticle a = new ArticleRenderer().renderArticle(List.of(row));
        StringBuilder html = new StringBuilder();
        html.append("<div id=\"CologneBasic\">\n");
        html.append("<h1>&nbsp;<span class=\"sdata_siddhanta\">").append(a.headwordIast).append("</span>");
        if (!a.pageRefsHtml.isEmpty()) html.append("&nbsp;").append(a.pageRefsHtml);
        html.append("</h1>\n");
        html.append(a.bodyHtml).append("\n</div>");
        return html.toString();
    }

    /**
     * Renders a group of Monier-Williams entries that share the same headword
     * into a single article, returning its parts separately (headword,
     * merged page references, body) so the frontend can lay them out.
     */
    public static RenderedArticle renderEntries(java.util.List<MwEntryDto> rows) {
        List<MwRow> mwRows = new ArrayList<>();
        for (MwEntryDto dto : rows) {
            mwRows.add(new MwRow(
                    null,
                    dto.getEntryId(),
                    dto.getPageCol(),
                    dto.getKey1(),
                    dto.getKey2(),
                    dto.getHomonym(),
                    dto.getEntryNo(),
                    dto.getBody()));
        }
        return new ArticleRenderer().renderArticle(mwRows);
    }
}
