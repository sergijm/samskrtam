package sm.selflearn.samskrtam.monierwilliams.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sm.selflearn.samskrtam.dictionary.service.TransliterationService;
import sm.selflearn.samskrtam.monierwilliams.dto.MwDictionaryEntryDto;
import sm.selflearn.samskrtam.monierwilliams.dto.MwWordSearchDto;
import sm.selflearn.samskrtam.monierwilliams.entity.MwEntry;
import sm.selflearn.samskrtam.monierwilliams.model.SanskritWordSearchResult;
import sm.selflearn.samskrtam.monierwilliams.repository.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class MwDictionaryEntryServiceTest {

    @Mock private MwEntryRepository entryRepository;
    @Mock private MwSanskritWordRepository sanskritWordRepository;
    @Mock private MwHomonymRepository homonymRepository;
    @Mock private MwAbbreviationRepository abbreviationRepository;
    @Mock private MwLiterarySourceRepository literarySourceRepository;
    @Mock private MwInfoRepository infoRepository;
    @Mock private MwLexicalInfoRepository lexicalInfoRepository;
    @Mock private TransliterationService transliterationService;
    @Mock private MwXmlTranslationExtractor xmlTranslationExtractor;

    @InjectMocks
    private MwDictionaryEntryService service;

    private MwEntry mockEntry;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockEntry = new MwEntry();
        mockEntry.setId(1);
        mockEntry.setRecordIdFull("12345");
        mockEntry.setKey1("test");
        mockEntry.setKey2("test_alt");
        mockEntry.setHomonymNum("1");
        mockEntry.setECode("1");
        mockEntry.setBody("<body>test body</body>");
    }

    @Nested
    @DisplayName("Get Entry Methods")
    class GetEntry {

        @Test
        void getEntryByRecordId_shouldReturnFullEntry() {
            when(entryRepository.findByRecordIdFull("12345")).thenReturn(Optional.of(mockEntry));
            when(xmlTranslationExtractor.extractTranslation(any(MwEntry.class))).thenReturn("translation");

            MwDictionaryEntryDto result = service.getEntryByRecordId("12345");

            assertNotNull(result);
            assertEquals("test", result.getKey1());
            assertEquals("translation", result.getMainTranslation());
            assertTrue(result.getDisplayTitle().contains("test_alt"));
            assertTrue(result.getDisplayTitle().contains("(Hom. 1)"));
        }

        @Test
        void getEntryByRecordId_whenNotFound_shouldThrowException() {
            when(entryRepository.findByRecordIdFull("notfound")).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> service.getEntryByRecordId("notfound"));
        }

        @Test
        void getEntriesByKey1_shouldReturnListOfEntries() {
            when(transliterationService.slp1RemoveStress("test")).thenReturn("test");
            when(entryRepository.findByKey1Normalized("test")).thenReturn(Collections.singletonList(mockEntry));
            when(xmlTranslationExtractor.extractTranslation(any(MwEntry.class))).thenReturn("translation");

            List<MwDictionaryEntryDto> result = service.getEntriesByKey1("test");

            assertEquals(1, result.size());
            assertEquals("test", result.get(0).getKey1());
        }
    }

    @Nested
    @DisplayName("Find Words Method")
    class FindWords {

        @Test
        void findWordsByKey1Normalized_shouldReturnMappedDtos() {
            SanskritWordSearchResult searchResult = new SanskritWordSearchResult() {
                @Override
                public String getSlp1Spelling() {
                    return "";
                }

                @Override
                public String getSlp1Normalized() {
                    return "";
                }

                @Override
                public String getIastSpelling() {
                    return "";
                }

                @Override
                public Boolean getIsPrimaryHeadword() {
                    return null;
                }

                @Override
                public Double getSimilarity() {
                    return 0.0;
                }
            };
            when(entryRepository.findWordsByKey1NormalizedSimilarity("query")).thenReturn(Collections.singletonList(searchResult));

            List<MwWordSearchDto> result = service.findWordsByKey1Normalized("query");

            assertEquals(1, result.size());
            assertEquals("test", result.get(0).getSlp1Spelling());
            assertEquals(1.0, result.get(0).getSimilarity());
        }
    }
}
