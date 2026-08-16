package sm.selflearn.samskrtam.curriculum.lexicon.imports;

import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeLexicalTopic;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeLexicalTopicId;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeLexicalTopicRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.MorphologyClassRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.PartOfSpeechRepository;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.model.TopicDomainType;
import sm.selflearn.samskrtam.curriculum.questgen.LexicalQuizItemGenerator;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Приём инкрементальной пачки стиха (lexicon-content-pipeline.md §7):
 * meaningNumber = max+1 для новых значений, идемпотентность повторной пачки,
 * создание VERSE-урока главы и накопление привязок.
 */
class VerseLexemeImportServiceTest {

    private final List<Lexeme> savedLexemes = new ArrayList<>();
    private final Map<String, List<Lexeme>> byKey = new HashMap<>();
    private final Map<String, Integer> maxMeaning = new HashMap<>();
    private Topic createdTopic;
    private final List<LexemeLexicalTopic> savedBindings = new ArrayList<>();
    private final List<LexemeLexicalTopicId> existingBindings = new ArrayList<>();

    private VerseLexemeImportService service() {
        LexemeRepository lexemeRepo = mock(LexemeRepository.class);
        when(lexemeRepo.findByLemmaSlp1AndGenderOrderByMeaningNumberAsc(any(), any()))
                .thenAnswer(inv -> byKey.getOrDefault(key(inv.getArgument(0), inv.getArgument(1)), List.of()));
        when(lexemeRepo.findMaxMeaningNumber(any())).thenAnswer(inv ->
                maxMeaning.getOrDefault(inv.getArgument(0), 0));
        when(lexemeRepo.save(any())).thenAnswer(inv -> {
            Lexeme l = inv.getArgument(0);
            if (l.getId() == null) {
                l.setId(UUID.randomUUID());
            }
            savedLexemes.add(l);
            String k = key(l.getLemmaSlp1(), l.getGender());
            byKey.computeIfAbsent(k, x -> new ArrayList<>()).add(l);
            maxMeaning.merge(l.getLemmaSlp1(), l.getMeaningNumber(), Math::max);
            return l;
        });

        TopicRepository topicRepo = mock(TopicRepository.class);
        when(topicRepo.findByCode(any())).thenAnswer(inv ->
                Optional.ofNullable(createdTopic != null && createdTopic.getCode().equals(inv.getArgument(0))
                        ? createdTopic : null));
        when(topicRepo.save(any())).thenAnswer(inv -> {
            createdTopic = inv.getArgument(0);
            return createdTopic;
        });

        LexemeLexicalTopicRepository bindingRepo = mock(LexemeLexicalTopicRepository.class);
        when(bindingRepo.existsById(any())).thenAnswer(inv -> existingBindings.contains(inv.getArgument(0)));
        when(bindingRepo.saveAll(any())).thenAnswer(inv -> {
            List<LexemeLexicalTopic> list = inv.getArgument(0);
            savedBindings.addAll(list);
            list.forEach(b -> existingBindings.add(b.getId()));
            return list;
        });

        return new VerseLexemeImportService(
                lexemeRepo,
                mock(PartOfSpeechRepository.class),
                mock(MorphologyClassRepository.class),
                topicRepo,
                bindingRepo,
                mock(QuestItemRepository.class),
                mock(LexicalQuizItemGenerator.class));
    }

    private static String key(String slp1, LexemeGender gender) {
        return slp1 + "|" + (gender == null ? "" : gender.name());
    }

    private static VerseLemmaBatchRequest batch(String workSlp1, int chapter,
                                                List<LemmaExportItem> words) {
        return new VerseLemmaBatchRequest(UUID.randomUUID(), null, workSlp1, workSlp1, chapter,
                "ch" + chapter, "title", "title", words);
    }

    private static LemmaExportItem word(String slp1, String iast, String gender, String glossRu, String glossEn) {
        return new LemmaExportItem(null, slp1, iast, "देव", gender, "NOUN", 1, List.of(), glossRu, glossEn, null);
    }

    @Test
    void importVerseBatch_newWords_createsLexemesAndVerseTopic() {
        VerseLexemeImportService service = service();

        VerseBatchImportResult result = service.importVerseBatch(batch(
                "bhagavad_gita", 1,
                List.of(word("nara", "nara", "MASCULINE", "мужчина", "man"),
                        word("gaja", "gaja", "MASCULINE", "слон", "elephant"))));

        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.updatedCount()).isZero();
        assertThat(savedLexemes).extracting(Lexeme::getMeaningNumber).containsExactly(1, 1);

        assertThat(createdTopic).isNotNull();
        assertThat(createdTopic.getCode()).isEqualTo("bhagavad_gita_1");
        assertThat(createdTopic.getDomain()).isEqualTo(TopicDomain.VERSE);
        assertThat(createdTopic.getDomainType()).isEqualTo(TopicDomainType.VERSE);
        assertThat(createdTopic.getLearningLevel()).isEqualTo(LearningLevel.L0);
        assertThat(savedBindings).hasSize(2);
    }

    @Test
    void importVerseBatch_differentGloss_sameLemma_getsNextMeaningNumber() {
        VerseLexemeImportService service = service();

        service.importVerseBatch(batch("w", 1, List.of(word("gram", "gram", "MASCULINE", "деревня", "village"))));
        VerseBatchImportResult second = service.importVerseBatch(batch("w", 1,
                List.of(word("gram", "gram", "MASCULINE", "ГРАММ", "gram-measure"))));

        assertThat(second.importedCount()).isEqualTo(1);
        assertThat(savedLexemes).hasSize(2);
        assertThat(savedLexemes).extracting(Lexeme::getMeaningNumber).containsExactly(1, 2);
        // Разные значения одного написания — не склеиваются (§7, lexicon.md §1).
        assertThat(savedLexemes).extracting(Lexeme::getGlossRu)
                .containsExactly("деревня", "ГРАММ");
    }

    @Test
    void importVerseBatch_resendSameVerse_idempotent_noDuplicates() {
        VerseLexemeImportService service = service();

        service.importVerseBatch(batch("w", 1, List.of(word("nara", "nara", "MASCULINE", "мужчина", "man"))));
        VerseBatchImportResult second = service.importVerseBatch(batch("w", 1,
                List.of(word("nara", "nara", "MASCULINE", "мужчина", "man"))));

        assertThat(second.importedCount()).isZero();
        assertThat(savedLexemes).hasSize(1);
        assertThat(savedBindings).hasSize(1);
        assertThat(createdTopic.getCode()).isEqualTo("w_1");
    }

    @Test
    void importVerseBatch_secondVerseOfSameChapter_accumulatesBindings() {
        VerseLexemeImportService service = service();

        service.importVerseBatch(batch("w", 1, List.of(word("nara", "nara", "MASCULINE", "мужчина", "man"))));
        service.importVerseBatch(batch("w", 1, List.of(word("gaja", "gaja", "MASCULINE", "слон", "elephant"))));

        assertThat(createdTopic.getCode()).isEqualTo("w_1");
        assertThat(savedBindings).hasSize(2);
        // Тот же урок главы, разные лексемы двух стихов.
        assertThat(savedBindings).extracting(b -> b.getId().getLexicalTopicId())
                .containsOnly(createdTopic.getId());
    }

    @Test
    void importVerseBatch_standalone_createsPerUserTopic() {
        VerseLexemeImportService service = service();
        UUID ownerId = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

        VerseBatchImportResult result = service.importVerseBatch(new VerseLemmaBatchRequest(
                UUID.randomUUID(), ownerId, null, null, 0, null, null, null,
                List.of(word("nara", "nara", "MASCULINE", "мужчина", "man"))));

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(createdTopic.getCode()).isEqualTo("user-" + ownerId);
        assertThat(createdTopic.getDomain()).isEqualTo(TopicDomain.VERSE);
        assertThat(createdTopic.getTitleRu()).isEqualTo("Мои слова");
    }
}