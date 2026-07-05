-- Add Sanskrit title fields to chapters table
-- Also make order_index nullable at DB level (backend computes default if not provided)
ALTER TABLE "sangraha"."chapters"
    ADD COLUMN "title_sa_iast" varchar(255) COLLATE "pg_catalog"."default",
    ADD COLUMN "title_sa_devanagari" varchar(255) COLLATE "pg_catalog"."default";

ALTER TABLE "sangraha"."chapters"
    ALTER COLUMN "order_index" DROP NOT NULL;