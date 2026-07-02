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

### ADR-006: sangraha-service — произведения, LLM-анализ стихов, синхронизация лексики через Kafka

**Статус:** Принято

**Контекст:** Нужен функционал работы с санскритскими текстами (произведения → главы →
стихи), LLM-анализ стиха (транслитерация, перевод, сандхи, грамматика) и передача
извлечённой лексики в существующий механизм VOCABULARY-квизов content-service
(`VocabularyCategory`/`VocabularyWord`/`VocabularyWordCategory`, дерево категорий уже
поддерживает агрегацию слов по поддереву — см. `VocabularyService.getVocabularyWordsForQuiz`).

**Решение:**
- Заводится новый сервис **`sangraha-service`** (Java 21 + Virtual Threads, схема БД `sangraha`), а не домен внутри `content-service` — см. `docs/services/sangraha-service.md`.
- LLM (OpenAI-совместимый API) вызывается напрямую из `sangraha-service`, конфигурация только через env (`SANGRAHA_LLM_*`), без дефолтов в yml. Ответ модели принимается строго через **tool calling** (один tool `submit_verse_analysis` со строгой JSON-схемой) — свободный текст не парсится.
- Никаких синхронных HTTP-вызовов между `sangraha-service` и `content-service`/`dictionary-service`. Единственный канал — **Kafka**, topic `sangraha-vocabulary-events` (transactional outbox, как в `user-service`/`quiz-service`), событие публикуется **на каждый проанализированный стих**.
- Иерархия `work.slug` → `chapter.slug` используется как `code` в дереве `VocabularyCategory` content-service (`categoryCode = "{workSlug}.{chapterSlug}"`), что даёт VOCABULARY-квиз «бесплатно» через уже существующий механизм агрегации по поддереву.
- Дедупликация слов в content-service — по `(wordIast, stem)`: при совпадении не создаём новый `VocabularyWord`, только добавляем связь `VocabularyWordCategory`.
- Связь слов стиха со словарными статьями `dictionary-service` (`slp1`) **не делается** в этой итерации.
- Версии `VerseAnalysis`/`VerseWord` не хранятся — повторный анализ перезаписывает предыдущий результат.
- Права доступа: весь write-контур `sangraha-service` — только `ADMIN`. Отдельная роль «редактор/переводчик» — отложена.

**Следствие:**
- Агент 2 (Domain), назначенный на `sangraha-service`, должен завести первый в проекте `@KafkaListener` — в `content-service` (consumer `sangraha-vocabulary-events`).
- **Shared DTO**: заведён `sm.selflearn.samskrtam.sangraha.event.SangrahaVocabularyEvent` в `shared/samskrtam-dtos` (пакет `sangraha`). Решение Агента 6: событие используется двумя сервисами (producer + consumer), локальный DTO создал бы дублирование и риск рассинхронизации.
- **Порт**: фиксирован `8089`, согласован с Агентом 5 DevOps.
- **Quiz(VOCABULARY) — только на уровне произведения**: Quiz заводится с `slug = workSlug`. Главы не получают отдельного Quiz — агрегация слов по поддереву категорий (`VocabularyService.getVocabularyWordsForQuiz`) уже поддерживает фильтрацию по `categoryCode = "{workSlug}.{chapterSlug}"` через дерево категорий. Отдельный Quiz на главу создал бы дублирование набора слов.

---

## 15. Kafka

- Топики именуются `<domain>-<событие-во-множественном-числе>-events`, kebab-case: `quiz-answered-events`, `quiz-session-status-changed-events`, `sangraha-vocabulary-events`.
- Публикация — только через Transactional Outbox Pattern (таблица `outbox_events` в схеме сервиса-источника + плановый publisher), см. пример в `user-service`/`quiz-service`. Прямая публикация в Kafka из бизнес-логики без outbox — запрещена.
- Синхронные вызовы между доменными сервисами (Domain ↔ Domain) по HTTP не приветствуются там, где можно обойтись асинхронным событием — см. ADR-006.

## 16. Мапперы Entity/DTO

### 16.1 Общее правило

Маппинг entity/model → DTO выносится в отдельный пакет `mapper/` внутри сервиса. Реализация — **MapStruct** с `@Mapper(componentModel = "spring")`. Один файл маппера — одна доменная область (entity → DTO и обратно).

### 16.2 Запрещённые паттерны

- `abstract class` с `@Autowired` внутри маппера — запрещён (нарушает Single Responsibility, смешивает маппинг и бизнес-логику).
- Полная реализация entity → DTO вручную (десятки строк `.builder()...build()`, дублирующие структуру MapStruct-маппера) внутри `*Service`/`*Controller` — блокирующее замечание на code review для **нового** кода: такой маппинг выносится в `mapper/`.

> **Пересмотрено:** первоначальная версия правила («любой `.builder()` вне `mapper/` — нарушение») оказалась нереалистичной — на момент пересмотра `.builder()` присутствует в сервисном слое всех сервисов (~30 файлов), в основном там, где DTO собирается из **нескольких источников** (агрегат + вычисляемые поля + данные другого сервиса), что MapStruct не выражает естественно. Требовать 1:1-маппер под каждый такой случай означало плодить мапперы с одним полем и вызовом сервиса внутри — то, что сам же п. 16.2 запрещает. Существующий код **не переписывается ретроактивно**; правило действует вперёд, см. §16.3.

### 16.3 Когда `.builder()` в сервисном слое — нормально, а когда выносить в `mapper/`

Критерий — **источник данных**, а не факт использования `.builder()`:

- **Простой 1:1 маппинг** entity/model → DTO (поля переносятся почти без трансформации) — выносится в `mapper/` через MapStruct. Если видите `.builder()`, где просто одно поле в одно, без вызова сервисов/репозиториев — это кандидат на вынос.
- **DTO собирается из нескольких источников** (несколько entity, plus данные из другого сервиса/HTTP-клиента, plus вычисляемые/агрегированные поля) — `.builder()` прямо в `*Service` **допустим**. Пример: `QuizDataAssembler`, `SessionFactory` — там сборка DTO неотделима от бизнес-логики сборки сессии.
- **Практическое правило:** если тело маппинга можно описать одной MapStruct-аннотацией `@Mapping` — это mapper. Если требуется `if`/цикл/вызов другого бина — это часть сервисной логики, и `.builder()` в `*Service` — нормальный способ собрать результат, не обязательно продукт для `mapper/`.
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
