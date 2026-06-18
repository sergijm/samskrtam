package sm.selflearn.samskrtam.dictionary.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sm.selflearn.samskrtam.monierwilliams.dto.MwWordSearchDto;
import sm.selflearn.samskrtam.monierwilliams.service.MwDictionaryEntryService;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class DictionaryServiceTest {

    @Mock
    private TransliterationService transliterationService;

    @Mock
    private MwDictionaryEntryService mwDictionaryEntryService;

    @InjectMocks
    private DictionaryService dictionaryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void searchWords_shouldReturnListOfWords() {
        String query = "test";
        when(transliterationService.normalizeToSlp1(anyString(), any())).thenReturn("test");
        when(mwDictionaryEntryService.findWordsByKey1Normalized(anyString()))
                .thenReturn(Collections.singletonList(
                        MwWordSearchDto.builder().slp1Normalized("test").similarity(1.0).build()
                ));

        List<MwWordSearchDto> result = dictionaryService.searchWords(query);

        assertEquals(1, result.size());
        assertEquals("test", result.get(0).getSlp1Normalized());
    }

    @Test
    void getEntryBySlp1Spelling_shouldReturnEntry() {
        String slp1Spelling = "test";
        when(mwDictionaryEntryService.getEntriesByKey1(slp1Spelling)).thenReturn(Collections.emptyList());

        var result = dictionaryService.getEntryBySlp1Spelling(slp1Spelling);

        assertEquals(0, result.getEntries().size());
    }
}
