package sm.selflearn.samskrtam.quiz.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Абстракция единицы квиза — составной ключ (itemType, externalRefId).
 *
 * <p>Не материализованная сущность, а значение (value object), передаваемое
 * между компонентами: генератор → модуль рендеринга.
 *
 * <p>Генератор не интерпретирует externalRefId — рендеринг вопроса (текст,
 * варианты ответов, деванагари) вызывается отдельно, по itemType, после того
 * как генератор отобрал список QuizItem для сессии.
 *
 * @see <a href="docs/quizzes/quiz-generator-spec.md#section-2.1">Спецификация §2.1</a>
 */
public final class QuizItem {

    private final ItemType itemType;
    private final UUID externalRefId;

    public QuizItem(ItemType itemType, UUID externalRefId) {
        this.itemType = Objects.requireNonNull(itemType, "itemType must not be null");
        this.externalRefId = Objects.requireNonNull(externalRefId, "externalRefId must not be null");
    }

    public ItemType itemType() { return itemType; }
    public UUID externalRefId() { return externalRefId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuizItem quizItem = (QuizItem) o;
        return itemType == quizItem.itemType && Objects.equals(externalRefId, quizItem.externalRefId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemType, externalRefId);
    }

    @Override
    public String toString() {
        return "QuizItem{" + "itemType=" + itemType + ", externalRefId=" + externalRefId + '}';
    }
}