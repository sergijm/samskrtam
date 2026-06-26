-- Агрегированная статистика пользователя по каждому слову лексического квиза.
-- Обновляется через UPSERT при каждом ответе в сессии типа VOCABULARY.
-- Агрегирует данные по всем квизам, в которых встречается данное слово.
CREATE TABLE quiz.word_statistics (
id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
user_id             UUID        NOT NULL,
vocabulary_word_id  UUID        NOT NULL,
total_attempts      INT         NOT NULL DEFAULT 0,
correct_answers     INT         NOT NULL DEFAULT 0,
last_seen_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
CONSTRAINT pk_word_statistics PRIMARY KEY (id),
CONSTRAINT uq_word_statistics_user_word UNIQUE (user_id, vocabulary_word_id)
);
CREATE INDEX idx_word_statistics_user_id
ON quiz.word_statistics (user_id);
CREATE INDEX idx_word_statistics_user_words
ON quiz.word_statistics (user_id, vocabulary_word_id);