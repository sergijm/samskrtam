package sm.selflearn.samskrtam.dictionary.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sm.selflearn.samskrtam.monierwilliams.service.MwDictionaryEntryService;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class DictionaryServiceTest {

    @Mock
    private MwDictionaryEntryService mwDictionaryEntryService;

    @InjectMocks
    private DictionaryService dictionaryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void searchByLemma_shouldReturnEntry() {
        String query = "rāma";
        when(mwDictionaryEntryService.getEntriesByLemmaIast(anyString())).thenReturn(Collections.emptyList());

        var result = dictionaryService.searchByLemma(query);

        assertEquals(0, result.getEntries().size());
    }

    @Test
    void getEntryBySlp1Spelling_shouldReturnEntry() {
        String slp1Spelling = "test";
        when(mwDictionaryEntryService.getEntriesByKey1(slp1Spelling)).thenReturn(Collections.emptyList());

        var result = dictionaryService.getEntryBySlp1Spelling(slp1Spelling);

        assertEquals(0, result.getEntries().size());
    }
}
