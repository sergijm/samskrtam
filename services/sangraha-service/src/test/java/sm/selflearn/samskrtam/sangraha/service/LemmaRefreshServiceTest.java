package sm.selflearn.samskrtam.sangraha.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.sangraha.dto.LemmaRefreshResponse;
import sm.selflearn.samskrtam.sangraha.model.Gender;
import sm.selflearn.samskrtam.sangraha.model.Lemma;
import sm.selflearn.samskrtam.sangraha.model.PartOfSpeech;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;
import sm.selflearn.samskrtam.sangraha.model.VerseWordMorphology;
import sm.selflearn.samskrtam.sangraha.repository.LemmaRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LemmaRefreshServiceTest {

    private final Map<String, Lemma> store = new HashMap<>();
    private final List<VerseWord> linkedWords = new ArrayList<>();
    private LemmaRepository lemmaRepo;
    private VerseWordRepository verseWordRepo;

    @BeforeEach
    void setUp() {
        store.clear();
        linkedWords.clear();

        lemmaRepo = mock(LemmaRepository.class);
        when(lemmaRepo.findAll()).thenAnswer(inv -> new ArrayList<>(store.values()));
        when(lemmaRepo.saveAll(any())).thenAnswer(inv -> {
            Iterable<Lemma> lemmas = inv.getArgument(0);
            for (Lemma l : lemmas) {
                if (l.getId() == null) {
                    l.setId(UUID.randomUUID());
                }
                store.put(l.getLemmaSlp1(), l);
            }
            return new ArrayList<>(store.values());
        });

        verseWordRepo = mock(VerseWordRepository.class);
        when(verseWordRepo.saveAll(any())).thenAnswer(inv -> {
            Iterable<VerseWord> words = inv.getArgument(0);
            List<VerseWord> copy = new ArrayList<>();
            words.forEach(copy::add);
            linkedWords.addAll(copy);
            return copy;
        });
    }

    private VerseRepository analyzedRepo(Verse... verses) {
        VerseRepository repo = mock(VerseRepository.class);
        List<Verse> analyzedOnly = java.util.Arrays.stream(verses)
                .filter(v -> v.getStatus() == VerseStatus.ANALYZED)
                .toList();
        when(repo.findAllByStatusAndDeletedAtIsNullAndIdGreaterThan(
                eq(VerseStatus.ANALYZED), any(), any()))
                .thenReturn(analyzedOnly, List.of());
        return repo;
    }

    private Verse verse(VerseStatus status, VerseWord... words) {
        Verse v = new Verse();
        v.setId(UUID.randomUUID());
        v.setStatus(status);
        v.setVerseWords(new ArrayList<>(List.of(words)));
        for (VerseWord w : words) {
            w.setVerse(v);
        }
        return v;
    }

    private VerseWord word(String lemmaIast, PartOfSpeech pos, Gender gender) {
        VerseWord w = new VerseWord();
        w.setId(UUID.randomUUID());
        w.setLemmaIast(lemmaIast);
        w.setPos(pos);
        w.setSurfaceIast(lemmaIast);
        w.setSurfaceDevanagari(lemmaIast);
        if (gender != null) {
            VerseWordMorphology m = new VerseWordMorphology();
            m.setGender(gender);
            w.setMorphology(m);
        }
        return w;
    }

    private LemmaRefreshService service(VerseRepository verseRepo) {
        return new LemmaRefreshService(verseRepo, verseWordRepo, lemmaRepo, new TransliterationService());
    }

    // ------------------------------------------------------------

    @Test
    void refresh_groupsByLemmaSlp1AndGender() {
        LemmaRefreshResponse response = service(analyzedRepo(verse(
                VerseStatus.ANALYZED,
                word("gaja", PartOfSpeech.NOUN, Gender.MASCULINE),
                word("gaja", PartOfSpeech.NOUN, Gender.MASCULINE),
                word("gaja", PartOfSpeech.NOUN, Gender.FEMININE),
                word("nadi", PartOfSpeech.NOUN, Gender.FEMININE)))).refresh();

        assertThat(response.lemmaCount()).isEqualTo(2);
        assertThat(response.newLemmaCount()).isEqualTo(2);

        Lemma gaja = store.get("gaja");
        assertThat(gaja).isNotNull();
        assertThat(gaja.getOccurrenceCount()).isEqualTo(3);
        assertThat(gaja.getGender()).isEqualTo("MASCULINE");
        assertThat(gaja.getLemmaDevanagari()).isNotBlank();

        Lemma nadi = store.get("nadi");
        assertThat(nadi).isNotNull();
        assertThat(nadi.getOccurrenceCount()).isEqualTo(1);
        assertThat(nadi.getGender()).isEqualTo("FEMININE");
    }

    @Test
    void refresh_linksLemmaIdToAllWordsOfGroup() {
        VerseRepository repo = analyzedRepo(verse(
                VerseStatus.ANALYZED,
                word("gaja", PartOfSpeech.NOUN, Gender.MASCULINE),
                word("gaja", PartOfSpeech.NOUN, Gender.FEMININE),
                word("nadi", PartOfSpeech.NOUN, Gender.FEMININE)));

        service(repo).refresh();

        assertThat(linkedWords).hasSize(3);
        assertThat(linkedWords).allSatisfy(w -> assertThat(w.getLemmaId()).isNotNull());
        Lemma gaja = store.get("gaja");
        for (VerseWord w : linkedWords) {
            if ("gaja".equals(w.getLemmaIast())) {
                assertThat(w.getLemmaId()).isEqualTo(gaja.getId());
            }
        }
    }

    @Test
    void refresh_dominantPosIsModaWithAlphaTieBreak() {
        LemmaRefreshResponse response = service(analyzedRepo(verse(
                VerseStatus.ANALYZED,
                word("rama", PartOfSpeech.NOUN, Gender.MASCULINE),
                word("rama", PartOfSpeech.NOUN, Gender.MASCULINE),
                word("rama", PartOfSpeech.ADJECTIVE, Gender.MASCULINE)))).refresh();

        assertThat(response.lemmaCount()).isEqualTo(1);
        assertThat(store.get("rama").getDominantPosCode()).isEqualTo("NOUN");
    }

    @Test
    void refresh_frequencyRankDeterministicByCountThenAlpha() {
        service(analyzedRepo(verse(
                VerseStatus.ANALYZED,
                word("gaja", PartOfSpeech.NOUN, Gender.MASCULINE),
                word("gaja", PartOfSpeech.NOUN, Gender.MASCULINE),
                word("nadi", PartOfSpeech.NOUN, Gender.FEMININE)))).refresh();

        assertThat(store.get("gaja").getFrequencyRank()).isEqualTo(1);
        assertThat(store.get("nadi").getFrequencyRank()).isEqualTo(2);
    }

    @Test
    void refresh_secondRunIsIdempotent_newCountZero() {
        VerseRepository repo = analyzedRepo(verse(
                VerseStatus.ANALYZED,
                word("gaja", PartOfSpeech.NOUN, Gender.MASCULINE)));
        LemmaRefreshService service = service(repo);

        LemmaRefreshResponse first = service.refresh();
        LemmaRefreshResponse second = service.refresh();

        assertThat(first.newLemmaCount()).isEqualTo(1);
        assertThat(second.newLemmaCount()).isZero();
        assertThat(second.lemmaCount()).isEqualTo(1);
        assertThat(store).hasSize(1);
    }

    @Test
    void refresh_ignoresNonAnalyzedVerses() {
        LemmaRefreshResponse response = service(analyzedRepo(
                verse(VerseStatus.ANALYZED,
                        word("gaja", PartOfSpeech.NOUN, Gender.MASCULINE)),
                verse(VerseStatus.DRAFT,
                        word("ashva", PartOfSpeech.NOUN, Gender.MASCULINE)))).refresh();

        assertThat(response.lemmaCount()).isEqualTo(1);
        assertThat(store.keySet()).containsExactly("gaja");
    }
}