### ADR-002: Семантика Quiz vs Lesson vs Activity

**Статус:** Принято

**Контекст:** Термин «quiz» использовался для обозначения и урока (единицы контента) и квиза (набора вопросов). Одновременно принято решение о будущей абстракции Activity.

**Решение:**
- **Lesson** = единица контента (`A_STEM_DECLENSIONS`, словарный урок)
- **Quiz** = конкретная активность: случайная выборка вопросов из урока на одну сессию
- **QuizSession** = прохождение квиза пользователем — правильное название, не трогать
- **Activity** = будущая абстракция над `Quiz`, `Flashcard`, `RecallExercise` и др. (реализация в M5+)

**Следствие:**
- `QuizRepository`/`QuizContentService` переименовываются в `LessonRepository`/`LessonContentService`
- `QuizSession`/`QuizAnswer` — не переименовываются (семантически верны)
- `quizId` в контексте статистики, указывающий на урок, переименовывается в `lessonId`
- Kafka топики `quiz-answered-events`, `quiz-session-status-changed-events` — не переименовываются
- Роут `/api/v1/quiz/` остаётся неизменным (принадлежит quiz-service)
- `QuizListItemResponse` удаляется как дубль `LessonItemResponse`

### ADR-003: Хранение окончаний склонений в БД

**Статус:** Принято

**Контекст:** Для уроков склонений (declensions) необходима эталонная таблица окончаний, по которой quiz-service может:
1. Строить правильные ответы для каждого вопроса (caseType + numberType + gender).
2. Проверять ответы пользователя, сравнивая с эталоном.
3. Гибко поддерживать разные окончания для мужского и женского рода в рамках одного типа гласной (если потребуется).

**Решение:**
Окончания хранятся в базе данных в таблице `case_endings` со схемой:

| Колонка | Тип | Описание |
|---|---|---|
| `vowel_type` | varchar | Тип тематической гласной (`A_MASC`, `A_NEUT`, `A_FEM`, `I`, `I_LONG`, `U`, `U_LONG`, `R`) |
| `gender` | varchar | Грамматический род (`MASCULINE`, `FEMININE`, `NEUTER`, `UNSPECIFIED`) |
| `case_type` | varchar | Падеж (`NOMINATIVE`, `ACCUSATIVE`, ...) |
| `number_type` | varchar | Число (`SINGULAR`, `DUAL`, `PLURAL`) |
| `ending` | varchar | Окончание в IAST (например, `aḥ`, `am`) |

**Ключ:** (vowel_type, gender, case_type, number_type)

**Примечание:** Для уроков, где род не различает окончания (declensions-i, declensions-u, declensions-r), в таблице gender = `UNSPECIFIED`. Для уроков с одним родом (declensions-a-masc, declensions-a-neut, declensions-a-fem, declensions-i-long, declensions-u-long) gender = фактический род.

**Следствие:**
- Quiz-service при генерации вопроса читает окончание из `case_endings` по ключу (vowelType, gender, caseType, numberType).
- Таблица заполняется при инициализации данных (миграция Flyway или seed).
- Для проверки ответа не требуется дополнительная логика — прямое сравнение с эталоном.

### ADR-004: Формирование вопросов для уроков с двумя родами

**Статус:** Принято

**Контекст:** Уроки `declensions-i`, `declensions-u`, `declensions-r` охватывают два рода, но окончания для мужского и женского рода совпадают во всех падежах и числах.

**Решение:**
- Вопросы для этих уроков не дублируются по роду — каждому сочетанию (caseType, numberType) соответствует ровно один вопрос.
- Поле `gender` в GrammarQuestionProgress для таких уроков передаётся как `null` или `UNSPECIFIED`.
- Общее количество вопросов в уроке = 24 (8 падежей × 3 числа).
- Для уроков с одним родом (declensions-a-masc, declensions-a-neut, declensions-a-fem, declensions-i-long, declensions-u-long) поле `gender` обязательно и количество вопросов также 24 (gender фиксирован).

**Следствие:**
- Клиентская агрегация прогресса по ключу (gender, caseType, numberType) для уроков с двумя родами использует gender = UNSPECIFIED.
- API возвращает gender в ответе всегда, но для смешанных уроков он равен UNSPECIFIED.

### ADR-005: Единство окончаний для основ -i, -u, -ṛ независимо от рода

**Статус:** Принято

**Контекст:** Для уроков склонений с основами на -i, -u, -ṛ (`vowel_type`: I, I_LONG, U, U_LONG, R) исторически падежные окончания не различаются по грамматическому роду ни в одном падеже/числе. Это отличает их от основ на -a, где финальная гласная и род жёстко связаны (a-masc, a-neut, a-fem). При этом уроки `declensions-i`, `declensions-u`, `declensions-r` содержат слова как мужского, так и женского рода, и прогресс по каждому роду нужно отслеживать отдельно (разные основы/слова).

**Решение:**
- В таблице `case_endings` для `vowel_type = I | I_LONG | U | U_LONG | R` поле `gender` может быть любым (`MASCULINE`, `FEMININE`, `NEUTER`), но `ending_iast` для одного и того же `(case_type, number_type)` будет одинаковым для всех гендеров.
- Ключ агрегации прогресса остаётся `(gender, caseType, numberType)` для единообразия по всем 8 урокам склонений (включая declensions-i, declensions-u, declensions-r, где внутри урока две гендерные группы вопросов). Различие — в том, что `caseEnding` у двух гендерных групп совпадает, а `successRate` считается раздельно по роду, так как это разные основы/слова.
- В таблице `case_endings` допускаются дублирующие строки с одинаковым `ending_iast` и разным `gender`, либо (на усмотрение Агента 2) одна запись с `gender = UNSPECIFIED` для этих `vowel_type`. Выбор формата хранения — за Агентом 2 (Domain), но API должен возвращать корректные данные.

**Следствие:**
- ADR-003 (примечание про UNSPECIFIED для -i, -u, -r) дополнен: `UNSPECIFIED` — один из допустимых вариантов хранения; альтернатива — дублирующие записи с разным gender.
- ADR-004 уточнён: для уроков -i, -u, -r количество вопросов остаётся 24, если gender в рамках урока фиксирован; если урок содержит два рода — 48 (по 24 на каждый род), но окончания совпадают.
- Агент 2 (Domain) решает: хранить одну запись (UNSPECIFIED) или две (MASCULINE=FEMININE).
- Агент 3 (Frontend) получает `caseEnding` одинаковый для обоих родов в рамках одного (caseType, numberType), но прогресс агрегирует раздельно по роду.

---

## 15. Kafka

## 16. Мапперы Entity/DTO

### 16.1 Общее правило

Маппинг entity/model → DTO выносится в отдельный пакет `mapper/` внутри сервиса. Реализация — **MapStruct** с `@Mapper(componentModel = "spring")`. Один файл маппера — одна доменная область (entity → DTO и обратно).

### 16.2 Запрещённые паттерны

- Ручные мапперы в виде `@Component` с вызовом `.builder()` / конструктора — **code smell** (исключение: DTO → Entity для persistence-слоя вне сервисов, например, `RowMapper`).
- Маппинг entity → DTO внутри `*Service` или `*Controller` — запрещён, вся логика преобразования только в `mapper/`.
- `abstract class` с `@Autowired` внутри маппера — запрещён (нарушает Single Responsibility, смешивает маппинг и бизнес-логику).

### 16.3 Допустимые исключения

- `@Mapper(uses = {OtherMapper.class})` — допустимо для композиции мапперов.
- Default-методы и `@AfterMapping` / `@BeforeMapping` — допустимы для пост-обработки полей, не требующих вызова сервисов.
- Маппинг DTO ↔ Entity внутри `*Repository` (например, `RowMapper`) — не подпадает под это правило.

### 16.4 Пример эталонной структуры

```java
// services/quiz-service/src/main/java/.../quiz/mapper/QuizAnswerMapper.java
@Mapper(componentModel = "spring")
public interface QuizAnswerMapper {

    @Mapping(target = "isCorrect", source = "isCorrect")
    @Mapping(target = "correctOptionId", source = "correctWordId")
    AnswerResponse toAnswerResponse(boolean isCorrect, UUID correctWordId, GeneratedQuizQuestionDto dto);
}
```

### 16.5 Code Review

- Ручной маппер вне `mapper/` — блокирующий замечание.
- `abstract class` с `@Autowired` в маппере — блокирующее замечание.
- Исключения из правила 16.2 требуют комментария в коде с причиной.
