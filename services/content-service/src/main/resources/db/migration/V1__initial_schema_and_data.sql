-- V1__initial_schema_and_data.sql
-- Объединенная миграция, включающая начальную схему, таблицы для сгенерированных вопросов и данные.

-- 1. Создание схемы
CREATE SCHEMA IF NOT EXISTS content;

-- 2. Создание таблицы quizzes
CREATE TABLE content.quizzes (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug                  VARCHAR(50)  NOT NULL UNIQUE,
    title_ru              VARCHAR(255) NOT NULL,
    title_en              VARCHAR(255) NOT NULL,
    description_ru        VARCHAR(500),
    description_en        VARCHAR(500),
    quiz_type             VARCHAR(50)  NOT NULL,
    difficulty            VARCHAR(20)  NOT NULL DEFAULT 'BEGINNER',
    questions_per_session INT          NOT NULL DEFAULT 10,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at            TIMESTAMPTZ,
    CONSTRAINT ck_quiz_type    CHECK (quiz_type IN ('DECLENSIONS','A_STEM_DECLENSIONS','AA_STEM_DECLENSIONS','I_STEM_DECLENSIONS','II_STEM_DECLENSIONS','U_STEM_DECLENSIONS','UU_STEM_DECLENSIONS','R_STEM_DECLENSIONS','CONJUGATIONS','VOCABULARY')),
    CONSTRAINT ck_difficulty   CHECK (difficulty IN ('BEGINNER','INTERMEDIATE','ADVANCED')),
    CONSTRAINT ck_slug_format  CHECK (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$')
);

-- 3. Создание таблицы questions
CREATE TABLE content.questions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id           UUID NOT NULL REFERENCES content.quizzes(id),
    text_ru           TEXT NOT NULL,
    text_en           TEXT NOT NULL,
    explanation_ru    TEXT NOT NULL,
    explanation_en    TEXT NOT NULL,
    correct_option_id UUID,
    declension_stem_id UUID,
    target_case       VARCHAR(20),
    target_number     VARCHAR(20),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ
);

-- 4. Создание таблицы question_options
CREATE TABLE content.question_options (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES content.questions(id),
    form_iast   VARCHAR(255) NOT NULL,
    form_devanagari VARCHAR(255)
);

-- 5. Создание таблицы declension_stems
CREATE TABLE content.declension_stems (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stem_name_iast          VARCHAR(50) NOT NULL UNIQUE,
    stem_name_devanagari    VARCHAR(50),
    vowel_type              VARCHAR(20) NOT NULL,
    gender                  VARCHAR(20) NOT NULL,
    CONSTRAINT ck_vowel_type CHECK (vowel_type IN ('A_STEM', 'AA_STEM', 'I_STEM', 'II_STEM', 'U_STEM', 'UU_STEM', 'R_STEM')),
    CONSTRAINT ck_gender CHECK (gender IN ('MASCULINE', 'FEMININE', 'NEUTER', 'UNKNOWN'))
);

-- 6. Создание таблицы declension_forms
CREATE TABLE content.declension_forms (
    declension_stem_id      UUID NOT NULL REFERENCES content.declension_stems(id),
    case_type               VARCHAR(20) NOT NULL,
    number_type             VARCHAR(20) NOT NULL,
    form_iast               VARCHAR(50) NOT NULL,
    form_devanagari         VARCHAR(50),
    PRIMARY KEY (declension_stem_id, case_type, number_type),
    CONSTRAINT ck_case_type CHECK (case_type IN ('NOMINATIVE', 'ACCUSATIVE', 'INSTRUMENTAL', 'DATIVE', 'ABLATIVE', 'GENITIVE', 'LOCATIVE', 'VOCATIVE')),
    CONSTRAINT ck_number_type CHECK (number_type IN ('SINGULAR', 'DUAL', 'PLURAL'))
);

-- 7. Создание таблицы vocabulary_words (обновленная структура из V4)
CREATE TABLE content.vocabulary_words (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    word_iast        VARCHAR(255) NOT NULL,
    word_devanagari  VARCHAR(255) NOT NULL,
    translation_ru   VARCHAR(500) NOT NULL,
    translation_en   VARCHAR(500) NOT NULL,
    gender           VARCHAR(20) NOT NULL,
    stem             VARCHAR(255) NOT NULL,
    root             VARCHAR(255),
    explanation_ru   VARCHAR(500) NOT NULL,
    explanation_en   VARCHAR(500) NOT NULL,
    created_at       TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_vocabulary_gender CHECK (gender IN ('MASCULINE', 'FEMININE', 'NEUTER', 'UNKNOWN'))
);

-- 8. Создание таблицы generated_quiz_data
CREATE TABLE content.generated_quiz_data
(
    id                      UUID PRIMARY KEY,
    quiz_id                 UUID                        NOT NULL,
    user_locale             VARCHAR(10)                 NOT NULL,
    generated_at            TIMESTAMP WITH TIME ZONE    NOT NULL,
    vocabulary_words_json   TEXT,
    CONSTRAINT fk_generated_quiz_data_quiz_id FOREIGN KEY (quiz_id) REFERENCES content.quizzes (id)
);

-- 9. Создание таблицы generated_questions
CREATE TABLE content.generated_questions
(
    id                          UUID PRIMARY KEY,
    generated_quiz_data_id      UUID                        NOT NULL,
    quiz_id                     UUID                        NOT NULL,
    text                        TEXT,
    explanation_ru              TEXT,
    explanation_en              TEXT,
    declension_stem_id          UUID,
    target_case                 VARCHAR(255),
    target_number               VARCHAR(255),
    correct_form_iast           VARCHAR(255),
    correct_form_devanagari     VARCHAR(255),
    vocabulary_word_id          UUID,
    question_source_language    VARCHAR(255),
    question_target_language    VARCHAR(255),
    correct_translation_ru      TEXT,
    correct_translation_en      TEXT,
    user_locale                 VARCHAR(10),
    CONSTRAINT fk_generated_questions_generated_quiz_data_id FOREIGN KEY (generated_quiz_data_id) REFERENCES content.generated_quiz_data (id),
    CONSTRAINT fk_generated_questions_quiz_id FOREIGN KEY (quiz_id) REFERENCES content.quizzes (id),
    CONSTRAINT fk_generated_questions_declension_stem_id FOREIGN KEY (declension_stem_id) REFERENCES content.declension_stems (id),
    CONSTRAINT fk_generated_questions_vocabulary_word_id FOREIGN KEY (vocabulary_word_id) REFERENCES content.vocabulary_words (id)
);

-- 10. Создание таблицы vocabulary_categories
CREATE TABLE content.vocabulary_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    parent_id UUID,
    name_ru VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    description_ru TEXT,
    description_en TEXT,
    CONSTRAINT fk_vocabulary_categories_parent
        FOREIGN KEY (parent_id)
            REFERENCES content.vocabulary_categories(id)
            ON DELETE RESTRICT
);

-- 11. Создание таблицы vocabulary_word_categories
CREATE TABLE content.vocabulary_word_categories (
    vocabulary_word_id UUID NOT NULL,
    category_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (
                 vocabulary_word_id,
                 category_id
        ),
    CONSTRAINT fk_vwc_word
        FOREIGN KEY (vocabulary_word_id)
            REFERENCES content.vocabulary_words(id)
            ON DELETE CASCADE,
    CONSTRAINT fk_vwc_category
        FOREIGN KEY (category_id)
            REFERENCES content.vocabulary_categories(id)
            ON DELETE CASCADE
);

-- Комментарии к таблицам и колонкам
COMMENT ON TABLE content.quizzes IS 'Таблица для хранения информации о квизах';
COMMENT ON COLUMN content.quizzes.id IS 'Уникальный идентификатор квиза';
COMMENT ON COLUMN content.quizzes.slug IS 'Уникальный читаемый идентификатор квиза для URL';
COMMENT ON COLUMN content.quizzes.title_ru IS 'Название квиза на русском языке';
COMMENT ON COLUMN content.quizzes.title_en IS 'Название квиза на английском языке';
COMMENT ON COLUMN content.quizzes.description_ru IS 'Описание квиза на русском языке';
COMMENT ON COLUMN content.quizzes.description_en IS 'Описание квиза на английском языке';
COMMENT ON COLUMN content.quizzes.quiz_type IS 'Тип квиза (например, склонения, спряжения, лексика)';
COMMENT ON COLUMN content.quizzes.difficulty IS 'Уровень сложности квиза';
COMMENT ON COLUMN content.quizzes.questions_per_session IS 'Количество вопросов в одной сессии квиза';
COMMENT ON COLUMN content.quizzes.created_at IS 'Дата и время создания записи';
COMMENT ON COLUMN content.quizzes.deleted_at IS 'Дата и время удаления записи (для мягкого удаления)';

COMMENT ON TABLE content.questions IS 'Таблица для хранения вопросов квизов';
COMMENT ON COLUMN content.questions.id IS 'Уникальный идентификатор вопроса';
COMMENT ON COLUMN content.questions.quiz_id IS 'Идентификатор квиза, к которому относится вопрос';
COMMENT ON COLUMN content.questions.text_ru IS 'Текст вопроса на русском языке';
COMMENT ON COLUMN content.questions.text_en IS 'Текст вопроса на английском языке';
COMMENT ON COLUMN content.questions.explanation_ru IS 'Объяснение к вопросу на русском языке';
COMMENT ON COLUMN content.questions.explanation_en IS 'Объяснение к вопросу на английском языке';
COMMENT ON COLUMN content.questions.correct_option_id IS 'Идентификатор правильного варианта ответа';
COMMENT ON COLUMN content.questions.declension_stem_id IS 'Идентификатор основы склонения (для квизов по склонениям)';
COMMENT ON COLUMN content.questions.target_case IS 'Цележный падеж (для квизов по склонениям)';
COMMENT ON COLUMN content.questions.target_number IS 'Целевое число (для квизов по склонениям)';
COMMENT ON COLUMN content.questions.created_at IS 'Дата и время создания записи';
COMMENT ON COLUMN content.questions.deleted_at IS 'Дата и время удаления записи (для мягкого удаления)';

COMMENT ON TABLE content.question_options IS 'Таблица для хранения вариантов ответов к вопросам';
COMMENT ON COLUMN content.question_options.id IS 'Уникальный идентификатор варианта ответа';
COMMENT ON COLUMN content.question_options.question_id IS 'Идентификатор вопроса, к которому относится вариант ответа';
COMMENT ON COLUMN content.question_options.form_iast IS 'Форма слова в IAST';
COMMENT ON COLUMN content.question_options.form_devanagari IS 'Форма слова в деванагари';

COMMENT ON TABLE content.vocabulary_words IS 'Таблица для хранения словарных слов';
COMMENT ON COLUMN content.vocabulary_words.id IS 'Уникальный идентификатор словарного слова';
COMMENT ON COLUMN content.vocabulary_words.word_iast IS 'Слово в IAST';
COMMENT ON COLUMN content.vocabulary_words.word_devanagari IS 'Слово в деванагари';
COMMENT ON COLUMN content.vocabulary_words.translation_ru IS 'Перевод слова на русский язык';
COMMENT ON COLUMN content.vocabulary_words.translation_en IS 'Перевод слова на английский язык';
COMMENT ON COLUMN content.vocabulary_words.gender IS 'Грамматический род слова';
COMMENT ON COLUMN content.vocabulary_words.stem IS 'Основа слова';
COMMENT ON COLUMN content.vocabulary_words.root IS 'Корень слова';
COMMENT ON COLUMN content.vocabulary_words.created_at IS 'Дата и время создания записи';
COMMENT ON COLUMN content.vocabulary_words.updated_at IS 'Дата и время последнего обновления записи';

COMMENT ON TABLE content.declension_stems IS 'Таблица для хранения основ склонений';
COMMENT ON COLUMN content.declension_stems.id IS 'Уникальный идентификатор основы склонения';
COMMENT ON COLUMN content.declension_stems.stem_name_iast IS 'Название основы в IAST';
COMMENT ON COLUMN content.declension_stems.stem_name_devanagari IS 'Название основы в деванагари';
COMMENT ON COLUMN content.declension_stems.vowel_type IS 'Тип гласной основы';
COMMENT ON COLUMN content.declension_stems.gender IS 'Грамматический род основы';

COMMENT ON TABLE content.declension_forms IS 'Таблица для хранения форм склонений';
COMMENT ON COLUMN content.declension_forms.declension_stem_id IS 'Идентификатор основы склонения';
COMMENT ON COLUMN content.declension_forms.case_type IS 'Тип падежа';
COMMENT ON COLUMN content.declension_forms.number_type IS 'Тип числа';
COMMENT ON COLUMN content.declension_forms.form_iast IS 'Форма слова в IAST';
COMMENT ON COLUMN content.declension_forms.form_devanagari IS 'Форма слова в деванагари';

-- Добавление оставшихся внешних ключей и ограничений для таблицы questions
ALTER TABLE content.questions
    ADD CONSTRAINT fk_declension_stem
        FOREIGN KEY (declension_stem_id) REFERENCES content.declension_stems(id),
    ADD CONSTRAINT ck_question_target_case CHECK (target_case IN ('NOMINATIVE', 'ACCUSATIVE', 'INSTRUMENTAL', 'DATIVE', 'ABLATIVE', 'GENITIVE', 'LOCATIVE', 'VOCATIVE')),
    ADD CONSTRAINT ck_question_target_number CHECK (target_number IN ('SINGULAR', 'DUAL', 'PLURAL'));

ALTER TABLE content.questions
    ADD CONSTRAINT fk_correct_option
    FOREIGN KEY (correct_option_id) REFERENCES content.question_options(id);

-- ALTER TABLE для generated_questions (перемещено после создания таблицы)
ALTER TABLE content.generated_questions
    ADD COLUMN stem VARCHAR(255),
    ADD COLUMN case_type VARCHAR(255),
    ADD COLUMN number_type VARCHAR(255),
    ADD COLUMN question_number INT NOT NULL DEFAULT 0;

-- Индексы из V4
CREATE INDEX idx_vwc_word
    ON content.vocabulary_word_categories(vocabulary_word_id);

CREATE INDEX idx_vwc_category
    ON content.vocabulary_word_categories(category_id);

CREATE INDEX idx_vocabulary_words_iast
    ON content.vocabulary_words(word_iast);

CREATE INDEX idx_vocabulary_words_devanagari
    ON content.vocabulary_words(word_devanagari);

CREATE INDEX idx_vocabulary_words_root
    ON content.vocabulary_words(root);
