/*
 Navicat Premium Data Transfer

 Source Server         : mdm-dev-vm
 Source Server Type    : PostgreSQL
 Source Server Version : 170009 (170009)
 Source Host           : mdm-dev:5432
 Source Catalog        : samskrtam
 Source Schema         : cologne_mw

 Target Server Type    : PostgreSQL
 Target Server Version : 170009 (170009)
 File Encoding         : 65001

 Date: 23/06/2026 06:09:56
*/


-- ----------------------------
-- Sequence structure for abbreviation_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_mw"."abbreviation_id_seq";
CREATE SEQUENCE "cologne_mw"."abbreviation_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for biological_name_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_mw"."biological_name_id_seq";
CREATE SEQUENCE "cologne_mw"."biological_name_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for div_marker_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_mw"."div_marker_id_seq";
CREATE SEQUENCE "cologne_mw"."div_marker_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for entry_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_mw"."entry_id_seq";
CREATE SEQUENCE "cologne_mw"."entry_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for entry_relation_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_mw"."entry_relation_id_seq";
CREATE SEQUENCE "cologne_mw"."entry_relation_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for foreign_word_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_mw"."foreign_word_id_seq";
CREATE SEQUENCE "cologne_mw"."foreign_word_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for homonym_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_mw"."homonym_id_seq";
CREATE SEQUENCE "cologne_mw"."homonym_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for info_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_mw"."info_id_seq";
CREATE SEQUENCE "cologne_mw"."info_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for lexcat_info_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_mw"."lexcat_info_id_seq";
CREATE SEQUENCE "cologne_mw"."lexcat_info_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for lexical_info_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_mw"."lexical_info_id_seq";
CREATE SEQUENCE "cologne_mw"."lexical_info_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for literary_source_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_mw"."literary_source_id_seq";
CREATE SEQUENCE "cologne_mw"."literary_source_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for page_break_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_mw"."page_break_id_seq";
CREATE SEQUENCE "cologne_mw"."page_break_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for sanskrit_word_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_mw"."sanskrit_word_id_seq";
CREATE SEQUENCE "cologne_mw"."sanskrit_word_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for westergaard_link_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_mw"."westergaard_link_id_seq";
CREATE SEQUENCE "cologne_mw"."westergaard_link_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for whitney_link_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_mw"."whitney_link_id_seq";
CREATE SEQUENCE "cologne_mw"."whitney_link_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Table structure for abbreviation
-- ----------------------------
DROP TABLE IF EXISTS "cologne_mw"."abbreviation";
CREATE TABLE "cologne_mw"."abbreviation" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_mw".abbreviation_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "abbrev_text" text COLLATE "pg_catalog"."default" NOT NULL,
  "expansion" text COLLATE "pg_catalog"."default",
  "slp1_spelling" text COLLATE "pg_catalog"."default",
  "position_order" int4,
  "created_at" timestamp(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for biological_name
-- ----------------------------
DROP TABLE IF EXISTS "cologne_mw"."biological_name";
CREATE TABLE "cologne_mw"."biological_name" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_mw".biological_name_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "name_type" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
  "name_text" text COLLATE "pg_catalog"."default" NOT NULL,
  "position_order" int4,
  "created_at" timestamp(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for div_marker
-- ----------------------------
DROP TABLE IF EXISTS "cologne_mw"."div_marker";
CREATE TABLE "cologne_mw"."div_marker" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_mw".div_marker_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "div_type" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
  "position_order" int4,
  "created_at" timestamp(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for entry
-- ----------------------------
DROP TABLE IF EXISTS "cologne_mw"."entry";
CREATE TABLE "cologne_mw"."entry" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_mw".entry_id_seq'::regclass),
  "record_id_full" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "page" int4 NOT NULL,
  "column_num" int4 NOT NULL,
  "key1" text COLLATE "pg_catalog"."default" NOT NULL,
  "key2" text COLLATE "pg_catalog"."default" NOT NULL,
  "homonym_num" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
  "e_code" varchar(5) COLLATE "pg_catalog"."default" NOT NULL,
  "body" text COLLATE "pg_catalog"."default",
  "is_supplement" bool DEFAULT false,
  "created_at" timestamp(6) DEFAULT now(),
  "record_id_numeric" int4,
  "main_definition" text COLLATE "pg_catalog"."default",
  "short_definition" varchar(300) COLLATE "pg_catalog"."default",
  "part_of_speech" varchar(50) COLLATE "pg_catalog"."default",
  "gender" varchar(20) COLLATE "pg_catalog"."default",
  "key1_normalized" text COLLATE "pg_catalog"."default" GENERATED ALWAYS AS (
regexp_replace(key1, '[^a-zA-Z]'::text, ''::text, 'g'::text)
) STORED,
  "key1_iast" text COLLATE "pg_catalog"."default",
  "key1_iast_plain" text COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "cologne_mw"."entry"."record_id_full" IS 'Оригинальный Cologne record ID (может содержать точку, например "1.1")';
COMMENT ON COLUMN "cologne_mw"."entry"."record_id_numeric" IS 'Числовая часть record_id_full (для сортировки)';

-- ----------------------------
-- Table structure for entry_relation
-- ----------------------------
DROP TABLE IF EXISTS "cologne_mw"."entry_relation";
CREATE TABLE "cologne_mw"."entry_relation" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_mw".entry_relation_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "related_entry_id" int4 NOT NULL,
  "relation_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "extra_data" text COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for foreign_word
-- ----------------------------
DROP TABLE IF EXISTS "cologne_mw"."foreign_word";
CREATE TABLE "cologne_mw"."foreign_word" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_mw".foreign_word_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "lang_type" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "language_name" varchar(50) COLLATE "pg_catalog"."default",
  "script" varchar(20) COLLATE "pg_catalog"."default",
  "word_text" text COLLATE "pg_catalog"."default" NOT NULL,
  "position_order" int4,
  "created_at" timestamp(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for homonym
-- ----------------------------
DROP TABLE IF EXISTS "cologne_mw"."homonym";
CREATE TABLE "cologne_mw"."homonym" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_mw".homonym_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "homonym_number" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
  "homonym_text" text COLLATE "pg_catalog"."default",
  "position_order" int4,
  "created_at" timestamp(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for info
-- ----------------------------
DROP TABLE IF EXISTS "cologne_mw"."info";
CREATE TABLE "cologne_mw"."info" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_mw".info_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "info_type" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "info_value" text COLLATE "pg_catalog"."default",
  "verb_cp" text COLLATE "pg_catalog"."default",
  "verb_parse" text COLLATE "pg_catalog"."default",
  "westergaard_root" text COLLATE "pg_catalog"."default",
  "westergaard_section" text COLLATE "pg_catalog"."default",
  "westergaard_sayana_ref" text COLLATE "pg_catalog"."default",
  "whitney_root" text COLLATE "pg_catalog"."default",
  "whitney_page" text COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for lexcat_info
-- ----------------------------
DROP TABLE IF EXISTS "cologne_mw"."lexcat_info";
CREATE TABLE "cologne_mw"."lexcat_info" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_mw".lexcat_info_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "lexcat_value" text COLLATE "pg_catalog"."default" NOT NULL,
  "lex_id" varchar(50) COLLATE "pg_catalog"."default",
  "stem" text COLLATE "pg_catalog"."default",
  "root_class" text COLLATE "pg_catalog"."default",
  "inflect_id" text COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for lexical_info
-- ----------------------------
DROP TABLE IF EXISTS "cologne_mw"."lexical_info";
CREATE TABLE "cologne_mw"."lexical_info" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_mw".lexical_info_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "lex_type" varchar(20) COLLATE "pg_catalog"."default",
  "gender_standard" text COLLATE "pg_catalog"."default",
  "gender_raw" text COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for literary_source
-- ----------------------------
DROP TABLE IF EXISTS "cologne_mw"."literary_source";
CREATE TABLE "cologne_mw"."literary_source" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_mw".literary_source_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "source_ref" text COLLATE "pg_catalog"."default" NOT NULL,
  "position_order" int4,
  "created_at" timestamp(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for page_break
-- ----------------------------
DROP TABLE IF EXISTS "cologne_mw"."page_break";
CREATE TABLE "cologne_mw"."page_break" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_mw".page_break_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "page" int4 NOT NULL,
  "column_num" int2 NOT NULL,
  "position_order" int4,
  "created_at" timestamp(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for sanskrit_word
-- ----------------------------
DROP TABLE IF EXISTS "cologne_mw"."sanskrit_word";
CREATE TABLE "cologne_mw"."sanskrit_word" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_mw".sanskrit_word_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "slp1_spelling" text COLLATE "pg_catalog"."default" NOT NULL,
  "iast_spelling" text COLLATE "pg_catalog"."default",
  "is_primary_headword" bool DEFAULT false,
  "position_order" int4,
  "created_at" timestamp(6) DEFAULT now(),
  "slp1_normalized" text COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for westergaard_link
-- ----------------------------
DROP TABLE IF EXISTS "cologne_mw"."westergaard_link";
CREATE TABLE "cologne_mw"."westergaard_link" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_mw".westergaard_link_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "root_slp1" text COLLATE "pg_catalog"."default" NOT NULL,
  "section_item" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "sayana_ref" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "created_at" timestamp(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for whitney_link
-- ----------------------------
DROP TABLE IF EXISTS "cologne_mw"."whitney_link";
CREATE TABLE "cologne_mw"."whitney_link" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_mw".whitney_link_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "root_slp1" text COLLATE "pg_catalog"."default" NOT NULL,
  "page" int4 NOT NULL,
  "created_at" timestamp(6) DEFAULT now()
)
;

-- ----------------------------
-- Function structure for clean_translation
-- ----------------------------
DROP FUNCTION IF EXISTS "cologne_mw"."clean_translation"("input_text" text);
CREATE OR REPLACE FUNCTION "cologne_mw"."clean_translation"("input_text" text)
  RETURNS "pg_catalog"."text" AS $BODY$
DECLARE
    result TEXT := input_text;
    words TEXT[];
    cleaned_words TEXT[];
    w TEXT;
BEGIN
    IF input_text IS NULL OR input_text = '' THEN
        RETURN NULL;
    END IF;
    
    -- 1. Удаляем всё содержимое в скобках [] и ()
    --    Убираем все круглые скобки и их содержимое
    result := regexp_replace(result, '\([^()]*\)', '', 'g');
    --    Убираем все квадратные скобки и их содержимое
    result := regexp_replace(result, '\[[^\[\]]*\]', '', 'g');
    
    -- 2. Убираем все пунктуационные символы (оставляем только буквы и пробелы)
    result := regexp_replace(result, '[^a-zA-Z ]', ' ', 'g');
    
    -- 3. Разбиваем на слова
    words := regexp_split_to_array(result, '\s+');
    
    -- 4. Оставляем только слова, состоящие ИСКЛЮЧИТЕЛЬНО из a-z (строчные буквы)
    cleaned_words := ARRAY[]::TEXT[];
    FOREACH w IN ARRAY words LOOP
        IF w != '' AND w ~ '^[a-z]+$' THEN
            cleaned_words := array_append(cleaned_words, w);
        END IF;
    END LOOP;
    
    -- 5. Собираем обратно в строку
    result := array_to_string(cleaned_words, ' ');
    
    RETURN result;
END;
$BODY$
  LANGUAGE plpgsql IMMUTABLE
  COST 100;

-- ----------------------------
-- Function structure for get_e_code_priority
-- ----------------------------
DROP FUNCTION IF EXISTS "cologne_mw"."get_e_code_priority"("e_code" text);
CREATE OR REPLACE FUNCTION "cologne_mw"."get_e_code_priority"("e_code" text)
  RETURNS "pg_catalog"."int4" AS $BODY$
BEGIN
    IF e_code IS NULL THEN
        RETURN 999;
    END IF;

    CASE e_code
        -- Основные записи (наивысший приоритет)
        WHEN '1' THEN RETURN 10;
        WHEN '1A' THEN RETURN 9;
        WHEN '1B' THEN RETURN 8;
        WHEN '1C' THEN RETURN 7;
        WHEN '1E' THEN RETURN 6;
        
        -- Второй уровень
        WHEN '2' THEN RETURN 20;
        WHEN '2A' THEN RETURN 19;
        WHEN '2B' THEN RETURN 18;
        WHEN '2C' THEN RETURN 17;
        WHEN '2E' THEN RETURN 16;
        
        -- Третий уровень
        WHEN '3' THEN RETURN 30;
        WHEN '3A' THEN RETURN 29;
        WHEN '3B' THEN RETURN 28;
        WHEN '3C' THEN RETURN 27;
        WHEN '3E' THEN RETURN 26;
        
        -- Четвёртый уровень
        WHEN '4' THEN RETURN 40;
        WHEN '4A' THEN RETURN 39;
        WHEN '4B' THEN RETURN 38;
        WHEN '4C' THEN RETURN 37;
        WHEN '4E' THEN RETURN 36;
        
        -- H2 серия (специальные подзаписи)
        WHEN 'H2A' THEN RETURN 51;
        WHEN 'H2B' THEN RETURN 52;
        WHEN 'H2C' THEN RETURN 53;
        WHEN 'H2E' THEN RETURN 54;
        
        -- H3 серия
        WHEN 'H3A' THEN RETURN 61;
        WHEN 'H3B' THEN RETURN 62;
        WHEN 'H3C' THEN RETURN 63;
        WHEN 'H3E' THEN RETURN 64;
        
        -- H4 серия
        WHEN 'H4A' THEN RETURN 71;
        WHEN 'H4B' THEN RETURN 72;
        WHEN 'H4C' THEN RETURN 73;
        WHEN 'H4E' THEN RETURN 74;
        
        -- Всё остальное (низший приоритет)
        ELSE RETURN 999;
    END CASE;
END;
$BODY$
  LANGUAGE plpgsql IMMUTABLE
  COST 100;

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_mw"."abbreviation_id_seq"
OWNED BY "cologne_mw"."abbreviation"."id";
SELECT setval('"cologne_mw"."abbreviation_id_seq"', 199238, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_mw"."biological_name_id_seq"
OWNED BY "cologne_mw"."biological_name"."id";
SELECT setval('"cologne_mw"."biological_name_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_mw"."div_marker_id_seq"
OWNED BY "cologne_mw"."div_marker"."id";
SELECT setval('"cologne_mw"."div_marker_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_mw"."entry_id_seq"
OWNED BY "cologne_mw"."entry"."id";
SELECT setval('"cologne_mw"."entry_id_seq"', 575729, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_mw"."entry_relation_id_seq"
OWNED BY "cologne_mw"."entry_relation"."id";
SELECT setval('"cologne_mw"."entry_relation_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_mw"."foreign_word_id_seq"
OWNED BY "cologne_mw"."foreign_word"."id";
SELECT setval('"cologne_mw"."foreign_word_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_mw"."homonym_id_seq"
OWNED BY "cologne_mw"."homonym"."id";
SELECT setval('"cologne_mw"."homonym_id_seq"', 11703, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_mw"."info_id_seq"
OWNED BY "cologne_mw"."info"."id";
SELECT setval('"cologne_mw"."info_id_seq"', 278978, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_mw"."lexcat_info_id_seq"
OWNED BY "cologne_mw"."lexcat_info"."id";
SELECT setval('"cologne_mw"."lexcat_info_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_mw"."lexical_info_id_seq"
OWNED BY "cologne_mw"."lexical_info"."id";
SELECT setval('"cologne_mw"."lexical_info_id_seq"', 457782, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_mw"."literary_source_id_seq"
OWNED BY "cologne_mw"."literary_source"."id";
SELECT setval('"cologne_mw"."literary_source_id_seq"', 319002, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_mw"."page_break_id_seq"
OWNED BY "cologne_mw"."page_break"."id";
SELECT setval('"cologne_mw"."page_break_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_mw"."sanskrit_word_id_seq"
OWNED BY "cologne_mw"."sanskrit_word"."id";
SELECT setval('"cologne_mw"."sanskrit_word_id_seq"', 654466, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_mw"."westergaard_link_id_seq"
OWNED BY "cologne_mw"."westergaard_link"."id";
SELECT setval('"cologne_mw"."westergaard_link_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_mw"."whitney_link_id_seq"
OWNED BY "cologne_mw"."whitney_link"."id";
SELECT setval('"cologne_mw"."whitney_link_id_seq"', 1, false);

-- ----------------------------
-- Indexes structure for table abbreviation
-- ----------------------------
CREATE INDEX "idx_mw_abbreviation_entry_id" ON "cologne_mw"."abbreviation" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table abbreviation
-- ----------------------------
ALTER TABLE "cologne_mw"."abbreviation" ADD CONSTRAINT "abbreviation_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table biological_name
-- ----------------------------
CREATE INDEX "idx_mw_biological_name_entry_id" ON "cologne_mw"."biological_name" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Checks structure for table biological_name
-- ----------------------------
ALTER TABLE "cologne_mw"."biological_name" ADD CONSTRAINT "biological_name_name_type_check" CHECK (name_type::text = ANY (ARRAY['bot'::character varying, 'bio'::character varying]::text[]));

-- ----------------------------
-- Primary Key structure for table biological_name
-- ----------------------------
ALTER TABLE "cologne_mw"."biological_name" ADD CONSTRAINT "biological_name_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table div_marker
-- ----------------------------
CREATE INDEX "idx_mw_div_marker_entry_id" ON "cologne_mw"."div_marker" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table div_marker
-- ----------------------------
ALTER TABLE "cologne_mw"."div_marker" ADD CONSTRAINT "div_marker_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table entry
-- ----------------------------
CREATE INDEX "entry_key1_normalized_idx" ON "cologne_mw"."entry" USING btree (
  "key1_normalized" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_mw_entry_key1" ON "cologne_mw"."entry" USING btree (
  "key1" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_mw_entry_key1_iast" ON "cologne_mw"."entry" USING btree (
  "key1_iast" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_mw_entry_key1_iast_plain" ON "cologne_mw"."entry" USING btree (
  "key1_iast_plain" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_mw_entry_key1_iast_plain_trgm" ON "cologne_mw"."entry" USING gin (
  "key1_iast_plain" COLLATE "pg_catalog"."default" "public"."gin_trgm_ops"
);
CREATE INDEX "idx_mw_entry_main_definition" ON "cologne_mw"."entry" USING gin (
  to_tsvector('english'::regconfig, main_definition) "pg_catalog"."tsvector_ops"
);
CREATE INDEX "idx_mw_entry_page" ON "cologne_mw"."entry" USING btree (
  "page" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_mw_entry_record_id" ON "cologne_mw"."entry" USING btree (
  "record_id_full" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_mw_entry_record_id_full" ON "cologne_mw"."entry" USING btree (
  "record_id_full" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_mw_entry_record_id_numeric" ON "cologne_mw"."entry" USING btree (
  "record_id_numeric" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_mw_entry_short_definition" ON "cologne_mw"."entry" USING btree (
  to_tsvector('english'::regconfig, short_definition::text) "pg_catalog"."tsvector_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table entry
-- ----------------------------
ALTER TABLE "cologne_mw"."entry" ADD CONSTRAINT "entry_record_id_key" UNIQUE ("record_id_full");

-- ----------------------------
-- Primary Key structure for table entry
-- ----------------------------
ALTER TABLE "cologne_mw"."entry" ADD CONSTRAINT "entry_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table entry_relation
-- ----------------------------
CREATE INDEX "idx_mw_entry_relation_entry_id" ON "cologne_mw"."entry_relation" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_mw_entry_relation_related" ON "cologne_mw"."entry_relation" USING btree (
  "related_entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table entry_relation
-- ----------------------------
ALTER TABLE "cologne_mw"."entry_relation" ADD CONSTRAINT "unique_relation" UNIQUE ("entry_id", "related_entry_id", "relation_type");

-- ----------------------------
-- Primary Key structure for table entry_relation
-- ----------------------------
ALTER TABLE "cologne_mw"."entry_relation" ADD CONSTRAINT "entry_relation_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table foreign_word
-- ----------------------------
CREATE INDEX "idx_mw_foreign_word_entry_id" ON "cologne_mw"."foreign_word" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table foreign_word
-- ----------------------------
ALTER TABLE "cologne_mw"."foreign_word" ADD CONSTRAINT "foreign_word_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table homonym
-- ----------------------------
CREATE INDEX "idx_mw_homonym_entry_id" ON "cologne_mw"."homonym" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table homonym
-- ----------------------------
ALTER TABLE "cologne_mw"."homonym" ADD CONSTRAINT "homonym_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table info
-- ----------------------------
CREATE INDEX "idx_mw_info_entry_id" ON "cologne_mw"."info" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table info
-- ----------------------------
ALTER TABLE "cologne_mw"."info" ADD CONSTRAINT "info_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table lexcat_info
-- ----------------------------
CREATE INDEX "idx_mw_lexcat_info_entry_id" ON "cologne_mw"."lexcat_info" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table lexcat_info
-- ----------------------------
ALTER TABLE "cologne_mw"."lexcat_info" ADD CONSTRAINT "lexcat_info_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table lexical_info
-- ----------------------------
CREATE INDEX "idx_mw_lexical_info_entry_id" ON "cologne_mw"."lexical_info" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table lexical_info
-- ----------------------------
ALTER TABLE "cologne_mw"."lexical_info" ADD CONSTRAINT "lexical_info_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table literary_source
-- ----------------------------
CREATE INDEX "idx_mw_literary_source_entry_id" ON "cologne_mw"."literary_source" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table literary_source
-- ----------------------------
ALTER TABLE "cologne_mw"."literary_source" ADD CONSTRAINT "literary_source_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table page_break
-- ----------------------------
CREATE INDEX "idx_mw_page_break_entry_id" ON "cologne_mw"."page_break" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table page_break
-- ----------------------------
ALTER TABLE "cologne_mw"."page_break" ADD CONSTRAINT "page_break_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sanskrit_word
-- ----------------------------
CREATE INDEX "idx_mw_sanskrit_word_entry_id" ON "cologne_mw"."sanskrit_word" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_mw_sanskrit_word_key1_search_trgm" ON "cologne_mw"."sanskrit_word" USING gin (
  "slp1_normalized" COLLATE "pg_catalog"."default" "public"."gin_trgm_ops"
);
CREATE INDEX "idx_mw_sanskrit_word_slp1" ON "cologne_mw"."sanskrit_word" USING btree (
  "slp1_spelling" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sanskrit_word
-- ----------------------------
ALTER TABLE "cologne_mw"."sanskrit_word" ADD CONSTRAINT "sanskrit_word_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table westergaard_link
-- ----------------------------
CREATE INDEX "idx_mw_westergaard_link_entry_id" ON "cologne_mw"."westergaard_link" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table westergaard_link
-- ----------------------------
ALTER TABLE "cologne_mw"."westergaard_link" ADD CONSTRAINT "westergaard_link_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table whitney_link
-- ----------------------------
CREATE INDEX "idx_mw_whitney_link_entry_id" ON "cologne_mw"."whitney_link" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table whitney_link
-- ----------------------------
ALTER TABLE "cologne_mw"."whitney_link" ADD CONSTRAINT "whitney_link_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table abbreviation
-- ----------------------------
ALTER TABLE "cologne_mw"."abbreviation" ADD CONSTRAINT "abbreviation_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_mw"."entry" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table biological_name
-- ----------------------------
ALTER TABLE "cologne_mw"."biological_name" ADD CONSTRAINT "biological_name_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_mw"."entry" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table div_marker
-- ----------------------------
ALTER TABLE "cologne_mw"."div_marker" ADD CONSTRAINT "div_marker_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_mw"."entry" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table entry_relation
-- ----------------------------
ALTER TABLE "cologne_mw"."entry_relation" ADD CONSTRAINT "entry_relation_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_mw"."entry" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "cologne_mw"."entry_relation" ADD CONSTRAINT "entry_relation_related_entry_id_fkey" FOREIGN KEY ("related_entry_id") REFERENCES "cologne_mw"."entry" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table foreign_word
-- ----------------------------
ALTER TABLE "cologne_mw"."foreign_word" ADD CONSTRAINT "foreign_word_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_mw"."entry" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table homonym
-- ----------------------------
ALTER TABLE "cologne_mw"."homonym" ADD CONSTRAINT "homonym_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_mw"."entry" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table info
-- ----------------------------
ALTER TABLE "cologne_mw"."info" ADD CONSTRAINT "info_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_mw"."entry" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table lexcat_info
-- ----------------------------
ALTER TABLE "cologne_mw"."lexcat_info" ADD CONSTRAINT "lexcat_info_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_mw"."entry" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table lexical_info
-- ----------------------------
ALTER TABLE "cologne_mw"."lexical_info" ADD CONSTRAINT "lexical_info_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_mw"."entry" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table literary_source
-- ----------------------------
ALTER TABLE "cologne_mw"."literary_source" ADD CONSTRAINT "literary_source_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_mw"."entry" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table page_break
-- ----------------------------
ALTER TABLE "cologne_mw"."page_break" ADD CONSTRAINT "page_break_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_mw"."entry" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table sanskrit_word
-- ----------------------------
ALTER TABLE "cologne_mw"."sanskrit_word" ADD CONSTRAINT "sanskrit_word_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_mw"."entry" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table westergaard_link
-- ----------------------------
ALTER TABLE "cologne_mw"."westergaard_link" ADD CONSTRAINT "westergaard_link_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_mw"."entry" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table whitney_link
-- ----------------------------
ALTER TABLE "cologne_mw"."whitney_link" ADD CONSTRAINT "whitney_link_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_mw"."entry" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
