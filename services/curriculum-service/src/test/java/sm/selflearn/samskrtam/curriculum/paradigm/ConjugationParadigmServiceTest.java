package sm.selflearn.samskrtam.curriculum.paradigm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.content.dto.ConjugationParadigmPageDto;
import sm.selflearn.samskrtam.content.model.NumberType;
import sm.selflearn.samskrtam.content.model.Voice;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConjugationParadigmServiceTest {

    private ConjugationFormRepository conjugationFormRepository;
    private ConjugationParadigmService service;

    @BeforeEach
    void setUp() {
        conjugationFormRepository = mock(ConjugationFormRepository.class);
        service = new ConjugationParadigmService(conjugationFormRepository);
    }

    private ConjugationForm form(String lemma, Voice voice, int person, NumberType number, String iast) {
        ConjugationForm f = new ConjugationForm();
        f.setTopicCode("presence-indicativus");
        f.setLemmaIast(lemma);
        f.setLemmaDevanagari("भू");
        f.setMeaningRu("быть, становиться");
        f.setVoice(voice);
        f.setPerson(person);
        f.setNumberType(number);
        f.setSentenceIast(iast);
        f.setSentenceDevanagari("अहम् सुखी भवामि।");
        f.setTranslationRu("Я становлюсь счастливым.");
        return f;
    }

    @Test
    void emptyTopic_returnsEmptyPage() {
        when(conjugationFormRepository
                .findByTopicCodeOrderByLemmaIastAscVoiceAscPersonDescNumberTypeAsc("presence-indicativus"))
                .thenReturn(List.of());

        ConjugationParadigmPageDto page = service.getParadigmPage("presence-indicativus", 0, null);

        assertThat(page.getTotalCount()).isZero();
        assertThat(page.getParadigm()).isNull();
    }

    @Test
    void groupsByLemma_andServesOneVerbPerPage() {
        ConjugationForm bhūSg = form("bhū", Voice.PARASMAIPADA, 1, NumberType.SINGULAR, "bhavāmi.");
        ConjugationForm bhūPl = form("bhū", Voice.PARASMAIPADA, 1, NumberType.PLURAL, "bhavāmaḥ.");
        ConjugationForm paṭhSg = form("paṭh", Voice.PARASMAIPADA, 3, NumberType.SINGULAR, "paṭhati.");
        when(conjugationFormRepository
                .findByTopicCodeOrderByLemmaIastAscVoiceAscPersonDescNumberTypeAsc("presence-indicativus"))
                .thenReturn(List.of(bhūSg, bhūPl, paṭhSg));

        ConjugationParadigmPageDto page0 = service.getParadigmPage("presence-indicativus", 0, null);
        ConjugationParadigmPageDto page1 = service.getParadigmPage("presence-indicativus", 1, null);

        assertThat(page0.getTotalCount()).isEqualTo(2);
        assertThat(page0.getParadigm().getLemmaIast()).isEqualTo("bhū");
        assertThat(page0.getParadigm().getVoice()).isEqualTo(Voice.PARASMAIPADA);
        assertThat(page0.getParadigm().getForms()).hasSize(2);
        assertThat(page1.getParadigm().getLemmaIast()).isEqualTo("paṭh");
    }

    @Test
    void voiceFilter_limitsCarousel() {
        ConjugationForm labhSg = form("labh", Voice.ATMANEPADA, 1, NumberType.SINGULAR, "labhe.");
        when(conjugationFormRepository
                .findByTopicCodeAndVoiceOrderByLemmaIastAscPersonDescNumberTypeAsc(
                        "presence-indicativus", Voice.ATMANEPADA))
                .thenReturn(List.of(labhSg));

        ConjugationParadigmPageDto page = service.getParadigmPage("presence-indicativus", 0, Voice.ATMANEPADA);

        assertThat(page.getTotalCount()).isEqualTo(1);
        assertThat(page.getParadigm().getVoice()).isEqualTo(Voice.ATMANEPADA);
    }

    @Test
    void indexOutOfRange_returnsEmptyPageWithTotalCount() {
        when(conjugationFormRepository
                .findByTopicCodeOrderByLemmaIastAscVoiceAscPersonDescNumberTypeAsc("presence-indicativus"))
                .thenReturn(List.of(form("bhū", Voice.PARASMAIPADA, 1, NumberType.SINGULAR, "bhavāmi.")));

        ConjugationParadigmPageDto page = service.getParadigmPage("presence-indicativus", 5, null);

        assertThat(page.getTotalCount()).isEqualTo(1);
        assertThat(page.getParadigm()).isNull();
    }
}