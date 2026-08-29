-- Migration: move normalize_lemma to lingua and fix the cologne_frisch schema rename.
--
-- Background: the cologne_frisch schema was renamed from "frisch" to "cologne_frisch",
-- but the stored function bodies still hard-reference the old "frisch." schema prefix
-- (tables, custom types and frisch.normalize_lemma). This broke every call, e.g.
-- cologne_frisch.get_lemma_json() -> frisch.get_lemma_info() -> frisch.normalize_lemma().
--
-- This script:
--   1. ensures the lingua schema and creates lingua.normalize_lemma (NFKD-strip);
--   2. rewrites ALL cologne_frisch functions so that every "frisch." reference inside
--      their bodies becomes "cologne_frisch." (same-schema objects) and
--      frisch.normalize_lemma becomes lingua.normalize_lemma;
--   3. drops the now-unused cologne_frisch.normalize_lemma;
--   4. drops the redundant public diacritic helpers.

-- 1. Target schema + the canonical normalize_lemma (NFKD-strip, as-is)
CREATE SCHEMA IF NOT EXISTS lingua;

DROP FUNCTION IF EXISTS lingua.normalize_lemma(text);
CREATE OR REPLACE FUNCTION lingua.normalize_lemma(p_text text)
  RETURNS pg_catalog.text AS $BODY$
    SELECT lower(
        regexp_replace(normalize(trim(p_text), NFKD), '[\u0300-\u036f]', '', 'g')
    );
$BODY$
  LANGUAGE sql IMMUTABLE STRICT
  COST 100;

-- 2. Rewrite every cologne_frisch function body: frisch. -> cologne_frisch.,
--    and frisch.normalize_lemma -> lingua.normalize_lemma.
--    The token dance avoids clobbering the "cologne_frisch." prefix itself
--    (which contains the substring "frisch.").
DO $$
DECLARE
    r      RECORD;
    def    TEXT;
    newdef TEXT;
BEGIN
    FOR r IN
        SELECT p.oid
        FROM pg_proc p
        JOIN pg_namespace n ON n.oid = p.pronamespace
        WHERE n.nspname = 'cologne_frisch'
          AND p.prokind = 'f'
    LOOP
        def := pg_get_functiondef(r.oid);

        newdef := replace(def, 'cologne_frisch.', 'ZZ_COL_FR_ZZ');
        newdef := replace(newdef, 'frisch.', 'cologne_frisch.');
        newdef := replace(newdef, 'ZZ_COL_FR_ZZ', 'cologne_frisch.');
        newdef := replace(newdef, 'cologne_frisch.normalize_lemma', 'lingva.normalize_lemma');

        EXECUTE newdef;
    END LOOP;
END $$;

-- 3. normalize_lemma is now provided by lingua; drop the cologne_frisch copy
DROP FUNCTION IF EXISTS cologne_frisch.normalize_lemma(text);

-- 4. Drop redundant public diacritic helpers
DROP FUNCTION IF EXISTS public.slp1_to_iast_plain(text);
DROP FUNCTION IF EXISTS public.remove_iast_diacritics(text);
