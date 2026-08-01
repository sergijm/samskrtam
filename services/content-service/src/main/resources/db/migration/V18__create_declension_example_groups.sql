-- Таблица кэша verseId[] по группам (vowel_type, gender, case_type, number_type)
-- для вкладки «Примеры» на странице шага склонений (content-service.md §12).
-- Пустой verse_ids — валидный результат («искали, ничего не нашли»).
CREATE TABLE IF NOT EXISTS content.declension_example_groups (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vowel_type        VARCHAR NOT NULL,
    gender            VARCHAR NOT NULL,
    case_type         VARCHAR NOT NULL,
    number_type       VARCHAR NOT NULL,
    verse_ids         JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (vowel_type, gender, case_type, number_type)
);