-- lemma_statistics: полный сброс + фикс уникальности для null-рода.
--
-- UNIQUE(lemma_id, gender) был с дефолтным NULLS DISTINCT, поэтому строки
-- (lemma_id, NULL) никогда не дедуплицировались: каждый запуск refresh-statistics
-- вставлял новую строку для лемм без рода (союзы/частицы/наречия), накапливая
-- дубликаты в выдаче lemmas/export.
--
-- 1. Полная очистка таблицы (пересчитывается через POST /lemmas/refresh-statistics).
-- 2. Уникальность пересоздаётся как NULLS NOT DISTINCT, чтобы ON CONFLICT
--    дедуплицировал и строки без рода.

DELETE FROM sangraha.lemma_statistics;

ALTER TABLE sangraha.lemma_statistics
    DROP CONSTRAINT IF EXISTS uq_lemma_statistics_lemma_gender;

ALTER TABLE sangraha.lemma_statistics
    ADD CONSTRAINT uq_lemma_statistics_lemma_gender
    UNIQUE NULLS NOT DISTINCT (lemma_id, gender);
