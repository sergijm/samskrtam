package sm.selflearn.samskrtam.quest.declension;

import sm.selflearn.samskrtam.quest.HighlightToken;
import sm.selflearn.samskrtam.quest.QuestItemPayload;

import java.util.List;

/**
 * Payload for DECLENSION_MATCH: word forms of one lexeme <-> case+number labels.
 * See curriculum-quest-items.md §2.4.
 */
public record DeclensionMatchPayload(
        String lemmaIast,
        String morphologyClassCode,
        List<DeclensionMatchPair> pairs,
        List<HighlightToken> highlights
) implements QuestItemPayload {

    public record DeclensionMatchPair(
            String pairId,
            String wordFormIast,
            String wordFormDevanagari,
            String caseType,
            String numberType
    ) {
    }
}