-- Уникальность словаря не только по SLP1, но и по тексту леммы в IAST:
-- на одну и ту же IAST-запись не может быть больше одной строки lemma
-- (lemma-classification.md §1.1). Группировка уже идёт по lemma_slp1
-- (детерминированная конверсия), UNIQUE(iast) — жёсткая защита от дублей.
BEGIN;

ALTER TABLE "sangraha"."lemma"
    ADD CONSTRAINT "uq_lemma_iast" UNIQUE ("lemma_iast");

COMMIT;