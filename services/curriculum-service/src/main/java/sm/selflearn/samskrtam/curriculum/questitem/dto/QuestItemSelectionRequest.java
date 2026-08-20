package sm.selflearn.samskrtam.curriculum.questitem.dto;

import sm.selflearn.samskrtam.quest.AnswerMode;

import java.util.List;

/**
 * Request for selecting one quest item per (progress_tag, item_type, answer_mode)
 * group via the window-function endpoint.
 *
 * @param progressTags optional list of tags to filter by; null or empty = all tags
 * @param itemType     optional item type filter (e.g. "DECLENSION_FORM"); null = all types
 * @param answerMode   optional answer mode filter; null = all modes
 * @param limit        max items to return; 0 = no limit
 */
public record QuestItemSelectionRequest(
        List<String> progressTags,
        String itemType,
        AnswerMode answerMode,
        int limit
) {
    public QuestItemSelectionRequest {
        if (progressTags != null && progressTags.isEmpty()) {
            progressTags = null;
        }
    }
}