/*
 Navicat Premium Data Transfer

 Source Server         : localhost-pg17
 Source Server Type    : PostgreSQL
 Source Server Version : 170000 (170000)
 Source Host           : localhost:5436
 Source Catalog        : samskrtam
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 170000 (170000)
 File Encoding         : 65001

 Date: 24/08/2026 13:53:11
*/


-- ----------------------------
-- Type structure for gtrgm
-- ----------------------------
DROP TYPE IF EXISTS "public"."gtrgm";
CREATE TYPE "public"."gtrgm" (
  INPUT = "public"."gtrgm_in",
  OUTPUT = "public"."gtrgm_out",
  INTERNALLENGTH = VARIABLE,
  CATEGORY = U,
  DELIMITER = ','
);
ALTER TYPE "public"."gtrgm" OWNER TO "postgres";

-- ----------------------------
-- Function structure for apply_guna
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."apply_guna"("root" text);
CREATE OR REPLACE FUNCTION "public"."apply_guna"("root" text)
  RETURNS "pg_catalog"."text" AS $BODY$
DECLARE
    result TEXT := root;
    i INTEGER;
    found_vowel BOOLEAN := FALSE;
    first_char TEXT;
    vowel_pos INTEGER := 0;
    vowel_char TEXT;
BEGIN
    IF root IS NULL OR root = '' THEN
        RETURN NULL;
    END IF;

    -- =====================================================
    -- Ищем ПЕРВУЮ гласную в корне (начиная с позиции 1)
    -- =====================================================
    
    FOR i IN 1..LENGTH(root) LOOP
        vowel_char := SUBSTRING(root, i, 1);
        
        -- Проверяем, является ли символ гласной
        IF vowel_char IN ('a', 'ā', 'i', 'ī', 'u', 'ū', 'ṛ', 'ṝ', 'ḷ', 'ḹ', 'e', 'o') THEN
            vowel_pos := i;
            EXIT;
        END IF;
    END LOOP;

    -- Если гласная не найдена, возвращаем как есть
    IF vowel_pos = 0 THEN
        RETURN root;
    END IF;

    -- =====================================================
    -- Применяем правила гуны к найденной гласной
    -- =====================================================
    
    vowel_char := SUBSTRING(root, vowel_pos, 1);
    
    CASE vowel_char
        -- a, ā, e, o — уже в guna-ступени (не меняются)
        WHEN 'a', 'ā', 'e', 'o' THEN
            RETURN root;
        
        -- i → e
        WHEN 'i' THEN
            result := SUBSTRING(root, 1, vowel_pos-1) || 'e' || SUBSTRING(root, vowel_pos+1);
        
        -- u → o
        WHEN 'u' THEN
            result := SUBSTRING(root, 1, vowel_pos-1) || 'o' || SUBSTRING(root, vowel_pos+1);
        
        -- ṛ → ar
        WHEN 'ṛ' THEN
            result := SUBSTRING(root, 1, vowel_pos-1) || 'ar' || SUBSTRING(root, vowel_pos+1);
        
        -- ḷ → al
        WHEN 'ḷ' THEN
            result := SUBSTRING(root, 1, vowel_pos-1) || 'al' || SUBSTRING(root, vowel_pos+1);
        
        -- Долгие гласные (ī, ū, ṝ, ḹ) — не меняются
        WHEN 'ī', 'ū', 'ṝ', 'ḹ' THEN
            RETURN root;
        
        ELSE
            RETURN root;
    END CASE;

    RETURN result;
END;
$BODY$
  LANGUAGE plpgsql IMMUTABLE
  COST 100;

-- ----------------------------
-- Function structure for gin_extract_query_trgm
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gin_extract_query_trgm"(text, internal, int2, internal, internal, internal, internal);
CREATE OR REPLACE FUNCTION "public"."gin_extract_query_trgm"(text, internal, int2, internal, internal, internal, internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/pg_trgm', 'gin_extract_query_trgm'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gin_extract_value_trgm
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gin_extract_value_trgm"(text, internal);
CREATE OR REPLACE FUNCTION "public"."gin_extract_value_trgm"(text, internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/pg_trgm', 'gin_extract_value_trgm'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gin_trgm_consistent
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gin_trgm_consistent"(internal, int2, text, int4, internal, internal, internal, internal);
CREATE OR REPLACE FUNCTION "public"."gin_trgm_consistent"(internal, int2, text, int4, internal, internal, internal, internal)
  RETURNS "pg_catalog"."bool" AS '$libdir/pg_trgm', 'gin_trgm_consistent'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gin_trgm_triconsistent
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gin_trgm_triconsistent"(internal, int2, text, int4, internal, internal, internal);
CREATE OR REPLACE FUNCTION "public"."gin_trgm_triconsistent"(internal, int2, text, int4, internal, internal, internal)
  RETURNS "pg_catalog"."char" AS '$libdir/pg_trgm', 'gin_trgm_triconsistent'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_compress
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_compress"(internal);
CREATE OR REPLACE FUNCTION "public"."gtrgm_compress"(internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/pg_trgm', 'gtrgm_compress'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_consistent
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_consistent"(internal, text, int2, oid, internal);
CREATE OR REPLACE FUNCTION "public"."gtrgm_consistent"(internal, text, int2, oid, internal)
  RETURNS "pg_catalog"."bool" AS '$libdir/pg_trgm', 'gtrgm_consistent'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_decompress
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_decompress"(internal);
CREATE OR REPLACE FUNCTION "public"."gtrgm_decompress"(internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/pg_trgm', 'gtrgm_decompress'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_distance
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_distance"(internal, text, int2, oid, internal);
CREATE OR REPLACE FUNCTION "public"."gtrgm_distance"(internal, text, int2, oid, internal)
  RETURNS "pg_catalog"."float8" AS '$libdir/pg_trgm', 'gtrgm_distance'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_in
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_in"(cstring);
CREATE OR REPLACE FUNCTION "public"."gtrgm_in"(cstring)
  RETURNS "public"."gtrgm" AS '$libdir/pg_trgm', 'gtrgm_in'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_options
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_options"(internal);
CREATE OR REPLACE FUNCTION "public"."gtrgm_options"(internal)
  RETURNS "pg_catalog"."void" AS '$libdir/pg_trgm', 'gtrgm_options'
  LANGUAGE c IMMUTABLE
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_out
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_out"("public"."gtrgm");
CREATE OR REPLACE FUNCTION "public"."gtrgm_out"("public"."gtrgm")
  RETURNS "pg_catalog"."cstring" AS '$libdir/pg_trgm', 'gtrgm_out'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_penalty
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_penalty"(internal, internal, internal);
CREATE OR REPLACE FUNCTION "public"."gtrgm_penalty"(internal, internal, internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/pg_trgm', 'gtrgm_penalty'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_picksplit
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_picksplit"(internal, internal);
CREATE OR REPLACE FUNCTION "public"."gtrgm_picksplit"(internal, internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/pg_trgm', 'gtrgm_picksplit'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_same
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_same"("public"."gtrgm", "public"."gtrgm", internal);
CREATE OR REPLACE FUNCTION "public"."gtrgm_same"("public"."gtrgm", "public"."gtrgm", internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/pg_trgm', 'gtrgm_same'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for gtrgm_union
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."gtrgm_union"(internal, internal);
CREATE OR REPLACE FUNCTION "public"."gtrgm_union"(internal, internal)
  RETURNS "public"."gtrgm" AS '$libdir/pg_trgm', 'gtrgm_union'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for is_valid_hk
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."is_valid_hk"("word" text);
CREATE OR REPLACE FUNCTION "public"."is_valid_hk"("word" text)
  RETURNS "pg_catalog"."bool" AS $BODY$
BEGIN
    IF word IS NULL OR word = '' THEN
        RETURN FALSE;
    END IF;
    -- HK: только ASCII буквы и . - _
    RETURN word ~ '^[a-zA-Z.\-_ ]+$';
END;
$BODY$
  LANGUAGE plpgsql IMMUTABLE
  COST 100;

-- ----------------------------
-- Function structure for is_valid_iast
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."is_valid_iast"("word" text);
CREATE OR REPLACE FUNCTION "public"."is_valid_iast"("word" text)
  RETURNS "pg_catalog"."bool" AS $BODY$
BEGIN
    IF word IS NULL OR word = '' THEN
        RETURN FALSE;
    END IF;
    -- IAST символы: a-z + диакритики
    RETURN word ~ '^[a-zA-ZāīūṛṝḷḹṃḥśṣṭḍṇñĀĪŪṚṜḶḸṂḤŚṢṬḌṆÑ\- ]+$';
END;
$BODY$
  LANGUAGE plpgsql IMMUTABLE
  COST 100;

-- ----------------------------
-- Function structure for is_valid_itrans
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."is_valid_itrans"("word" text);
CREATE OR REPLACE FUNCTION "public"."is_valid_itrans"("word" text)
  RETURNS "pg_catalog"."bool" AS $BODY$
BEGIN
    IF word IS NULL OR word = '' THEN
        RETURN FALSE;
    END IF;
    -- ITRANS: a-z, A-Z, . ~ ^ ' " и специальные комбинации
    RETURN word ~ '^[a-zA-Z.~^''"\- ]+$';
END;
$BODY$
  LANGUAGE plpgsql IMMUTABLE
  COST 100;

-- ----------------------------
-- Function structure for is_valid_slp1
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."is_valid_slp1"("word" text);
CREATE OR REPLACE FUNCTION "public"."is_valid_slp1"("word" text)
  RETURNS "pg_catalog"."bool" AS $BODY$
BEGIN
    IF word IS NULL OR word = '' THEN
        RETURN FALSE;
    END IF;
    -- SLP1: только ASCII + / \ ^ . - _
    RETURN word ~ '^[a-zA-Z/\\^.\-_ ]+$';
END;
$BODY$
  LANGUAGE plpgsql IMMUTABLE
  COST 100;

-- ----------------------------
-- Function structure for remove_iast_diacritics
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."remove_iast_diacritics"("iast" text);
CREATE OR REPLACE FUNCTION "public"."remove_iast_diacritics"("iast" text)
  RETURNS "pg_catalog"."text" AS $BODY$
DECLARE
    result TEXT := iast;
BEGIN
    IF iast IS NULL THEN
        RETURN NULL;
    END IF;
    
    -- Долгие гласные → краткие
    result := REPLACE(result, 'ā', 'a');
    result := REPLACE(result, 'ī', 'i');
    result := REPLACE(result, 'ū', 'u');
    result := REPLACE(result, 'ṛ', 'r');
    result := REPLACE(result, 'ṝ', 'r');
    result := REPLACE(result, 'ḷ', 'l');
    result := REPLACE(result, 'ḹ', 'l');
    
    -- Носовые и другие с диакритиками
    result := REPLACE(result, 'ṃ', 'm');
    result := REPLACE(result, 'ṁ', 'm');
    result := REPLACE(result, 'ḥ', 'h');
    
    -- Ретрофлексные
    result := REPLACE(result, 'ṭ', 't');
    result := REPLACE(result, 'ḍ', 'd');
    result := REPLACE(result, 'ṇ', 'n');
    result := REPLACE(result, 'ṅ', 'n');
    result := REPLACE(result, 'ñ', 'n');
    
    -- Шипящие → s
    result := REPLACE(result, 'ś', 's');
    result := REPLACE(result, 'ṣ', 's');
    
    -- Удаляем все остальные комбинируемые диакритики
    -- (для Unicode символов с комбинируемыми знаками)
    result := UNACCENT(result);
    
    RETURN result;
END;
$BODY$
  LANGUAGE plpgsql IMMUTABLE
  COST 100;

-- ----------------------------
-- Function structure for set_limit
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."set_limit"(float4);
CREATE OR REPLACE FUNCTION "public"."set_limit"(float4)
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'set_limit'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for show_limit
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."show_limit"();
CREATE OR REPLACE FUNCTION "public"."show_limit"()
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'show_limit'
  LANGUAGE c STABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for show_trgm
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."show_trgm"(text);
CREATE OR REPLACE FUNCTION "public"."show_trgm"(text)
  RETURNS "pg_catalog"."_text" AS '$libdir/pg_trgm', 'show_trgm'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for similarity
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."similarity"(text, text);
CREATE OR REPLACE FUNCTION "public"."similarity"(text, text)
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'similarity'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for similarity_dist
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."similarity_dist"(text, text);
CREATE OR REPLACE FUNCTION "public"."similarity_dist"(text, text)
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'similarity_dist'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for similarity_op
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."similarity_op"(text, text);
CREATE OR REPLACE FUNCTION "public"."similarity_op"(text, text)
  RETURNS "pg_catalog"."bool" AS '$libdir/pg_trgm', 'similarity_op'
  LANGUAGE c STABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for slp1_to_iast
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."slp1_to_iast"("word" text);
CREATE OR REPLACE FUNCTION "public"."slp1_to_iast"("word" text)
  RETURNS "pg_catalog"."text" AS $BODY$
SELECT
    -- сибилянты и придыхание h (обрабатываем последними, чтобы не зацепить
    -- уже вставленные диакритические комбинации типа 'h' внутри 'kh','gh' и т.п.
    -- порядок здесь не критичен, т.к. SLP1 однобайтовая и каждый символ
    -- заменяется независимо ровно один раз за проход)
    replace(replace(replace(
    -- губные придыхательные/непридыхательные
    replace(replace(replace(replace(replace(
    -- дентальные
    replace(replace(replace(replace(replace(
    -- ретрофлексные
    replace(replace(replace(replace(replace(
    -- палатальные
    replace(replace(replace(replace(replace(
    -- велярные
    replace(replace(replace(replace(replace(
    -- анусвара / висарга
    replace(replace(
    -- дифтонги (двухбуквенный результат, но односимвольный вход в SLP1)
    replace(replace(
    -- долгие/слоговые гласные и r̥/l̥
    replace(replace(replace(replace(replace(replace(
        word,
        -- гласные (порядок важен только для читаемости, конфликтов нет —
        -- каждый SLP1-символ уникален и заменяется один раз)
        'A','ā'), 'I','ī'), 'U','ū'), 'f','ṛ'), 'F','ṝ'), 'x','ḷ'),
        -- дифтонги
        'E','ai'), 'O','au'),
        -- анусвара/висарга
        'M','ṃ'), 'H','ḥ'),
        -- велярные придыхательные/носовой (длинные комбинации заменяем целиком, символ за символ)
        'K','kh'), 'G','gh'), 'N','ṅ'), 'k','k'), 'g','g'),
        -- палатальные
        'C','ch'), 'J','jh'), 'Y','ñ'), 'c','c'), 'j','j'),
        -- ретрофлексные
        'W','ṭh'), 'Q','ḍh'), 'R','ṇ'), 'w','ṭ'), 'q','ḍ'),
        -- дентальные
        'T','th'), 'D','dh'), 't','t'), 'd','d'), 'n','n'),
        -- губные
        'P','ph'), 'B','bh'), 'p','p'), 'b','b'), 'm','m'),
        -- сибилянты
        'S','ś'), 'z','ṣ'), 's','s')
$BODY$
  LANGUAGE sql IMMUTABLE STRICT
  COST 100;

-- ----------------------------
-- Function structure for slp1_to_iast_plain
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."slp1_to_iast_plain"("slp1" text);
CREATE OR REPLACE FUNCTION "public"."slp1_to_iast_plain"("slp1" text)
  RETURNS "pg_catalog"."text" AS $BODY$
BEGIN
    IF slp1 IS NULL THEN
        RETURN NULL;
    END IF;
    RETURN remove_iast_diacritics(slp1_to_iast(slp1));
END;
$BODY$
  LANGUAGE plpgsql IMMUTABLE
  COST 100;

-- ----------------------------
-- Function structure for strict_word_similarity
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."strict_word_similarity"(text, text);
CREATE OR REPLACE FUNCTION "public"."strict_word_similarity"(text, text)
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'strict_word_similarity'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for strict_word_similarity_commutator_op
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."strict_word_similarity_commutator_op"(text, text);
CREATE OR REPLACE FUNCTION "public"."strict_word_similarity_commutator_op"(text, text)
  RETURNS "pg_catalog"."bool" AS '$libdir/pg_trgm', 'strict_word_similarity_commutator_op'
  LANGUAGE c STABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for strict_word_similarity_dist_commutator_op
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."strict_word_similarity_dist_commutator_op"(text, text);
CREATE OR REPLACE FUNCTION "public"."strict_word_similarity_dist_commutator_op"(text, text)
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'strict_word_similarity_dist_commutator_op'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for strict_word_similarity_dist_op
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."strict_word_similarity_dist_op"(text, text);
CREATE OR REPLACE FUNCTION "public"."strict_word_similarity_dist_op"(text, text)
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'strict_word_similarity_dist_op'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for strict_word_similarity_op
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."strict_word_similarity_op"(text, text);
CREATE OR REPLACE FUNCTION "public"."strict_word_similarity_op"(text, text)
  RETURNS "pg_catalog"."bool" AS '$libdir/pg_trgm', 'strict_word_similarity_op'
  LANGUAGE c STABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for unaccent
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."unaccent"(text);
CREATE OR REPLACE FUNCTION "public"."unaccent"(text)
  RETURNS "pg_catalog"."text" AS '$libdir/unaccent', 'unaccent_dict'
  LANGUAGE c STABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for unaccent
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."unaccent"(regdictionary, text);
CREATE OR REPLACE FUNCTION "public"."unaccent"(regdictionary, text)
  RETURNS "pg_catalog"."text" AS '$libdir/unaccent', 'unaccent_dict'
  LANGUAGE c STABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for unaccent_init
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."unaccent_init"(internal);
CREATE OR REPLACE FUNCTION "public"."unaccent_init"(internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/unaccent', 'unaccent_init'
  LANGUAGE c VOLATILE
  COST 1;

-- ----------------------------
-- Function structure for unaccent_lexize
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."unaccent_lexize"(internal, internal, internal, internal);
CREATE OR REPLACE FUNCTION "public"."unaccent_lexize"(internal, internal, internal, internal)
  RETURNS "pg_catalog"."internal" AS '$libdir/unaccent', 'unaccent_lexize'
  LANGUAGE c VOLATILE
  COST 1;

-- ----------------------------
-- Function structure for update_tsv_definition
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."update_tsv_definition"();
CREATE OR REPLACE FUNCTION "public"."update_tsv_definition"()
  RETURNS "pg_catalog"."trigger" AS $BODY$
BEGIN
    NEW.tsv_definition := to_tsvector('english', COALESCE(NEW.definition, ''));
    RETURN NEW;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

-- ----------------------------
-- Function structure for word_similarity
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."word_similarity"(text, text);
CREATE OR REPLACE FUNCTION "public"."word_similarity"(text, text)
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'word_similarity'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for word_similarity_commutator_op
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."word_similarity_commutator_op"(text, text);
CREATE OR REPLACE FUNCTION "public"."word_similarity_commutator_op"(text, text)
  RETURNS "pg_catalog"."bool" AS '$libdir/pg_trgm', 'word_similarity_commutator_op'
  LANGUAGE c STABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for word_similarity_dist_commutator_op
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."word_similarity_dist_commutator_op"(text, text);
CREATE OR REPLACE FUNCTION "public"."word_similarity_dist_commutator_op"(text, text)
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'word_similarity_dist_commutator_op'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for word_similarity_dist_op
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."word_similarity_dist_op"(text, text);
CREATE OR REPLACE FUNCTION "public"."word_similarity_dist_op"(text, text)
  RETURNS "pg_catalog"."float4" AS '$libdir/pg_trgm', 'word_similarity_dist_op'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for word_similarity_op
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."word_similarity_op"(text, text);
CREATE OR REPLACE FUNCTION "public"."word_similarity_op"(text, text)
  RETURNS "pg_catalog"."bool" AS '$libdir/pg_trgm', 'word_similarity_op'
  LANGUAGE c STABLE STRICT
  COST 1;
