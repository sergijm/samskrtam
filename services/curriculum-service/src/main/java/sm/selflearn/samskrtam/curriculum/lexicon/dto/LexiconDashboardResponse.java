package sm.selflearn.samskrtam.curriculum.lexicon.dto;

import java.util.List;

/**
 * Лендинг раздела «Лексика» на новой модели (lemma_translation +
 * lemma_lexical_topic). Контракт совместим со старым фронтендом
 * (src/types/lexicon.ts): прогресс пользователя и пользовательские коллекции
 * удалены вместе с таблицей lexeme (V41), поэтому masteredCount/collections/
 * quickStart/блок «сегодня» отдаются пустыми/нулевыми.
 */
public record LexiconDashboardResponse(
        LexiconProgressSummary summary,
        LexiconToday today,
        List<FrequencyBand> frequencyBands,
        List<LexicalTopic> topics,
        List<SemanticTopic> semanticTopics,
        List<LexiconPos> pos,
        List<UserCollection> collections,
        List<QuickStartPreset> quickStart) {

    public record LexiconProgressSummary(long totalWords, long masteredCount) {
    }

    public record LexiconToday(long reviewDue, long newWords, long weakWords) {
    }

    public record FrequencyBand(String id, int from, int to, long wordCount, long masteredCount) {
    }

    public record LexicalTopic(String id, String nameRu, String nameEn, long wordCount, long masteredCount) {
    }

    public record SemanticTopic(String id, String nameRu, String nameEn, long wordCount, long masteredCount) {
    }

    public record LexiconPos(String id, String nameRu, String nameEn, long wordCount) {
    }

    public record UserCollection(String id, String name, long wordCount) {
    }

    public record QuickStartPreset(String id, String titleRu, String titleEn, String metaRu, String metaEn) {
    }
}
