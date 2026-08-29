/*
 Navicat Premium Data Transfer

 Source Server         : localhost-pg17
 Source Server Type    : PostgreSQL
 Source Server Version : 170000 (170000)
 Source Host           : localhost:5436
 Source Catalog        : samskrtam
 Source Schema         : cologne_mw

 Target Server Type    : PostgreSQL
 Target Server Version : 170000 (170000)
 File Encoding         : 65001

 Date: 26/08/2026 22:19:18
*/


-- ----------------------------
-- Sequence structure for entries_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_mw"."entries_id_seq";
CREATE SEQUENCE "cologne_mw"."entries_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Table structure for entries
-- ----------------------------
DROP TABLE IF EXISTS "cologne_mw"."entries";
CREATE TABLE "cologne_mw"."entries" (
  "id" int8 NOT NULL DEFAULT nextval('"cologne_mw".entries_id_seq'::regclass),
  "entry_id" text COLLATE "pg_catalog"."default" NOT NULL,
  "page_col" text COLLATE "pg_catalog"."default",
  "key1" text COLLATE "pg_catalog"."default",
  "key2" text COLLATE "pg_catalog"."default",
  "homonym" text COLLATE "pg_catalog"."default",
  "entry_no" text COLLATE "pg_catalog"."default",
  "body" text COLLATE "pg_catalog"."default",
  "grammar" jsonb,
  "clean_text" text COLLATE "pg_catalog"."default",
  "imported_at" timestamptz(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_mw"."entries_id_seq"
OWNED BY "cologne_mw"."entries"."id";
SELECT setval('"cologne_mw"."entries_id_seq"', 286525, true);

-- ----------------------------
-- Uniques structure for table entries
-- ----------------------------
ALTER TABLE "cologne_mw"."entries" ADD CONSTRAINT "entries_entry_id_uniq" UNIQUE ("entry_id");

-- ----------------------------
-- Primary Key structure for table entries
-- ----------------------------
ALTER TABLE "cologne_mw"."entries" ADD CONSTRAINT "entries_pkey" PRIMARY KEY ("id");
