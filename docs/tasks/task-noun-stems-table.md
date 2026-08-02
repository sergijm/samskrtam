# Задача: таблица `noun_stems` — классификация словоизменительного класса слова

> Оркестратор: Агент 0. Контракт: Агент 6 (см. `docs/services/sangraha-service/
> verse-word-grammar.md` §1а и `docs/services/sangraha-service.md` §9 — источник
> истины для задач ниже). Затронут только sangraha-service.
>
> Задачи разбиты на шаги ~30 сек работы модели (DeepSeek V4 Flash / Qwen3 Coder 30B
> A3B Instruct) — каждый шаг самодостаточен.
>
> **Допущения оркестратора** (пользователь не уточнил явно — фиксирую, пересмотреть
> может любой агент по факту реализации): `@OneToMany` — потому что на одно слово
> возможно несколько строк (история классификаций разными моделями/прогонами), при
> поиске используется строка с максимальным `confidence`; заполнение таблицы данными
> — **вне рамок этой задачи** (как и заполнение `verse_words` — либо внешний скрипт,
> либо отдельный будущий процесс, решение не зафиксировано); `noun_stems` — в
> приоритете при поиске, при отсутствии строк — fallback на старую эвристику
> (определение `vowelType` по последней букве `VerseWord.stem`), которая **не
> удаляется** из кода в этой задаче.

---

## Агент 2 — Backend (sangraha-service)

**B1. Миграция.** Новый файл `V3__create_noun_stems.sql` (миграции сведены к одному
`V1` — см. `verse-word-grammar.md`, но это уже после первого релиза схемы, поэтому
здесь — обычный новый `V3`, после существующего `V2`) в
`services/sangraha-service/src/main/resources/db/migration/`. Таблица
`sangraha.noun_stems`: id (uuid, PK, DEFAULT gen_random_uuid()), verse_word_id (uuid,
NOT NULL, FK → sangraha.verse_words(id), ON DELETE CASCADE), stem_iast (varchar(200),
NOT NULL), stem_class (varchar(20), NOT NULL, CHECK — значения ровно те же 7, что
`ck_vowel_type` в content-service: `A_STEM`, `AA_STEM`, `I_STEM`, `II_STEM`, `U_STEM`,
`UU_STEM`, `R_STEM`), confidence (varchar(10), NOT NULL, CHECK — `HIGH`, `MEDIUM`,
`LOW`, тот же паттерн, что `ck_analysis_confidence`/аналогичный constraint у
`verse_words.analysis_confidence`, найти точное имя существующего constraint в V1 и
использовать симметричное `ck_noun_stem_confidence`), model (varchar(100), NOT NULL),
created_at (timestamptz(6), NOT NULL, DEFAULT now()). Constraint-имена по конвенции
проекта: `pk_noun_stems`, `fk_noun_stems_word`, `ck_noun_stem_class`,
`ck_noun_stem_confidence`.

**B2. Индексы.** В той же миграции: индекс на `verse_word_id` (обязателен для FK и
для `@OneToMany`-выборки — по аналогии с `idx_verse_words_verse_id` в V1);
композитный индекс на `(verse_word_id, stem_class)` — не пригодится напрямую (поиск
по `stem_class` без привязки к конкретному слову идёт не через этот индекс, см. B4),
поэтому вместо него — индекс на `stem_class` отдельно (используется в поисковом
запросе `/declension-examples`, см. B4, для фильтрации по `vowelType`). Сортировка по
`confidence`/`created_at` для выбора "лучшей" строки на слово — по одному слову
всегда небольшое количество строк (история классификаций одного слова), отдельный
индекс под это не нужен, сортировка в памяти на стороне приложения (см. B3)
достаточна.

**B3. JPA-сущность `NounStem`.** Новый класс в том же пакете `model/`, что
`VerseWordMorphology`/`VerseWordDerivation`. Поля: id (UUID, `@Id`,
`@GeneratedValue`), verseWord (`@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name
= "verse_word_id")`), stemIast (String), stemClass (новый Java-энум `StemClass` — те
же 7 значений, что `A_STEM`...`R_STEM`; уточнить у Агента 2, нет ли уже такого энума
под другим именем на стороне sangraha для переиспользования, прежде чем заводить
новый), confidence (переиспользовать существующий `AnalysisConfidence` энум `HIGH,
MEDIUM, LOW` — не заводить второй одинаковый энум), model (String), createdAt
(Instant/OffsetDateTime, `@CreationTimestamp` или ручной `DEFAULT now()` по аналогии с
уже имеющимися timestamp-полями проекта). На стороне `VerseWord` — новое поле
`nounStems` (`List<NounStem>`, `@OneToMany(mappedBy = "verseWord", cascade =
CascadeType.ALL, orphanRemoval = true)`) — по аналогии с существующим cascade-
паттерном `morphology`/`derivation`, но `List`, а не одиночная ссылка.

**B4. Обновить поиск в `/declension-examples`.** В реализации обработчика (найти
метод/репозиторий, где сейчас определяется `vowelType` слова по последней букве
`stem`, см. `sangraha-service.md` §9) — новая логика: для каждого `VerseWord`-
кандидата сначала смотрим его `noun_stems`; если список не пуст — берём строку с
максимальным `confidence` (`HIGH` > `MEDIUM` > `LOW`, сравнение не по алфавиту,
алфавитный порядок этих трёх строк не совпадает с приоритетом — нужен явный
`Comparator`/`CASE`-выражение), при равенстве `confidence` — с максимальным
`created_at`; `stemClass` этой строки = `vowelType` слова. Если `noun_stems` пуст для
данного слова — fallback на прежнюю эвристику по последней букве `stem` (код не
менять, только оборачивать условием "если нет строк в noun_stems"). Для `PRON_*` —
без изменений, фиксированное соответствие по лемме (см. `sangraha-service.md` §9),
`noun_stems` для местоимений не используется.

**B5. Юнит-тест на выбор "лучшей" строки (B4).** Чистая функция/метод, принимающая
`List<NounStem>` (моки, без БД), возвращающая одну строку. Кейсы: (a) одна строка —
возвращается она; (b) несколько с разным `confidence` — возвращается `HIGH`, даже
если она не последняя по `created_at`; (c) несколько с одинаковым `confidence` —
возвращается с максимальным `created_at`; (d) пустой список — метод сигнализирует
"нет данных" (Optional.empty() или аналог), не бросает исключение — вызывающий код
переходит на fallback-эвристику.

**B6. Интеграционный тест на fallback (B4).** Testcontainers PostgreSQL. Стих/слово
без единой строки в `noun_stems`, `VerseWord.stem` оканчивается на, например, `a` —
убедиться, что поиск по `vowelType = A_STEM` всё равно находит это слово (через
старую эвристику). Отдельный кейс: то же слово, но с одной строкой `noun_stems` с
`stem_class = U_STEM` (заведомо расходится с эвристикой по букве) — поиск по
`vowelType = A_STEM` его **не** находит, а по `vowelType = U_STEM` — находит
(`noun_stems` побеждает эвристику).

---

## Критерии готовности

- [ ] B1: миграция `V3__create_noun_stems.sql` применяется на чистой БД без ошибок
- [ ] B2: индексы созданы (`verse_word_id`, `stem_class`)
- [ ] B3: `NounStem` и `VerseWord.nounStems` — рабочий `@OneToMany`/`@ManyToOne`,
      cascade/orphanRemoval ведут себя как у `morphology`/`derivation`
- [ ] B4: поиск в `/declension-examples` использует `noun_stems` в приоритете, старая
      эвристика остаётся как fallback и не удалена
- [ ] B5, B6: тесты проходят, оба сценария (выбор лучшей строки, приоритет над
      эвристикой) покрыты
