# Quest Item Model — базовые Java-абстракции

> Связанные файлы: [quest-engine.md](./quest-engine.md) · [quest-catalog.md](curriculum-service/quest-catalog.md) · [curriculum-service.md](./curriculum-service.md)

Базовые интерфейсы и абстрактные классы для модели квестов — общие для curriculum-service (генерация) и quiz-service (прохождение, прогресс). Цель: добавление нового типа квеста (см. [quest-catalog.md](curriculum-service/quest-catalog.md)) не требует изменений в quiz-service и в алгоритме повторения — только новая реализация `QuestItemGenerator`/`AnswerChecker` в curriculum-service.

Пакеты: `sm.selflearn.samskrtam.content.quest.*` (генерация, только curriculum-service), `sm.selflearn.samskrtam.quest.*` (общая модель, живёт в `shared/samskrtam-dtos` — доступна curriculum-service, curriculum-service и quiz-service), `sm.selflearn.samskrtam.quiz.progress.*` (прохождение и прогресс, только quiz-service).

> **Версионирование и новое семейство типов (declension, §3):** curriculum-service (API v1) не
> трогается и продолжает обслуживать существующие типы как есть. Генерация и хранение
> `DECLENSION_FORM`/`DECLENSION_FORM_CHOICE`/`CASE_RECOGNITION`/`DECLENSION_MATCH` переносится
> в curriculum-service (API v2) — новый сервис, новый префикс пути, без изменений в
> curriculum-service. Реализация в curriculum-service не «генерирует по запросу» через
> `QuestItemGenerator.generate(ctx)`, а материализует заранее большой фиксированный набор
> `QuestItem` в БД (batch-генератор), а `GET`-эндпоинт curriculum-service просто отбирает
> нужное количество готовых строк по `topicId`/`itemType`. Контракт `QuestItem`/`QuestItemType`/`AnswerMode` (этот файл) один и тот же для обоих подходов — quiz-service не видит разницы. Подробности — [curriculum-quest-items.md](./curriculum-quest-items.md).

---

## 1. Общая модель (`shared/samskrtam-dtos`, `sm.selflearn.samskrtam.quest`)

Открытый реестр типов вместо enum — новый тип регистрируется без изменения существующего кода (Open/Closed).

```java
/** Идентификатор типа квеста. Реализации — константы в каждом домене (см. §3). */
public interface QuestItemType {
    String code();          // "DECLENSION_FORM", "CONJUGATION_FORM", "VOCABULARY_SYNONYM" ...
    QuestDomain domain();    // MORPHOLOGY | PHONOLOGY | SYNTAX | LEXICON | PROSODY
    AnswerMode defaultAnswerMode();
}

public enum QuestDomain { MORPHOLOGY, PHONOLOGY, SYNTAX, LEXICON, PROSODY }

/** Как принимается и проверяется ответ. Одно значение может обслуживать много типов. */
public enum AnswerMode {
    FREE_TEXT,       // ввод словоформы/перевода
    SINGLE_CHOICE,   // выбор одного варианта из distractors
    MULTI_SELECT,    // выбор нескольких (напр. все члены сложного слова)
    SPAN_SELECT,     // выделение части текста (напр. границы сандхи в строке)
    MATCHING         // соединение пар из двух списков (напр. словоформа ↔ падеж/число)
}

/** Маркерный интерфейс данных, специфичных для конкретного типа — реализуется record'ом на тип. */
public interface QuestItemPayload { }

/**
 * Единица задания. Один класс на все типы — различие только в payload.
 * Иммутабельна, содержимое формирует curriculum-service.
 */
public final class QuestItem {
    private final UUID id;
    private final UUID questId;
    private final QuestItemType type;
    private final String prompt;              // что показываем пользователю
    private final AnswerMode answerMode;
    private final List<String> distractors;   // пусто для FREE_TEXT и MATCHING
    private final QuestItemPayload payload;    // тип-специфичные данные, для AnswerChecker

    // конструктор, геттеры — без сеттеров, объект собирается генератором целиком
}

/** Именованный набор QuestItem одной темы (было Lesson/Quiz). */
public final class Quest {
    private final UUID id;
    private final String slug;
    private final QuestItemType itemType;   // все items одного Quest — одного типа
    private final String titleRu;
    private final String titleEn;
}
```

**Контракт `AnswerMode.MATCHING`:** `distractors` не используется (пусто) — вариантов для
подбора столько же, сколько пар, и все они «правильные» для какой-то из пар. Список пар
(левая часть — словоформа, правая — падеж+число или иной атрибут) хранится в `payload`
конкретного типа (пример: `DeclensionMatchPayload`, см. [curriculum-quest-items.md §3](curriculum-service/curriculum-quest-items.md#3-типы-payload)). `userAnswer` в этом режиме —
сериализованный список сопоставлений (`{leftId: rightId, ...}`), `AnswerChecker` считает
ответ верным только при полном совпадении всех пар (без частичного зачёта — упрощение
первой версии).

---

## 2. curriculum-service — генерация (`sm.selflearn.samskrtam.content.quest`)

```java
/** Контекст генерации: что известно на момент запроса (пользователь, Quest, опциональные фильтры). */
public record GenerationContext(UUID questId, UUID userId, Map<String, String> params) { }

/**
 * Генерирует QuestItem для одного Quest. Одна реализация на QuestItemType.
 * Реестр реализаций — Spring-бины, инжектятся как Map<String, QuestItemGenerator<?>>
 * (ключ — QuestItemType.code()), без switch по типу в вызывающем коде.
 */
public interface QuestItemGenerator<P extends QuestItemPayload> {
    QuestItemType type();
    List<QuestItem> generate(GenerationContext ctx);
}

/** Базовая реализация с шаблонным методом — конкретные генераторы переопределяют только buildPayload/buildPrompt. */
public abstract class AbstractQuestItemGenerator<P extends QuestItemPayload>
        implements QuestItemGenerator<P> {

    @Override
    public final List<QuestItem> generate(GenerationContext ctx) {
        return sourceEntities(ctx).stream()
                .map(entity -> buildItem(ctx, entity))
                .toList();
    }

    /** Откуда брать данные для генерации — конкретная выборка из БД curriculum-service. */
    protected abstract List<?> sourceEntities(GenerationContext ctx);

    /** Собрать один QuestItem из одной сущности-источника. */
    protected abstract QuestItem buildItem(GenerationContext ctx, Object entity);
}

/**
 * Проверка ответа — отдельно от генерации: генератор мог отдать item заранее (кешируется),
 * а проверка выполняется на каждый Attempt заново, без повторного обращения к БД.
 */
public interface AnswerChecker<P extends QuestItemPayload> {
    QuestItemType type();
    CheckResult check(P payload, String userAnswer);
}

public record CheckResult(boolean correct, String correctAnswer, String explanation) { }
```

**Пример реализации (declension, уже существующий тип):**

```java
public record DeclensionPayload(
        String stemIast, VowelType vowelType, Gender gender,
        CaseType caseType, NumberType numberType, String correctEnding
) implements QuestItemPayload { }

@Component
public class DeclensionQuestItemGenerator
        extends AbstractQuestItemGenerator<DeclensionPayload> {

    @Override public QuestItemType type() { return GrammarQuestItemTypes.DECLENSION_FORM; }

    @Override
    protected List<?> sourceEntities(GenerationContext ctx) {
        return declensionStemRepository.findByQuestId(ctx.questId());
    }

    @Override
    protected QuestItem buildItem(GenerationContext ctx, Object entity) {
        DeclensionStem stem = (DeclensionStem) entity;
        // строит prompt/distractors/payload — деталь конкретного типа,
        // не влияет на контракт QuestItem
        ...
    }
}
```

---

## 3. Реестр типов по доменам

Каждый домен (см. [quest-catalog.md §1](curriculum-service/quest-catalog.md#1-домены)) — отдельный класс-держатель констант `QuestItemType`, без общего enum:

```java
public final class GrammarQuestItemTypes {
    public static final QuestItemType DECLENSION_FORM = QuestItemTypes.of(
            "DECLENSION_FORM", QuestDomain.MORPHOLOGY, AnswerMode.FREE_TEXT);
    /** Тип 1 задания (лемма → выбрать словоформу из вариантов) — тот же payload,
     *  что и DECLENSION_FORM, отдельный AnswerMode на уровне QuestItem, см. curriculum-quest-items.md §2.1. */
    public static final QuestItemType DECLENSION_FORM_CHOICE = QuestItemTypes.of(
            "DECLENSION_FORM_CHOICE", QuestDomain.MORPHOLOGY, AnswerMode.SINGLE_CHOICE);
    /** Тип 3 задания (словоформа → определить падеж[, число, [род]]), см. curriculum-quest-items.md §2.3. */
    public static final QuestItemType CASE_RECOGNITION = QuestItemTypes.of(
            "CASE_RECOGNITION", QuestDomain.MORPHOLOGY, AnswerMode.SINGLE_CHOICE);
    /** Тип 4 задания (список словоформ ↔ список падеж+число), см. curriculum-quest-items.md §2.4. */
    public static final QuestItemType DECLENSION_MATCH = QuestItemTypes.of(
            "DECLENSION_MATCH", QuestDomain.MORPHOLOGY, AnswerMode.MATCHING);
    public static final QuestItemType NUMERAL_FORM = QuestItemTypes.of(
            "NUMERAL_FORM", QuestDomain.MORPHOLOGY, AnswerMode.FREE_TEXT);
    public static final QuestItemType CONJUGATION_FORM = QuestItemTypes.of(
            "CONJUGATION_FORM", QuestDomain.MORPHOLOGY, AnswerMode.FREE_TEXT);
    public static final QuestItemType PARTICIPLE_FORM = QuestItemTypes.of(
            "PARTICIPLE_FORM", QuestDomain.MORPHOLOGY, AnswerMode.FREE_TEXT);
    public static final QuestItemType ABSOLUTIVE_FORM = QuestItemTypes.of(
            "ABSOLUTIVE_FORM", QuestDomain.MORPHOLOGY, AnswerMode.FREE_TEXT);
}

public final class PhonologyQuestItemTypes {
    public static final QuestItemType SANDHI_SPLIT = QuestItemTypes.of(
            "SANDHI_SPLIT", QuestDomain.PHONOLOGY, AnswerMode.FREE_TEXT);
    public static final QuestItemType SANDHI_JOIN = QuestItemTypes.of(
            "SANDHI_JOIN", QuestDomain.PHONOLOGY, AnswerMode.SINGLE_CHOICE);
}

public final class SyntaxQuestItemTypes {
    public static final QuestItemType KARAKA_CASE_CHOICE = QuestItemTypes.of(
            "KARAKA_CASE_CHOICE", QuestDomain.SYNTAX, AnswerMode.SINGLE_CHOICE);
}

public final class LexiconQuestItemTypes {
    public static final QuestItemType VOCABULARY_WORD = QuestItemTypes.of(
            "VOCABULARY_WORD", QuestDomain.LEXICON, AnswerMode.FREE_TEXT);
    public static final QuestItemType VOCABULARY_SYNONYM = QuestItemTypes.of(
            "VOCABULARY_SYNONYM", QuestDomain.LEXICON, AnswerMode.SINGLE_CHOICE);
    public static final QuestItemType VOCABULARY_ANTONYM = QuestItemTypes.of(
            "VOCABULARY_ANTONYM", QuestDomain.LEXICON, AnswerMode.FREE_TEXT);
    public static final QuestItemType VOCABULARY_ROOT = QuestItemTypes.of(
            "VOCABULARY_ROOT", QuestDomain.LEXICON, AnswerMode.FREE_TEXT);
    public static final QuestItemType VOCABULARY_SEMANTIC_FIELD = QuestItemTypes.of(
            "VOCABULARY_SEMANTIC_FIELD", QuestDomain.LEXICON, AnswerMode.SINGLE_CHOICE);
    public static final QuestItemType VOCABULARY_GENDER = QuestItemTypes.of(
            "VOCABULARY_GENDER", QuestDomain.LEXICON, AnswerMode.SINGLE_CHOICE);
}

public final class ProsodyQuestItemTypes {
    public static final QuestItemType CHANDAS_IDENTIFICATION = QuestItemTypes.of(
            "CHANDAS_IDENTIFICATION", QuestDomain.PROSODY, AnswerMode.SINGLE_CHOICE);
}

public final class CompoundQuestItemTypes {
    public static final QuestItemType COMPOUND_SPLIT = QuestItemTypes.of(
            "COMPOUND_SPLIT", QuestDomain.MORPHOLOGY, AnswerMode.FREE_TEXT);
    public static final QuestItemType COMPOUND_TYPE = QuestItemTypes.of(
            "COMPOUND_TYPE", QuestDomain.MORPHOLOGY, AnswerMode.SINGLE_CHOICE);
}
```

`QuestItemTypes.of(...)` — фабрика, создающая простую record-реализацию `QuestItemType`; добавление нового типа — одна строка в соответствующем классе-держателе, без изменения остальной кодовой базы.

---

## 4. quiz-service — прохождение и прогресс (`sm.selflearn.samskrtam.quiz.progress`)

Не знает о payload и типах — работает только с `itemId`, `correct: boolean` и абстрактным алгоритмом планирования. См. модель данных в [quest-engine.md §2.2](./quest-engine.md#22-quiz-service--прогресс).

```java
public record ItemProgress(
        UUID userId, UUID itemId,
        int repetitions, BigDecimal easeFactor, int intervalDays,
        Instant dueAt, boolean lastResult
) { }

public enum ProgressStatus { NEW, LEARNING, DUE, MASTERED }

/** Вычисление статуса по прогрессу — единая логика для любого типа (см. quest-engine.md §2.3). */
public interface ProgressStatusResolver {
    ProgressStatus resolve(Optional<ItemProgress> progress, Instant now);
}

/**
 * Алгоритм планирования интервалов — абстракция вместо жёстко зашитого SM-2,
 * чтобы алгоритм можно было заменить/A-B-тестировать не трогая остальной сервис.
 */
public interface SpacedRepetitionAlgorithm {
    ItemProgress reschedule(Optional<ItemProgress> current, boolean answeredCorrectly, Instant now);
}

@Component
public class Sm2SpacedRepetitionAlgorithm implements SpacedRepetitionAlgorithm {
    @Override
    public ItemProgress reschedule(Optional<ItemProgress> current, boolean correct, Instant now) {
        // реализация правил из quest-engine.md §3
        ...
    }
}

/** Отбор кандидатов на сессию — тоже абстракция, реализация по умолчанию — DUE→NEW→LEARNING (quest-engine.md §4.1). */
public interface SessionItemSelector {
    List<UUID> selectItems(UUID questId, UUID userId, int sessionSize);
}

/** Фасад для контроллера сессий — единственная точка входа, не зависящая от типа квеста. */
public interface QuestSessionService {
    QuestSession start(UUID questId, UUID userId, int size);
    AttemptResult submitAttempt(UUID sessionId, UUID itemId, String answer);
    SessionSummary complete(UUID sessionId);
}
```

---

## 5. Границы ответственности

| Слой | Знает про типы квестов | Знает про прогресс/повторение |
|---|---|---|
| curriculum-service (`QuestItemGenerator`, `AnswerChecker`) | да, по одному классу на тип | нет |
| shared-модель (`QuestItem`, `QuestItemType`) | только контракт, не логику типов | нет |
| quiz-service (`SpacedRepetitionAlgorithm`, `SessionItemSelector`) | нет | да, единая реализация для всех типов |

Новый тип из [quest-catalog.md](curriculum-service/quest-catalog.md) добавляется реализацией `QuestItemGenerator`/`AnswerChecker` и константой в соответствующем `*QuestItemTypes` — без изменений в quiz-service.
