-- ===================================================================
-- 1. ИСТОЧНИК: сырые данные корпуса, максимально близко к CoNLL-U
-- ===================================================================

CREATE TABLE corpus_work (
                             id              BIGSERIAL PRIMARY KEY,
                             slug            TEXT NOT NULL UNIQUE,        -- "Mahābhārata"
                             title_iast      TEXT NOT NULL,
                             title_deva      TEXT,
                             source          TEXT NOT NULL DEFAULT 'DCS', -- на будущее — если появятся другие источники
                             license_note    TEXT
);

CREATE TABLE corpus_chapter (
                                id              BIGSERIAL PRIMARY KEY,
                                work_id         BIGINT NOT NULL REFERENCES corpus_work(id),
                                chapter_number  TEXT,                        -- "0007" как в имени файла — текст, не int (бывают "0007a")
                                citation_form   TEXT,                        -- "AHS, Sū., 8" — как цитируется в традиции
                                dcs_chapter_id  INTEGER,                     -- их внутренний ID из имени файла, для сверки при реимпорте
                                source_filename TEXT NOT NULL,               -- для трассировки: из какого файла импортировано
                                UNIQUE (work_id, chapter_number)
);

-- Предложение = "sent_id" из CoNLL-U (# sent_id = 81997)
CREATE TABLE corpus_sentence (
                                 id              BIGSERIAL PRIMARY KEY,
                                 chapter_id      BIGINT NOT NULL REFERENCES corpus_chapter(id),
                                 dcs_sent_id     INTEGER NOT NULL,            -- их sent_id, для внешних ссылок/цитирования
                                 sent_counter    INTEGER,                     -- порядок внутри главы
                                 text_iast       TEXT NOT NULL,               -- # text = ... строка целиком
                                 text_deva       TEXT,                        -- транслитерируешь при импорте или лениво на чтении
                                 is_mantra       BOOLEAN DEFAULT FALSE,        -- IsMantra из MISC
                                 syntax_annotation TEXT,                       -- 'gold'/'silver'/null — качество синтаксической разметки
                                 UNIQUE (chapter_id, dcs_sent_id)
);
CREATE INDEX idx_sentence_chapter ON corpus_sentence(chapter_id);

-- Отдельное слово = строка CoNLL-U внутри предложения
CREATE TABLE corpus_token (
                              id              BIGSERIAL PRIMARY KEY,
                              sentence_id     BIGINT NOT NULL REFERENCES corpus_sentence(id),
                              token_index     SMALLINT NOT NULL,           -- поле ID из CoNLL-U (номер в предложении)
                              form            TEXT NOT NULL,               -- FORM — как слово выглядит в тексте (со слитным сандхи)
                              unsandhied      TEXT,                        -- MISC:Unsandhied — падапатха-версия, если есть
                              lemma_id        BIGINT REFERENCES corpus_lemma(id), -- см. ниже; nullable — не всегда лемма распознана
                              lemma_raw       TEXT,                        -- LEMMA как строка из файла — на случай расхождений с lemma_id
                              upos            TEXT,                        -- универсальный POS
                              xpos             TEXT,                        -- DCS-специфичный тег (см. pos.csv)
                              feats           JSONB,                        -- FEATS распарсенные в key:value (падеж, число, время и т.д.)
                              head_index      SMALLINT,                     -- HEAD — только для treebank-глав (в основном Ригведа)
                              deprel          TEXT,                          -- DEPREL
                              misc            JSONB,                         -- сырой MISC на случай полей, которые не вынесли отдельно
                              UNIQUE (sentence_id, token_index)
);
CREATE INDEX idx_token_lemma ON corpus_token(lemma_id);
CREATE INDEX idx_token_feats ON corpus_token USING GIN (feats);
CREATE INDEX idx_token_upos_xpos ON corpus_token(upos, xpos);

-- ===================================================================
-- 2. СЛОВАРЬ: из dictionary.csv (LemmaId — общий ключ для всего корпуса)
-- ===================================================================

CREATE TABLE corpus_lemma (
                              id              BIGINT PRIMARY KEY,          -- ЭТО САМ LemmaId из dictionary.csv — не свой serial!
                              lemma_iast      TEXT NOT NULL,
                              lemma_deva      TEXT,
                              primary_pos     TEXT,                         -- если словарь даёт "базовый" POS леммы
                              preverbs        TEXT,                         -- поле preverbs из dictionary.csv (для univerbified preverbs)
                              gloss_en        TEXT,                          -- краткий перевод, если есть в источнике
                              frequency_rank  INTEGER                        -- посчитать при импорте: COUNT(*) FROM corpus_token GROUP BY lemma_id
);
CREATE INDEX idx_lemma_iast ON corpus_lemma(lemma_iast);
CREATE INDEX idx_lemma_freq ON corpus_lemma(frequency_rank);

-- XPOS расшифровка (справочник из pos.csv) — не для джойнов в рантайме, для UI-тултипов
CREATE TABLE corpus_xpos_ref (
                                 xpos            TEXT PRIMARY KEY,
                                 description_en  TEXT
);

-- ===================================================================
-- 3. МОСТ К ТВОЕЙ ТАКСОНОМИИ УРОКОВ — самое важное для трёх твоих задач
-- ===================================================================

-- Уже существующие узлы курикулума (упрощённо — то, что у тебя в content-service)
-- Предполагаю, что таблица lesson уже есть; здесь только связь.

CREATE TABLE lesson_grammar_mapping (
                                        id              BIGSERIAL PRIMARY KEY,
                                        lesson_id       BIGINT NOT NULL REFERENCES lesson(id),
    -- Условие матчинга: JSONB, потому что правила разные по форме
    -- (POS+case+number, либо POS+verb_class+tense, либо конкретное правило сандхи)
                                        match_upos      TEXT,
                                        match_xpos      TEXT,
                                        match_feats     JSONB,           -- частичное совпадение: {"Case": "Nom", "Number": "Plur"}
                                        priority        SMALLINT DEFAULT 0  -- на случай пересечения правил — какое побеждает
);
CREATE INDEX idx_lesson_mapping_feats ON lesson_grammar_mapping USING GIN (match_feats);

-- Материализованная связь токен → урок (пересчитывается батчем при импорте/изменении mapping)
CREATE TABLE token_lesson (
                              token_id        BIGINT NOT NULL REFERENCES corpus_token(id),
                              lesson_id       BIGINT NOT NULL REFERENCES lesson(id),
                              PRIMARY KEY (token_id, lesson_id)
);
CREATE INDEX idx_token_lesson_lesson ON token_lesson(lesson_id);

-- ===================================================================
-- 4. ПРИМЕРЫ ДЛЯ КВИЗОВ/ПРАВИЛ — материализованный top-N (пайплайн из прошлого ответа)
-- ===================================================================

CREATE TABLE example_sentence (
                                  id              BIGSERIAL PRIMARY KEY,
    -- ключ примера — либо урок, либо конкретная лемма (для лексического квиза)
                                  lesson_id       BIGINT REFERENCES lesson(id),
                                  lemma_id        BIGINT REFERENCES corpus_lemma(id),
                                  sentence_id     BIGINT NOT NULL REFERENCES corpus_sentence(id),
                                  rank            SMALLINT NOT NULL,           -- 1..N — предпосчитанный приоритет
                                  unknown_lemma_count SMALLINT,                -- сколько "незнакомых" лемм на момент пересчёта (усреднённо)
                                  token_count     SMALLINT,                    -- длина предложения — для быстрой фильтрации по "коротко"
                                  CHECK (lesson_id IS NOT NULL OR lemma_id IS NOT NULL)
);
CREATE INDEX idx_example_lesson ON example_sentence(lesson_id, rank);
CREATE INDEX idx_example_lemma ON example_sentence(lemma_id, rank);

-- ===================================================================
-- 5. ТВОЙ РУЧНОЙ АНАЛИЗ (LLM/Vidyut) — там, где DCS не покрывает / нужен перевод
-- ===================================================================

CREATE TABLE sentence_analysis (
                                   sentence_id     BIGINT PRIMARY KEY REFERENCES corpus_sentence(id),
                                   translation_ru  TEXT,
                                   analysis_status TEXT NOT NULL DEFAULT 'dcs_only',
    -- 'dcs_only' | 'translated' | 'llm_reviewed' | 'manual_verified'
                                   analyzed_by     TEXT,             -- 'llm:claude-x' | 'vidyut' | 'human:username'
                                   analyzed_at     TIMESTAMPTZ,
                                   sandhi_breakdown JSONB             -- [{form, split, rule_id}, ...] — как на твоём скриншоте
);