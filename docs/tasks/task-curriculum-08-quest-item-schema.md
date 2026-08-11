# Задача: curriculum-service — Flyway-миграция `curriculum.quest_item`

**Что:** Новая generic-таблица `curriculum.quest_item`, обслуживающая 4 типа заданий
склонения (и в будущем — другие типы квестов) единой структурой.
**Зачем:** См. `docs/services/curriculum-quest-items.md` §1.

## Контекст
**Затронутые сервисы:** curriculum-service
**Зависит от:** уже применённых миграций lexicon-схемы (`curriculum.lexeme`, `curriculum.morphology_class`, `curriculum.lexeme_morphology`) — таблица должна ссылаться на существующую `curriculum.topic`.

## Шаги

1. Создать файл `services/curriculum-service/src/main/resources/db/migration/V5__create_quest_item.sql` (номер версии — следующий свободный после существующих файлов в `db/migration`, проверить командой `ls services/curriculum-service/src/main/resources/db/migration/`).
2. В файле — `CREATE TABLE curriculum.quest_item`:
   - `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
   - `topic_id UUID NOT NULL REFERENCES curriculum.topic (id) ON DELETE CASCADE`
   - `item_type VARCHAR(40) NOT NULL`
   - `answer_mode VARCHAR(20) NOT NULL`
   - `prompt TEXT NOT NULL`
   - `correct_answer TEXT NULL`
   - `distractors JSONB NOT NULL DEFAULT '[]'::jsonb`
   - `payload JSONB NOT NULL`
   - `generator_source VARCHAR(60) NOT NULL`
   - `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`
   - `CONSTRAINT chk_quest_item_answer_mode CHECK (answer_mode IN ('FREE_TEXT','SINGLE_CHOICE','MULTI_SELECT','SPAN_SELECT','MATCHING'))`
3. Индексы: `CREATE INDEX idx_quest_item_topic_type ON curriculum.quest_item (topic_id, item_type);` и `CREATE INDEX idx_quest_item_type ON curriculum.quest_item (item_type);`.
4. Идемпотентность генерации — добавить служебную таблицу `curriculum.quest_item_generation_key`: `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`, `quest_item_id UUID NOT NULL REFERENCES curriculum.quest_item (id) ON DELETE CASCADE`, `generation_key VARCHAR(200) NOT NULL` (строка вида `topicId:itemType:lexemeId:caseType:numberType`, формируется сервисным кодом), `CONSTRAINT uq_quest_item_generation_key UNIQUE (generation_key)`.
5. `COMMENT ON TABLE curriculum.quest_item IS 'Materialized quest items for all quest types (grammar+lexicon), see curriculum-quest-items.md §1.';`

## Критерии готовности (DoD)
- [ ] Миграция применяется без ошибок на чистой БД (`./gradlew :services:curriculum-service:flywayMigrate` или через тест-контейнер)
- [ ] Уникальный индекс `uq_quest_item_generation_key` не даёт создать два одинаковых по generation_key вопроса
