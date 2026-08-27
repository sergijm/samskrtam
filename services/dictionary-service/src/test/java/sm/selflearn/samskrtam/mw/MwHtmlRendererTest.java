package sm.selflearn.samskrtam.mw;

import org.junit.jupiter.api.Test;

import sm.selflearn.samskrtam.mw.dto.MwEntryDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MwHtmlRendererTest {

    @Test
    void renderEntry_withSanskritBody_rendersCologneBasicHtml() {
        String body = "<s>nara</s> ¦ a man, <ab n=\"N.\">N.</ab> name";
        String html = MwHtmlRenderer.renderEntry("nara", "12345", "529,1", "1", null, body);

        assertTrue(html.contains("<div id=\"CologneBasic\">"));
        assertTrue(html.contains("class=\"sdata_siddhanta\">नर</span>"));
        assertTrue(html.contains("Printed book page"));
        assertTrue(html.contains("529"));
    }

    @Test
    void renderEntry_devanagariHeadword_andAbbrevTooltip() {
        String body = "<s>devaH</s> ¦ god <ab>cf.</ab>";
        String html = MwHtmlRenderer.renderEntry("devaH", "999", "1,2", "1", null, body);

        assertTrue(html.contains("class=\"sdata_siddhanta\">देवः</span>"));
        assertTrue(html.contains("border-bottom: 1px dotted"));
        assertTrue(html.contains("confer, compare"));
    }

    @Test
    void renderEntry_nullBody_doesNotThrow() {
        String html = MwHtmlRenderer.renderEntry("kra", null, null, null, null, null);
        assertNotNull(html);
        assertTrue(html.contains("<div id=\"CologneBasic\">"));
    }

    @Test
    void renderEntry_stripsLeadingSeparator() {
        String body = "<s>nara</s> ¦ a man";
        String html = MwHtmlRenderer.renderEntry("nara", "1", "1,1", "1", null, body);
        assertFalse(html.contains("¦"));
        assertTrue(html.contains("a man"));
    }

    @Test
    void renderEntry_continuationEntry_noHlabel() {
        String html = MwHtmlRenderer.renderEntry("nara", "7", "1,1", "1A", null, "<s>nara</s>");
        assertFalse(html.contains("(H1A)"));
    }

    @Test
    void renderEntries_groupsRows_oneHeadwordAndMergedPages() {
        MwEntryDto a = MwEntryDto.builder().key1("devaH").entryId("1").entryNo("1")
                .pageCol("1,1").body("<s>devaH</s> god").build();
        MwEntryDto b = MwEntryDto.builder().key1("devaH").entryId("2").entryNo("1A")
                .pageCol("1,2").body("<s>devaH</s> deity").build();

        MwHtmlRenderer.RenderedArticle article = MwHtmlRenderer.renderEntries(List.of(a, b));

        assertTrue(article.headwordDevanagari.contains("देवः"));
        // each DB record on its own line
        assertTrue(article.bodyHtml.contains("(H1)"));
        assertTrue(article.bodyHtml.contains("<br>"));
        // all page references merged into a single line
        assertEquals(1, countOccurrences(article.pageRefsHtml, "[Printed book page"));
        assertTrue(article.pageRefsHtml.contains(">1.1</a>"));
        assertTrue(article.pageRefsHtml.contains(">1.2</a>"));
    }

    @Test
    void renderEntries_eachRecordOnNewLine() {
        MwEntryDto a = MwEntryDto.builder().key1("devaH").entryId("1").entryNo("1")
                .homonym(null).pageCol("1,1").body("<s>devaH</s> god").build();
        MwEntryDto b = MwEntryDto.builder().key1("devaH").entryId("2").entryNo("1")
                .homonym(null).pageCol("2,3").body("<s>devaH</s> deity").build();

        MwHtmlRenderer.RenderedArticle article = MwHtmlRenderer.renderEntries(List.of(a, b));

        // two records, each its own line, not merged into one
        assertEquals(1, countOccurrences(article.bodyHtml, "<br>"));
        assertTrue(article.pageRefsHtml.contains(">1.1</a>"));
        assertTrue(article.pageRefsHtml.contains(">2.3</a>"));
        assertTrue(article.bodyHtml.contains("god"));
        assertTrue(article.bodyHtml.contains("deity"));
    }

    @Test
    void renderEntries_differentHomonym_separateRows() {
        MwEntryDto a = MwEntryDto.builder().key1("devaH").entryId("1").entryNo("1")
                .homonym("1").pageCol("1,1").body("<s>devaH</s> god").build();
        MwEntryDto b = MwEntryDto.builder().key1("devaH").entryId("2").entryNo("1")
                .homonym("2").pageCol("1,2").body("<s>devaH</s> god2").build();

        MwHtmlRenderer.RenderedArticle article = MwHtmlRenderer.renderEntries(List.of(a, b));

        // distinct homonym -> distinct groups, each with its own (H..) label
        assertEquals(2, countOccurrences(article.bodyHtml, "(H1)"));
        assertEquals(2, countOccurrences(article.bodyHtml, "<br>"));
        assertTrue(article.bodyHtml.contains("god"));
        assertTrue(article.bodyHtml.contains("god2"));
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0, idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) { count++; idx += needle.length(); }
        return count;
    }
}
