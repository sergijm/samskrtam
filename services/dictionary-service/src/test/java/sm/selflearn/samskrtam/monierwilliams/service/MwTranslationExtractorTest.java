package sm.selflearn.samskrtam.monierwilliams.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.monierwilliams.entity.MwEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MwTranslationExtractorTest {

    private MwTranslationExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new MwTranslationExtractor();
    }

    @Nested
    @DisplayName("Entry Type Specific Extraction")
    class EntryTypeExtraction {

        @Test
        void extractTranslation_mainEntry_eCode1_shouldExtract() {
            MwEntry entry = new MwEntry();
            entry.setECode("1");
            entry.setBody("<body><s>word</s><lex>m.</lex> a meaning. <ls>REF.</ls></body>");
            String expected = "a meaning";
            String result = extractor.extractTranslation(entry);
            assertEquals(expected, result);
        }

        @Test
        void extractTranslation_mainEntry_eCode1A_shouldExtract() {
            MwEntry entry = new MwEntry();
            entry.setECode("1A");
            entry.setBody("<body><s>word</s><lex>f.</lex> (<i>cf.</i>) another meaning. <ls>REF.</ls></body>");
            String expected = "another meaning";
            String result = extractor.extractTranslation(entry);
            assertEquals(expected, result);
        }

        @Test
        void extractTranslation_subEntry_eCode2_shouldExtract() {
            MwEntry entry = new MwEntry();
            entry.setECode("2");
            entry.setBody("<body><s>word</s><lex>n.</lex> sub meaning. <ls>REF.</ls></body>");
            String expected = "sub meaning";
            String result = extractor.extractTranslation(entry);
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("Generic and Fallback Extraction")
    class GenericExtraction {

        @Test
        void extractTranslation_genericEntry_shouldUseFallback() {
            MwEntry entry = new MwEntry();
            entry.setECode("X"); // Not a main or sub-entry code
            entry.setBody("<body><s>word</s> some text. <ls>REF.</ls></body>");
            String expected = "some text.";
            String result = extractor.extractTranslation(entry);
            assertEquals(expected, result);
        }

        @Test
        void extractTranslation_mainEntryFailed_shouldUseFallback() {
            MwEntry entry = new MwEntry();
            entry.setECode("1");
            // This body does not match the main entry pattern
            entry.setBody("<body><s>word</s> a generic text. <ls>REF.</ls></body>");
            String expected = "a generic text.";
            String result = extractor.extractTranslation(entry);
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
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
    }
}
