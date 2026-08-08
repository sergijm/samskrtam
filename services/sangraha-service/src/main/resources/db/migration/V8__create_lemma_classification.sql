-- Классификация лексем (lemma-classification.md §1.6–§1.7).
-- lemma_classification — одна строка на (lemma, scheme); UNIQUE(lemma_id, scheme_code) —
-- повторный run апдейтит существующую строку, дублей нет.
-- classification_run / classification_batch — метаданные прогона и одного LLM-вызова;
-- неудача батча помечает его FAILED, остальные батчи обрабатываются дальше.
BEGIN;

CREATE TABLE "sangraha"."classification_run" (
    "id" uuid NOT NULL DEFAULT gen_random_uuid(),
    "scheme_code" varchar(20) NOT NULL,
    "requested_batch_count" int4 NOT NULL,
    "completed_batch_count" int4 NOT NULL DEFAULT 0,
    "status" varchar(20) NOT NULL,
    "requested_by" varchar(100),
    "created_at" timestamptz(6) NOT NULL DEFAULT now(),
    "completed_at" timestamptz(6),
    CONSTRAINT "pk_classification_run" PRIMARY KEY ("id"),
    CONSTRAINT "fk_classification_run_scheme" FOREIGN KEY ("scheme_code")
        REFERENCES "sangraha"."classification_scheme" ("code"),
    CONSTRAINT "ck_classification_run_status" CHECK (
        status IN ('RUNNING', 'COMPLETED', 'COMPLETED_WITH_ERRORS'))
);

CREATE TABLE "sangraha"."classification_batch" (
    "id" uuid NOT NULL DEFAULT gen_random_uuid(),
    "scheme_code" varchar(20) NOT NULL,
    "run_id" uuid NOT NULL,
    "batch_index" int4 NOT NULL,
    "lemma_count" int4 NOT NULL,
    "status" varchar(20) NOT NULL,
    "error_message" text,
    "llm_model" varchar(100) NOT NULL,
    "created_at" timestamptz(6) NOT NULL DEFAULT now(),
    "completed_at" timestamptz(6),
    CONSTRAINT "pk_classification_batch" PRIMARY KEY ("id"),
    CONSTRAINT "fk_classification_batch_scheme" FOREIGN KEY ("scheme_code")
        REFERENCES "sangraha"."classification_scheme" ("code"),
    CONSTRAINT "fk_classification_batch_run" FOREIGN KEY ("run_id")
        REFERENCES "sangraha"."classification_run" ("id") ON DELETE CASCADE
);

CREATE INDEX "idx_classification_batch_run" ON "sangraha"."classification_batch" ("run_id");

CREATE TABLE "sangraha"."lemma_classification" (
    "id" uuid NOT NULL DEFAULT gen_random_uuid(),
    "lemma_id" uuid NOT NULL,
    "scheme_code" varchar(20) NOT NULL,
    "category_code" varchar(40),
    "gloss_ru" varchar(500),
    "gloss_en" varchar(500),
    "confidence" smallint,
    "status" varchar(20) NOT NULL DEFAULT 'CANDIDATE',
    "llm_model" varchar(100),
    "batch_id" uuid,
    "reviewed_by" varchar(100),
    "reviewed_at" timestamptz(6),
    "created_at" timestamptz(6) NOT NULL DEFAULT now(),
    "updated_at" timestamptz(6) NOT NULL DEFAULT now(),
    CONSTRAINT "pk_lemma_classification" PRIMARY KEY ("id"),
    CONSTRAINT "uq_lemma_classification_lemma_scheme" UNIQUE ("lemma_id", "scheme_code"),
    CONSTRAINT "fk_lemma_classification_lemma" FOREIGN KEY ("lemma_id")
        REFERENCES "sangraha"."lemma" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_lemma_classification_scheme" FOREIGN KEY ("scheme_code")
        REFERENCES "sangraha"."classification_scheme" ("code"),
    CONSTRAINT "fk_lemma_classification_batch" FOREIGN KEY ("batch_id")
        REFERENCES "sangraha"."classification_batch" ("id"),
    CONSTRAINT "ck_lemma_classification_status" CHECK (
        status IN ('CANDIDATE', 'APPROVED', 'REJECTED'))
);

CREATE INDEX "idx_lemma_classification_scheme" ON "sangraha"."lemma_classification" ("scheme_code");
CREATE INDEX "idx_lemma_classification_lemma" ON "sangraha"."lemma_classification" ("lemma_id");

COMMIT;