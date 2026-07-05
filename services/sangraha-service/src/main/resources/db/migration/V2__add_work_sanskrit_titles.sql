-- Add Sanskrit title fields to works table
ALTER TABLE "sangraha"."works"
    ADD COLUMN "title_sa_iast" varchar(255) COLLATE "pg_catalog"."default",
    ADD COLUMN "title_sa_devanagari" varchar(255) COLLATE "pg_catalog"."default";