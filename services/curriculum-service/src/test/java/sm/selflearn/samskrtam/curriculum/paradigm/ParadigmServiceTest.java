package sm.selflearn.samskrtam.curriculum.paradigm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmPageDto;
import sm.selflearn.samskrtam.content.model.Gender;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.model.MorphologyClass;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParadigmServiceTest {

    private LexemeRepository lexemeRepository;
    private ParadigmService service;

    @BeforeEach
    void setUp() {
        lexemeRepository = mock(LexemeRepository.class);
        service = new ParadigmService(
                mock(ParadigmStemRepository.class), mock(ParadigmFormRepository.class), lexemeRepository);
    }

    private Lexeme lexeme(UUID id, String iast, String deva, LexemeGender gender, String... classCodes) {
        Lexeme l = new Lexeme();
        l.setId(id);
        l.setLemmaIast(iast);
        l.setLemmaDevanagari(deva);
        l.setGender(gender);
        Set<MorphologyClass> classes = new HashSet<>();
        for (String code : classCodes) {
            MorphologyClass mc = new MorphologyClass();
            mc.setCode(code);
            classes.add(mc);
        }
        l.setMorphologyClasses(classes);
        return l;
    }

    @Test
    void aStemMerged_lexemeGenderNull_usesActualClass() {
        when(lexemeRepository.findWithMorphologyByCodeIn(eq(List.of("a-stem-masc", "a-stem-neut"))))
                .thenReturn(List.of(lexeme(UUID.fromString("00000000-0000-0000-0000-000000000001"), "nara", "नर", null, "a-stem-masc")));

        DeclensionParadigmPageDto page = service.getParadigmPage("a-stem", 0);

        assertThat(page.getTotalCount()).isEqualTo(1);
        assertThat(page.getParadigm()).isNotNull();
        assertThat(page.getParadigm().getVowelType()).isEqualTo(VowelType.A_STEM);
        assertThat(page.getParadigm().getGender()).isEqualTo(Gender.MASCULINE);
        assertThat(page.getParadigm().getForms()).isNotEmpty();
        assertThat(page.getParadigm().getForms())
                .anyMatch(f -> f.getCaseType().name().equals("NOMINATIVE")
                        && f.getNumberType().name().equals("SINGULAR")
                        && f.getFormIast().equals("naraḥ"));
    }

    @Test
    void aStemMerged_neuterLexemeGenderNull_usesActualClass() {
        when(lexemeRepository.findWithMorphologyByCodeIn(eq(List.of("a-stem-masc", "a-stem-neut"))))
                .thenReturn(List.of(lexeme(UUID.fromString("00000000-0000-0000-0000-000000000001"), "phala", "फल", null, "a-stem-neut")));

        DeclensionParadigmPageDto page = service.getParadigmPage("a-stem", 0);

        assertThat(page.getParadigm()).isNotNull();
        assertThat(page.getParadigm().getVowelType()).isEqualTo(VowelType.A_STEM);
        assertThat(page.getParadigm().getGender()).isEqualTo(Gender.NEUTER);
        assertThat(page.getParadigm().getForms())
                .anyMatch(f -> f.getCaseType().name().equals("NOMINATIVE")
                        && f.getNumberType().name().equals("SINGULAR")
                        && f.getFormIast().equals("phalam"));
    }

    @Test
    void aStemMerged_allLexemesBoundBeforeParadigms() {
        when(lexemeRepository.findWithMorphologyByCodeIn(anyCollection()))
                .thenReturn(List.of(
                        lexeme(UUID.fromString("00000000-0000-0000-0000-000000000001"), "nara", "नर", LexemeGender.MASCULINE, "a-stem-masc"),
                        lexeme(UUID.fromString("00000000-0000-0000-0000-000000000002"), "phala", "फल", LexemeGender.NEUTER, "a-stem-neut")));

        DeclensionParadigmPageDto page = service.getParadigmPage("a-stem", 1);

        assertThat(page.getTotalCount()).isEqualTo(2);
        assertThat(page.getParadigm()).isNotNull();
        assertThat(page.getParadigm().getGender()).isEqualTo(Gender.NEUTER);
    }
}