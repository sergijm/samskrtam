-- Поиск примеров стихов по точной словоформе (POST /api/v1/sangraha/words/examples)
-- фильтрует verse_words по surface_iast IN (...). Точных совпадений в корпусе
-- мало, но без индекса запрос получает seq scan по всей таблице.
CREATE INDEX "idx_verse_words_surface_iast" ON "sangraha"."verse_words" ("surface_iast");
