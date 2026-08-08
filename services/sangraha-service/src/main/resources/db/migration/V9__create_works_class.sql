-- Классификатор произведений (works_class): иерархия категорий с id→parent_id.
-- Таблица уже создана в базе (данные существуют), поэтому миграция идемпотентна.
CREATE TABLE IF NOT EXISTS "sangraha"."works_class" (
    "id" uuid NOT NULL,
    "parent_id" uuid,
    "classification" text NOT NULL,
    "code" text NOT NULL,
    "title_sa_iast" text NOT NULL,
    "title_sa_deva" text,
    "title_ru" text NOT NULL,
    "title_en" text NOT NULL,
    "sort_order" int4 NOT NULL DEFAULT 0,
    CONSTRAINT "works_class_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "works_class_parent_id_fkey" FOREIGN KEY ("parent_id")
        REFERENCES "sangraha"."works_class" ("id") ON DELETE CASCADE,
    CONSTRAINT "works_class_code_key" UNIQUE ("code")
);

-- Связь произведение ↔ категория (many-to-many): произведение может быть
-- в нескольких категориях классификатора.
CREATE TABLE IF NOT EXISTS "sangraha"."works_work_class" (
    "work_id" uuid NOT NULL,
    "class_id" uuid NOT NULL,
    CONSTRAINT "works_work_class_pkey" PRIMARY KEY ("work_id", "class_id"),
    CONSTRAINT "works_work_class_class_id_fkey" FOREIGN KEY ("class_id")
        REFERENCES "sangraha"."works_class" ("id") ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT "works_work_class_work_id_fkey" FOREIGN KEY ("work_id")
        REFERENCES "sangraha"."works" ("id") ON DELETE CASCADE ON UPDATE NO ACTION
);