# Задача: quiz-service — CurriculumClient для declension-семейства QuestItem

**Что:** Новый WebClient-компонент в quiz-service, обращающийся к curriculum-service (API v2)
за `QuestItem` типов `DECLENSION_FORM`/`DECLENSION_FORM_CHOICE`/`CASE_RECOGNITION`/`DECLENSION_MATCH`,
и конфигурация количества вопросов каждого типа за сессию.
**Зачем:** См. `docs/services/curriculum-quest-items.md` §5, `docs/services/quest-engine.md` §5.

## Контекст
**Затронутые сервисы:** quiz-service (не curriculum-service — отдельный сервис/модуль в монорепо)
**Зависит от:** task-curriculum-11-quest-item-controller.md (эндпоинт должен существовать)

## Шаги

1. В `application.yml` quiz-service добавить блок (значения — дефолты, переопределяемые через env, без хардкода секретов):
   ```
   curriculum-service:
     base-url: ${CURRICULUM_SERVICE_URL:http://curriculum-service:8091}
   quiz:
     declension-session:
       single-choice-count: ${QUIZ_DECLENSION_SINGLE_CHOICE_COUNT:5}
       free-text-count: ${QUIZ_DECLENSION_FREE_TEXT_COUNT:5}
       case-recognition-count: ${QUIZ_DECLENSION_CASE_RECOGNITION_COUNT:3}
       match-count: ${QUIZ_DECLENSION_MATCH_COUNT:2}
   ```
   (значения — не итоговый source of truth конфигурации, синтаксис путей env-переменных согласовать с уже принятым в quiz-service стилем, свериться с существующим `application.yml`).
2. `DeclensionSessionProperties` (`@ConfigurationProperties(prefix = "quiz.declension-session")`) — поля `singleChoiceCount`, `freeTextCount`, `caseRecognitionCount`, `matchCount` (int).
3. `CurriculumClient` (WebClient, по образцу существующего `ContentClient` — свериться с его стилем реактивных вызовов, error-handling, таймаутами): метод `Flux<QuestItemDto> fetchQuestItems(UUID topicId, String itemType, int limit)` → `GET {base-url}/api/v2/curriculum/quest-items?topicId={topicId}&itemType={itemType}&limit={limit}`.
4. `QuestItemDto` (клиентский DTO в quiz-service, не путать с серверным DTO curriculum-service) — минимальный набор полей, нужных quiz-service: `id`, `itemType`, `answerMode`, `prompt`, `correctAnswer`, `distractors`, `payload` (как `JsonNode` или `Map<String,Object>` — payload используется только для отображения на фронте, quiz-service не должен парсить его типизированно, только прокидывать дальше).
5. В существующем месте старта сессии (класс, реализующий `QuestSessionService.start`, см. `quest-item-model.md` §4) — добавить ветвление: если `questId` соответствует теме с `itemType` из declension-семейства (определить через существующий механизм связи Quest↔Topic — если такой связи ещё нет в коде, зафиксировать как блокер и не выдумывать её здесь, эскалировать оркестратору), собрать пул кандидатов четырьмя вызовами `curriculumClient.fetchQuestItems(topicId, "DECLENSION_FORM_CHOICE", properties.singleChoiceCount())` и так далее по каждому из 4 типов с соответствующим count из `DeclensionSessionProperties`, объединить в один список перед применением отбора DUE/NEW/LEARNING (`quest-engine.md` §4.1) — сам алгоритм отбора не меняется, меняется только источник пула кандидатов.
6. Для остальных (не-declension) типов — оставить прежний путь через `ContentClient` (curriculum-service v1) без изменений.

## Критерии готовности (DoD)
- [ ] Интеграционный тест: mock curriculum-service (WireMock/MockWebServer) возвращает фиксированный набор QuestItem по каждому из 4 типов — сессия стартует с ожидаемым суммарным количеством вопросов (`singleChoiceCount + freeTextCount + caseRecognitionCount + matchCount`, либо меньше, если curriculum-service вернул меньше по DUE-приоритету — уточнить фактическое поведение при недостатке данных отдельным тест-кейсом)
- [ ] Non-declension Quest (например, VOCABULARY_WORD) по-прежнему идёт через `ContentClient`, регресс не сломан

## Статус реализации
Выполнены шаги 1–4 и 6: конфиг в `application.yml`, `DeclensionSessionProperties`,
`CurriculumClient`, клиентский `QuestItemDto`, non-declension путь не тронут. Компиляция quiz-service
проходит.

**Блокер (шаг 5):** ветвление при старте сессии не реализовано. В коде quiz-service
отсутствует целевая модель из `quest-item-model.md` §4 — класс `QuestSessionService.start`,
связь Quest↔Topic и отбор DUE/NEW/LEARNING поверх `QuestItem` не существуют (quiz-service
по-прежнему на legacy-модели `QuizSessionService`/`SessionCreationService`/`QuizGenerator` над
`QuizItem`+`QuizItemScore`). Механизм Quest↔Topic выдумывать не стали — эскалировано
оркестратору. Соответственно оба DoD-теста, зависящих от старта сессии, не могут быть
написаны до миграции quiz-service на целевую модель.
