package sm.selflearn.samskrtam.dictionary.service;

import sm.selflearn.samskrtam.common.transliteration.TransliterationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransliterationServiceTest {

    private TransliterationService transliterationService;

    @BeforeEach
    void setUp() {
        transliterationService = new TransliterationService();
    }

    @Test
    void normalizeToSlp1_withDevanagari_shouldTransliterate() {
        String input = "नमस्ते";
        String expected = "namaste";
        String result = transliterationService.normalizeToSlp1(input, null);
        assertEquals(expected, result);
    }

    @Test
    void normalizeToSlp1_withIast_shouldTransliterate() {
        String input = "namaste";
        String expected = "namaste";
        String result = transliterationService.normalizeToSlp1(input, "iast");
        assertEquals(expected, result);
    }

    @Test
    void normalizeToSlp1_withItrans_shouldTransliterate() {
        String input = "namaste";
        String expected = "namaste";
        String result = transliterationService.normalizeToSlp1(input, "itrans");
        assertEquals(expected, result);
    }

    @Test
    void slp1RemoveStress_shouldRemoveNonAlphabeticChars() {
        String input = "na'ma/s-te";
        String expected = "namaste";
        String result = transliterationService.slp1RemoveStress(input);
        assertEquals(expected, result);
    }

    @Test
    void slp1RemoveStress_withNullInput_shouldReturnNull() {
        assertNull(transliterationService.slp1RemoveStress(null));
    }
}
