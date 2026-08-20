-- Удаление noun_stems (verse-word-grammar.md §1а): таблица замещена nominal_lemmas
-- (V5), JPA-сущность/репозиторий удалены из кода. Данные уже мигрированы в
-- nominal_lemmas, поэтому таблица больше не нужна.
DROP TABLE "sangraha"."noun_stems";