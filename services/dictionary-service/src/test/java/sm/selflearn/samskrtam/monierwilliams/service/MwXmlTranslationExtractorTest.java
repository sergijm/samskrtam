package sm.selflearn.samskrtam.monierwilliams.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.monierwilliams.entity.MwEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MwXmlTranslationExtractorTest {

    private MwXmlTranslationExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new MwXmlTranslationExtractor();
    }

    @Nested
    @DisplayName("Standard Extraction Scenarios")
    class StandardExtraction {

        @Test
        void extractTranslation_withSimpleBody_shouldExtract() {
            MwEntry entry = new MwEntry();
            entry.setBody("<body><s>word</s><lex>m.</lex> a meaning. <ls>REF.</ls></body>");
            String expected = "a meaning.";
            String result = extractor.extractTranslation(entry);
            assertEquals(expected, result);
        }

        @Test
        void extractTranslation_withComplexBody_shouldExtract() {
            MwEntry entry = new MwEntry();
            entry.setBody("<body><s>word</s><lex>f.</lex> (<i>cf.</i> <ab>Goth.</ab> <i>gawaurd</i>) a complex meaning. <ls>REF.</ls></body>");
            String expected = "a complex meaning.";
            String result = extractor.extractTranslation(entry);
            assertEquals(expected, result);
        }

        @Test
        void extractTranslation_withMultipleSentences_shouldTakeFirst() {
            MwEntry entry = new MwEntry();
            entry.setBody("<body><s>word</s><lex>n.</lex> First sentence. Second sentence. <ls>REF.</ls></body>");
            String expected = "First sentence.";
            String result = extractor.extractTranslation(entry);
            assertEquals(expected, result);
        }

        @Test
        void extractTranslation_withNoLex_shouldExtractFromBody() {
            MwEntry entry = new MwEntry();
            entry.setBody("<body><s>word</s> some text. <ls>REF.</ls></body>");
            String expected = "some text.";
            String result = extractor.extractTranslation(entry);
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Malformed XML")
    class EdgeCases {

        @Test
        void extractTranslation_withEmptyBody_shouldReturnEmpty() {
            MwEntry entry = new MwEntry();
            entry.setBody("");
            String result = extractor.extractTranslation(entry);
            assertEquals("", result);
        }

        @Test
        void extractTranslation_withNullBody_shouldReturnEmpty() {
            MwEntry entry = new MwEntry();
            entry.setBody(null);
            String result = extractor.extractTranslation(entry);
            assertEquals("", result);
        }

        @Test
        void extractTranslation_withNoTranslationText_shouldReturnEmpty() {
            MwEntry entry = new MwEntry();
            entry.setBody("<body><s>word</s><lex>m.</lex><ls>REF.</ls></body>");
            String result = extractor.extractTranslation(entry);
            assertEquals("", result);
        }

        @Test
        void extractTranslation_withMalformedXml_shouldReturnEmpty() {
            MwEntry entry = new MwEntry();
            entry.setBody("<body><s>word</s<lex>m.</lex> a meaning.</body>");
            String result = extractor.extractTranslation(entry);
            assertEquals("", result, "Should fail gracefully on malformed XML");
        }

        @Test
        void extractTranslation_withLongTranslation_shouldBeTruncated() {
            MwEntry entry = new MwEntry();
            String longText = "This is a very long text that should be truncated because it exceeds the maximum length allowed by the cleaning process. It keeps going on and on, past the two hundred character mark, and even further beyond the three hundred character limit, just to be absolutely sure that the truncation logic is triggered correctly.";
            entry.setBody("<body><s>word</s><lex>n.</lex> " + longText + "</body>");
            String expected = longText.substring(0, 300) + "...";
            String result = extractor.extractTranslation(entry);
            assertEquals(expected, result);
        }
    }
}
