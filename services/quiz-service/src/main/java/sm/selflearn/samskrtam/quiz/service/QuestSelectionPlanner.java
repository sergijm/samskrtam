package sm.selflearn.samskrtam.quiz.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Merges per-item-type progress selections into a single interleaved list limited to
 * {@code limit} entries, mixing types round-robin. Pure and unit-testable.
 */
public final class QuestSelectionPlanner {

    private QuestSelectionPlanner() {
    }

    /**
     * @param groups selections per item type (already progress-ranked, e.g. due-first)
     * @param limit  maximum number of items to keep
     * @return interleaved items, at most {@code limit}; empty when nothing selected
     */
    public static <T> List<T> takeRoundRobin(List<? extends List<T>> groups, int limit) {
        List<T> result = new ArrayList<>();
        if (limit <= 0 || groups == null || groups.isEmpty()) {
            return result;
        }
        boolean progressed = true;
        int index = 0;
        while (progressed && result.size() < limit) {
            progressed = false;
            for (List<T> group : groups) {
                if (index < group.size()) {
                    result.add(group.get(index));
                    progressed = true;
                    if (result.size() >= limit) {
                        break;
                    }
                }
            }
            index++;
        }
        return result;
    }
}