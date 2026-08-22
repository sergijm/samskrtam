package sm.selflearn.samskrtam.curriculum.lexicon.imports;

import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LemmaLexicalTopic;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LemmaLexicalTopicId;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LemmaTranslation;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LemmaLexicalTopicRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LemmaTranslationRepository;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.model.TopicDomainType;
import sm.selflearn.samskrtam.curriculum.questgen.LexicalQuizItemGenerator;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Приём инкрементальной пачки стиха (lexicon-content-pipeline.md §7): пишем
 * переводы в lemma_translation и привязки в lemma_lexical_topic (без Lexeme),
 * создаём VERSE-урок главы и накапливаем привязки идемпотентно.
 */
class VerseLexemeImportServiceTest {

    private final List<LemmaTranslation> savedTranslations = new ArrayList<>();
    private final Map<String, List<LemmaTranslation>> byLemmaLang = new java.util.HashMap<>();
    private final List<LemmaLexicalTopic> savedBindings = new ArrayList<>();
    private final List<LemmaLexicalTopicId> existingBindings = new ArrayList<>();
    private Topic createdTopic;

    private VerseLexemeImportService service() {
        LemmaTranslationRepository translationRepo = mock(LemmaTranslationRepository.class);
        when(translationRepo.findByLemmaIastAndLanguage(any(), any()))
                .thenAnswer(inv -> byLemmaLang.getOrDefault(
                        inv.getArgument(0) + "|" + inv.getArgument(1), List.of()));
        when(translationRepo.save(any())).thenAnswer(inv -> {
            LemmaTranslation t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(UUID.randomUUID());
            }
            savedTranslations.add(t);
            byLemmaLang.computeIfAbsent(t.getLemmaIast() + "|" + t.getLanguage(),
                    x -> new ArrayList<>()).add(t);
            return t;
        });

        TopicRepository topicRepo = mock(TopicRepository.class);
        when(topicRepo.findByCode(any())).thenAnswer(inv ->
                Optional.ofNullable(createdTopic != null && createdTopic.getCode().equals(inv.getArgument(0))
                        ? createdTopic : null));
        when(topicRepo.save(any())).thenAnswer(inv -> {
            createdTopic = inv.getArgument(0);
            return createdTopic;
        });

        LemmaLexicalTopicRepository bindingRepo = mock(LemmaLexicalTopicRepository.class);
        when(bindingRepo.existsById(any())).thenAnswer(inv -> existingBindings.contains(inv.getArgument(0)));
        when(bindingRepo.save(any())).thenAnswer(inv -> {
            LemmaLexicalTopic b = inv.getArgument(0);
            savedBindings.add(b);
            existingBindings.add(b.getId());
            return b;
        });

        return new VerseLexemeImportService(
                translationRepo,
                bindingRepo,
                topicRepo,
                mock(QuestItemRepository.class),
                mock(LexicalQuizItemGenerator.class));
    }

    private static VerseLemmaBatchRequest batch(String workSlp1, int chapter,
                                                List<LemmaExportItem> words) {
        return new VerseLemmaBatchRequest(UUID.randomUUID(), null, workSlp1, workSlp1, chapter,
                "ch" + chapter, "title", "title", words);
    }

    private static LemmaExportItem word(String slp1, String iast, String gender, String glossRu, String glossEn) {
        return new LemmaExportItem(null, slp1, iast, "देव", gender, "NOUN", 1, List.of(), glossRu, glossEn, null);
    }

    private static String langKey(String lemmaIast, String language) {
        return lemmaIast + "|" + language;
    }

    @Test
    void importVerseBatch_newWords_createsTranslationsAndVerseTopic() {
        VerseLexemeImportService service = service();

        VerseBatchImportResult result = service.importVerseBatch(batch(
                "bhagavad_gita", 1,
                List.of(word("nara", "nara", "MASCULINE", "мужчина", "man"),
                        word("gaja", "gaja", "MASCULINE", "слон", "elephant"))));

        // imported counts lemmas (ru+en written for each new lemma)
        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.updatedCount()).isZero();

        assertThat(savedTranslations).hasSize(4); // 2 lemmas * (ru + en)
        assertThat(savedTranslations).filteredOn(t -> t.getLanguage().equals("ru"))
                .extracting(LemmaTranslation::getGloss)
                .containsExactlyInAnyOrder("мужчина", "слон");
        assertThat(savedTranslations).allMatch(LemmaTranslation::isMain);

        assertThat(createdTopic).isNotNull();
        assertThat(createdTopic.getCode()).isEqualTo("bhagavad_gita_1");
        assertThat(createdTopic.getDomain()).isEqualTo(TopicDomain.VERSE);
        assertThat(createdTopic.getDomainType()).isEqualTo(TopicDomainType.VERSE);
        assertThat(createdTopic.getLearningLevel()).isEqualTo(LearningLevel.L0);
        assertThat(savedBindings).hasSize(2);
        assertThat(savedBindings).extracting(b -> b.getId().getLemmaIast())
                .containsExactlyInAnyOrder("nara", "gaja");
    }

    @Test
    void importVerseBatch_resendSameVerse_idempotent_noDuplicates() {
        VerseLexemeImportService service = service();

        service.importVerseBatch(batch("w", 1, List.of(word("nara", "nara", "MASCULINE", "мужчина", "man"))));
        VerseBatchImportResult second = service.importVerseBatch(batch("w", 1,
                List.of(word("nara", "nara", "MASCULINE", "мужчина", "man"))));

        assertThat(second.importedCount()).isZero();
        assertThat(savedTranslations).hasSize(2); // ru + en, not duplicated
        assertThat(savedBindings).hasSize(1);
        assertThat(createdTopic.getCode()).isEqualTo("w_1");
    }

    @Test
    void importVerseBatch_differentGloss_sameLemma_stillIdempotent() {
        VerseLexemeImportService service = service();

        service.importVerseBatch(batch("w", 1, List.of(word("gram", "gram", "MASCULINE", "деревня", "village"))));
        VerseBatchImportResult second = service.importVerseBatch(batch("w", 1,
                List.of(word("gram", "gram", "MASCULINE", "ГРАММ", "gram-measure"))));

        // first gloss wins; the translation is not overwritten / duplicated
        assertThat(second.importedCount()).isZero();
        assertThat(savedTranslations).hasSize(2);
        assertThat(savedTranslations).filteredOn(t -> t.getLanguage().equals("ru"))
                .extracting(LemmaTranslation::getGloss)
                .containsExactly("деревня");
    }

    @Test
    void importVerseBatch_secondVerseOfSameChapter_accumulatesBindings() {
        VerseLexemeImportService service = service();

        service.importVerseBatch(batch("w", 1, List.of(word("nara", "nara", "MASCULINE", "мужчина", "man"))));
        service.importVerseBatch(batch("w", 1, List.of(word("gaja", "gaja", "MASCULINE", "слон", "elephant"))));

        assertThat(createdTopic.getCode()).isEqualTo("w_1");
        assertThat(savedBindings).hasSize(2);
        assertThat(savedBindings).extracting(b -> b.getId().getTopicCode())
                .containsOnly("w_1");
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
