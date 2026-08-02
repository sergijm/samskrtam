-- Источники текстов: откуда взят корпус произведения.
-- DCS = Digital Corpus of Sanskrit (https://www.sanskrit-linguistics.org/dcs/).
CREATE TABLE "sangraha"."sources" (
    "id" uuid NOT NULL DEFAULT gen_random_uuid(),
    "code" varchar(20) NOT NULL,
    "title_en" varchar(255) NOT NULL,
    "title_ru" varchar(255) NOT NULL,
    CONSTRAINT "pk_sources" PRIMARY KEY ("id"),
    CONSTRAINT "uq_sources_code" UNIQUE ("code")
);

INSERT INTO "sangraha"."sources" ("code", "title_en", "title_ru")
VALUES ('DCS', 'Digital Corpus of Sanskrit', 'Цифровой корпус санскрита');

-- Привязка произведений к источнику
ALTER TABLE "sangraha"."works" ADD COLUMN "source_id" uuid;

UPDATE "sangraha"."works"
SET "source_id" = (SELECT "id" FROM "sangraha"."sources" WHERE "code" = 'DCS')
WHERE "source_id" IS NULL;

ALTER TABLE "sangraha"."works" ALTER COLUMN "source_id" SET NOT NULL;

ALTER TABLE "sangraha"."works" ADD CONSTRAINT "fk_works_source"
    FOREIGN KEY ("source_id") REFERENCES "sangraha"."sources" ("id");

CREATE INDEX "idx_works_source_id" ON "sangraha"."works" ("source_id");
