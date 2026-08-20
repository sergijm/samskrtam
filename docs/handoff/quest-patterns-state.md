# Handoff: quest_pattern — читаемые коды операций + stub-типы

Компактный контекст и план. Дата: 2026-08-17.

## Goal

Внедрить `quest_pattern` — декоративную метку операции (из `docs/services/quest_catalog_2.md`)
на каждом `quest_item`, не меняя модель прогресса и quiz-service. Уже реализованные типы
переводятся на новую схему кодов; новые (нереализованные) типы регистрируются в реестре
`QuestItemTypes` как stub-константы без генераторов. Обратная совместимость с уже
сгенерированными `quest_item` не нужна — таблица перегенерируется заново.

## Проверенные решения (из анализа разговора)

1. **Три оси вместо 47 типов**: `QuestItemType` = навык (домен × AnswerMode); `quest_pattern`
   = когнитивная операция (46 кодов из доки); `AnswerMode` = механизм проверки/рендера.
2. **Коды** (в `quest_catalog_2.md` переименованы, нижний регистр, дефис, гласные, слово 3–5,
   весь код ≤ 9):
   - Глаголы: `ver-form` `ver-anal` `ver-match` `ver-class` `ver-fix` `ver-fill` `ver-tran`
     `ver-rev` `ver-odd` `ver-build`
   - Существительные: `nom-form` `nom-anal` `nom-match` `nom-class` `nom-fix` `nom-fill`
     `nom-tran` `nom-rev` `nom-odd` `nom-build`
   - Лексика: `lex-tran` `lex-rev` `lex-same` `lex-ant` `lex-cat` `lex-fill` `lex-root`
     `lex-poly` `lex-match` `lex-class` `lex-odd` `lex-anag` `lex-puz` `lex-name` `lex-num`
   - Сандхи: `san-join` `san-split` `san-match` `san-class` `san-fix` `san-pick` `san-undo`
     `san-tran` `san-chain` `san-scan` `san-build`
3. **quest_pattern — только метка**: не участвует в проверке ответа (по `AnswerMode`), в отборе
   сессии и в прогрессе (`itemType` + `progressTag`). Хранится VARCHAR(16) NULL в
   `curriculum.quest_item` → прокидывается в `QuestItemDto`.
4. **Stub-типы**: новые типы из доки регистрируются константами в shared-холдерах
   `GrammarQuestItemTypes` / `PhonologyQuestItemTypes` / `VocabularyQuestItemTypes` без
   генераторов и без данных (по образцу существующей заглушки `ConjunctionQuizItemGenerator`).
5. **Перенос реализованных типов**: `DECLENSION_FORM`/`_CHOICE` → `nom-form`,
   `CASE_RECOGNITION` → `nom-anal`, `DECLENSION_MATCH` → `nom-match`,
   `VOCABULARY_WORD` → `lex-tran` (заполняется генератором). `CASE_MEANING` — синтаксис, вне
   46 кодов доки → pattern остаётся NULL.
6. **Перегенерация**: `POST /api/v2/curriculum/quest-items/regenerate` (существующий) очищает
   `quest_item` целиком — разворачивание миграции + вызов эндпоинта достаточно, миграция
   данных не нужна.
7. **quiz-service не меняется**: проверка и сессии уже mode-агностичны. Три зашитых стыка
   (MATCHING-проверка, прогресс-бакеты, грид урока) — будущая работа отдельно, не блокирует.

## Словарь QuestPatterns (shared, `sm.selflearn.samskrtam.quest`)

Один класс-держатель `String`-констант по образцу `*QuestItemTypes`:

- `VERB_*` (10), `NOM_*` (10), `LEX_*` (15), `SND_*` (11).

## Компоненты к изменению

- **shared/samskrtam-dtos**
  - NEW `quest/QuestPatterns.java` — 46 констант.
  - `quest/GrammarQuestItemTypes.java` — + stub: `DECLENSION_CLASSIFY`(MATCHING),
    `DECLENSION_ODD`/`DECLENSION_CORRECTION`(SINGLE_CHOICE), `CONJUGATION_*` (11),
    `NUMERAL_FORM`, `PARTICIPLE_FORM`, `ABSOLUTIVE_FORM`.
  - NEW `quest/PhonologyQuestItemTypes.java` — stub: `SANDHI_SPLIT/JOIN/MATCH/CLASSIFY/
    CORRECTION/CHAIN/TRANSLITERATION/BUILD`.
  - `quest/VocabularyQuestItemTypes.java` — + stub: `VOCABULARY_RECALL/SYNONYM/ANTONYM/ROOT/
    SEMANTIC_FIELD/GENDER/ODD/ANAGRAM/MATCH/POLYSEMY`.
- **curriculum-service**
  - NEW migration `V33__add_quest_pattern.sql` — `ALTER TABLE curriculum.quest_item
    ADD COLUMN quest_pattern VARCHAR(16) NULL;`.
  - `questitem/QuestItem.java` — + `questPattern`.
  - `questitem/dto/QuestItemDto.java` — + `questPattern` (в конец record).
  - `questitem/mapper/QuestItemMapper.java` — авто-маппинг.
  - `questgen/DeclensionQuizItemGenerator.java` — `item.setQuestPattern(...)`.
  - `questgen/LexicalQuizItemGenerator.java` — `item.setQuestPattern("lex-tran")`.
  - `questgen/CaseMeaningQuizItemGenerator.java` — без изменений (pattern NULL).
- Тесты: `QuestItemControllerTest` (11 → 12 аргументов record).

## Не делаем сейчас (вне scope)

- Реальные генераторы conjugation/sandhi/новых LEX — stub-константы только.
- Генерализация трёх quiz-стыков, `SPAN_SELECT`, MULTI_SELECT проверка.
- Разбивка `quest_catalog_2.md` (551 > 350 строк) — отдельная фаза консолидации доков.
- PASSTHROUGH pattern в quiz-сессиях и фронт.

## Проверка

`./gradlew :shared:samskrtam-dtos:compileJava :services:curriculum-service:compileTestJava`
+ тесты curriculum: `QuestItemMapperTest`, `QuestItemControllerTest`, генераторы.