# Lexical Quizzes — типы заданий, генерация сессий, adaptive selection

> Связанные файлы: [lexicon.md](./lexicon.md), [lexical-curriculum.md](curriculum-service/lexical-curriculum.md),
> [quest-catalog.md](curriculum-service/quest-catalog.md) (уже существующие `VOCABULARY_*` типы — расширяются
> здесь, не заменяются), [quest-engine.md](./quest-engine.md) (quiz-service, генерация сессий).

---

## 0. Разделение ответственности (кто хранит, кто генерирует)

**curriculum-service** (модуль lexicon, см. `lexicon.md` §0) хранит **пулы**
(Lexeme + таксономии + occurrences + progress) и отдаёт их по запросу через read
API (`resolve pool`). curriculum-service не хранит готовых вопросов и не знает,
что такое «сессия» — сессии, ответы, события (`QuizAnsweredEvent`) остаются
доменом **quiz-service**, как и для grammar-квизов (`quest-engine.md`) — это всё
ещё межсервисный вызов quiz-service → curriculum-service (два разных
приложения/деплоя), просто curriculum-service — один сервис вместо ранее
предполагавшихся двух (`curriculum-service` + `lexicon-service`), см.
`lexicon.md` §0 п.3. Разница с текущим `VOCABULARY` в curriculum-service (§0
`lexicon.md`): там весь список слов квиза статичен и лежит в
`content.vocabulary_words`; здесь пул **резолвится на старте сессии** динамически
по параметрам, а сами вопросы (с дистракторами) генерируются quiz-service
generate-on-read — по той же архитектуре, что уже применяется для declension/
conjugation квизов (`quiz-sessions.yaml`, «Distractors are generated on-the-fly и
NOT persisted»).

Интеграционная точка (новая, аналог `ContentClient` у quiz-service):
`GET /api/v2/lexicon/pool/resolve` — принимает критерии отбора (§3), возвращает
список кандидатов `lexemeId` (+ достаточные для генерации вопроса поля: lemma,
gloss, pos, gender, доступные `WordForm`) без разбивки на «вопросы» — сборка
вопроса конкретного `questType` (§2) с дистракторами — задача quiz-service, как и
для остальных типов квестов.

---

## 1. Явное расширение quest-catalog.md — 5 категорий вопросов

Действующий `quest-catalog.md` уже содержит `VOCABULARY_WORD` (прямое/обратное
направление) и упоминание `VOCABULARY_SYNONYM`/`VOCABULARY_ANTONYM` в
`curriculum.md` Слой 3. Ниже — полный набор, реализующий все 5 категорий из
задачи (узнавание/recall/различение/контекст/использование), не только
flashcards:

| questType | Категория | Формат | Что проверяет |
|---|---|---|---|
| `VOCABULARY_RECOGNITION_DEVA` | Узнавание | Devanāgarī → meaning, MCQ | Чтение письма + значение |
| `VOCABULARY_RECOGNITION_IAST` | Узнавание | IAST → meaning, MCQ | Значение по транслитерации |
| `VOCABULARY_RECALL` | Recall | Meaning → Sanskrit (IAST/Devanāgarī), MCQ среди дистракторов той же `semanticTopic`/`pos` | Активное вспоминание формы по значению — сложнее, чем узнавание |
| `VOCABULARY_DISCRIMINATION` | Различение | «Какое из этих слов значит X» — дистракторы намеренно похожи (тот же `posCode` + близкий `frequencyRank`, или графически похожая лемма) | Разграничение близких по форме/значению слов, не просто угадывание по исключению |
| `VOCABULARY_CONTEXTUAL` | Контекст | Cloze: предложение из `SourceOccurrence`/`WordForm` с пропуском, MCQ на leмму | Понимание слова в реальном контексте, не в изоляции |
| `VOCABULARY_PRODUCTION` | Использование | Дано описание ситуации/переведённая фраза с пропуском, нужно выбрать слово, корректно завершающее санскритскую фразу (MCQ, не свободный ввод — остаётся auto-gradable) | Активное использование, ближе всего к «употреблению», без свободного текста (см. §6 — открытый вопрос) |

`VOCABULARY_SYNONYM`/`VOCABULARY_ANTONYM` (уже существующие в
`curriculum.md`/`quest-types-overview.md`) остаются как есть — это частный
случай `VOCABULARY_DISCRIMINATION` с дистракторами, подобранными по
антонимии/синонимии, а не по форме; переиспользуют ту же таксономию (для
дистракторов нужна `semanticTopic`-близость — берётся из `lexicon.md` §3.2).

Каждый Topic-quiz (§2 `lexical-curriculum.md`) по умолчанию включает **все**
применимые questType, не только recognition — конкретный набор на сессию
подбирается алгоритмом отбора (§4), не фиксируется на Topic.

---

## 2. Виды vocabulary-квизов (VocabularyQuizDefinition)

Только для видов, требующих **курируемого**, повторно используемого каталога —
`USER_COLLECTION` и `ADAPTIVE_REVIEW` персональны и не заводят строку в каталоге
(резолвятся по параметрам прямо при старте сессии, см. §5).

Таблица `curriculum.vocabulary_quiz_definition`:
id (UUID, PK), kind (VARCHAR 20, NOT NULL — `TOPIC`|`MIXED_TOPIC`|`FREQUENCY_BAND`|`SOURCE`),
titleRu / titleEn (VARCHAR 200, NOT NULL),
topicId (UUID, NULL — для `kind=TOPIC`, значение `curriculum.topic.id`),
complexQuizId (UUID, NULL — для `kind=MIXED_TOPIC`, значение `curriculum.complex_quiz.id`,
переиспользует `ComplexQuiz` из `curriculum-service` целиком, включая уже готовую
валидацию 2–4/5–7 тем, см. `lexical-curriculum.md` §1),
frequencyRankMax (INTEGER, NULL — для `kind=FREQUENCY_BAND`, кумулятивно `rank <= N`, см. `lexical-curriculum.md` §2),
sourceId (UUID, NULL — для `kind=SOURCE`, FK → lexicon.source.id),
sourceLocationPrefix (VARCHAR 100, NULL — опционально, для «Chapter/section vocabulary», §15 задачи: фильтр `source_occurrence.locationRef LIKE prefix || '%'`),
createdAt / updatedAt

Ровно один из `topicId`/`complexQuizId`/`frequencyRankMax`/`sourceId` заполнен, в
зависимости от `kind` — проверяется в сервисном слое (CHECK-констрейнт на 4
взаимоисключающих nullable-поля средствами БД неудобен и не даёт понятной
ошибки — валидация на уровне Java).

**`Core Vocabulary 1/2/…`** (задача §14) — это просто несколько заранее
заведённых строк `kind=FREQUENCY_BAND` с `frequencyRankMax = 100/250/500/1000/2000`
(соответствуют границам §2 `lexical-curriculum.md`) — не отдельный механизм.
Произвольный диапазон (`101 <= rank <= 250` без нижней границы 1) — это уже не
каталожный `VocabularyQuizDefinition`, а прямой параметризованный вызов
`pool/resolve?rankMin=101&rankMax=250` без сохранения в каталог (задача §14,
«quiz engine должен уметь динамически формировать сессии» — динамический путь
существует параллельно каталогу, каталог — просто набор часто используемых
именованных ярлыков для UI).

---

## 3. Отбор пула (`pool/resolve`)

Вход: произвольная комбинация — `topicIds[]` (0..N), `frequencyRankMin/Max`,
`posCodes[]`, `morphologyClassCodes[]`, `sourceId` (+ опционально
`sourceLocationPrefix`), `collectionId`, `excludeMasteredForUserId` (если задан —
исключить лексемы с `masteryScore >= 90` этого пользователя из
`user_lexeme_progress`, чтобы не тратить сессию на уже освоенное, кроме явного
`ADAPTIVE_REVIEW`, где наоборот интересны почти-освоенные для закрепления, см. §4).

Все фильтры **пересекаются** (AND) между измерениями и **объединяются** (OR)
внутри одного измерения (несколько `topicIds` = слова любой из этих тем — это и
есть механика `MIXED_TOPIC`/`Integrated Vocabulary Practice`).

**Правила балансировки состава** (задача §13 — «не просто случайное
объединение»), применяются в curriculum-service (модуль lexicon) до отдачи пула, не в quiz-service:
1. Если `topicIds.size() > 1` — квота на тему: не более `ceil(poolLimit / topicIds.size()) + 2` лексем от одной темы, чтобы большая тема не вытеснила малые (пример задачи: 4 темы → не более ~7 слов от каждой в пуле условных 20–25);
2. Внутри итогового пула — не более 2 лексем подряд одного `posCode` после сортировки по приоритету (§4) — реализуется финальным reshuffle с ограничением, не влияет на сам приоритет;
3. Пул всегда больше нужного размера сессии (обычно ×2–3) — фактическая выборка под сессию делает quiz-service через adaptive scoring (§4), curriculum-service не знает размера сессии.

---

## 4. Adaptive selection — формула приоритета

`priority(lexeme, user) = 3.0·overdue + 2.0·masteryGap + 1.5·recentError + 1.0·frequencyWeight + 0.5·novelty + jitter`

- `overdue = clamp((now − nextReviewAt) / 24h, 0, 3)`, 0 если `nextReviewAt` в будущем или NULL — совпадает по духу с уже используемым в quiz-service ADR-007 (`REVIEW`-бакет по `nextReviewAt`), вес наибольший: просроченный повтор — самый дорогой сигнал для долгосрочного запоминания (spaced repetition), поэтому 3.0;
- `masteryGap = (100 − masteryScore) / 100` (0..1) — чем ниже освоенность, тем выше приоритет; вес 2.0, второй по важности сигнал;
- `recentError = 1`, если последний ответ по этой лексеме (по данным `user_lexeme_progress`/недавней истории quiz-service) был неверным, иначе 0 — вес 1.5, обеспечивает быстрое повторное закрепление ошибки в той же/следующей сессии, не только по расписанию `nextReviewAt`;
- `frequencyWeight = 1 − (rank − 1) / 2000` (0..1, выше для частотных слов), при отсутствии `rank` (слово вне топ-2000 — из `Source`/пользовательской коллекции) — 0.3 (нейтральное значение, не 0, чтобы не занижать искусственно слова из коллекций); вес 1.0 — сознательно ниже review-сигналов: продвигать новую частотную лексику важно, но не важнее закрепления уже введённой;
- `novelty = 1`, если `exposureCount == 0`, иначе 0 — вес 0.5, наименьший: система должна вводить новые слова, но не за счёт вытеснения повторения (иначе mastery никогда не растёт — типичная ошибка наивных алгоритмов, которую задача просит избежать явно, п.17 «не делать просто…»);
- `jitter` — равномерный случайный шум `[0, 0.3)`, добавлен, чтобы при равных приоритетах сессии не были побайтово идентичны при повторном прохождении в тот же день.

Сессия: взять топ-`sessionSize × 2.5` по `priority` из пула (§3), применить
правила балансировки п.2 §3, отобрать первые `sessionSize`, вопросы — вперемешку
по `questType` (§1), не более 2 подряд одного типа (то же правило reshuffle, что
и для POS).

**Почему не проще (например, просто `nextReviewAt ASC`):** чистая
сортировка по due-дате не учитывает, что упавшее до due, но плохо усвоенное
слово (низкий `masteryScore`, недавняя ошибка) полезнее закрепить раньше
формального срока — а чистое частотное ранжирование без mastery/review быстро
вырождается в «показываем всегда одни и те же топ-100 слов». Формула — линейная
комбинация с весами, не ML-модель: сознательный выбор для прозрачности и
простоты первой реализации (в отличие от motion к обучаемой модели ranking'а,
которая требует данных, которых пока нет).

---

## 5. Session generation flow (сквозной сценарий)

1. Пользователь выбирает точку входа: конкретный Lexical Topic (`curriculum.topic`,
   `domain=LEXICON`), `VocabularyQuizDefinition` (Frequency/Source/Mixed), свою
   `UserCollection`, либо «Adaptive review» с домашнего экрана.
2. Фронтенд → `quiz-service`: `POST /quiz/vocabulary-v2/sessions/start` с
   параметрами точки входа (новый маршрут, параллельный существующему
   `/quiz/{slug}/sessions/start` — не переиспользует его, т.к. тот рассчитан на
   один `Lesson.id`, а не динамический пул, см. `lexicon.md` §0 п.2).
3. `quiz-service` → curriculum-service: `GET /api/v2/lexicon/pool/resolve` с этими параметрами
   (+ `excludeMasteredForUserId=userId`, если это не `ADAPTIVE_REVIEW`) → пул
   кандидатов.
4. `quiz-service` (не curriculum-service) → curriculum-service:
   `GET /users/{userId}/progress?lexemeIds=...` (batch) — тянет `masteryScore`/
   `nextReviewAt`/`exposureCount` только для кандидатов пула, применяет формулу
   §4 (сам scoring — код quiz-service, не curriculum-service, т.к. это тот же слой,
   где уже живёт вся остальная session-логика и ADR-007).
5. `quiz-service` генерирует вопросы нужных `questType` (§1) с дистракторами
   on-the-fly (дистракторы для `RECOGNITION`/`RECALL` — случайные лексемы того же
   `posCode`; для `DISCRIMINATION` — намеренно близкие, см. таблицу §1; для
   `CONTEXTUAL` — предложение берётся из `WordForm.sourceOccurrenceId` при
   наличии, иначе тип пропускается для этой лексемы), сохраняет `session_questions`
   у себя — по образцу существующего flow (`quiz-sessions.yaml`).
6. Ответы (`POST .../answer`) обновляют `user_lexeme_progress` — вызов
   curriculum-service (`PATCH /api/v2/lexicon/users/{userId}/progress/{lexemeId}`) из
   `quiz-service` при каждом ответе или батчем при `complete` (решение о
   синхронности/батче — задача реализации, не архитектурное решение этого
   документа); тем же способом, каким `quiz-service` сегодня пишет `grammar_form_score`.

---

## 6. Открытые вопросы

- `VOCABULARY_PRODUCTION` через MCQ — компромисс ради auto-grading; полноценная
  генерация санскритской фразы пользователем потребовала бы NLP-проверки
  свободного ввода (сандхи, синонимия) — вне периметра текущей архитектуры,
  возможное будущее расширение, не блокирует текущий дизайн.
- Единая шкала `masteryScore`/`nextReviewAt` для лексики отдельно от grammar
  `quiz_item_score` — сознательно раздельные таблицы (домены разные сервисы), но
  формула пересчёта на ответ должна быть согласована с ADR-007, чтобы поведение
  «due review» ощущалось пользователем одинаково в grammar- и lexical-квизах;
  фиксация точных констант ADR-007 применительно к лексике — задача при
  реализации.
- Кому физически принадлежит вызов enrichment для `VOCABULARY_CONTEXTUAL`
  (нужен реальный `SourceOccurrence` с достаточным контекстом предложения, не
  только одно слово) — зависит от объёма размеченных `source_occurrence` на
  момент реализации, см. `lexicon-content-pipeline.md` §2.
