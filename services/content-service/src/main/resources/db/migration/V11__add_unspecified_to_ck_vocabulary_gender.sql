-- V11: Add UNSPECIFIED to ck_vocabulary_gender CHECK constraint.
-- Java Gender enum has UNSPECIFIED but the DB constraint was missing it.
ALTER TABLE "content"."vocabulary_words" DROP CONSTRAINT IF EXISTS "ck_vocabulary_gender";
ALTER TABLE "content"."vocabulary_words" ADD CONSTRAINT "ck_vocabulary_gender"
    CHECK (gender::text = ANY (ARRAY['MASCULINE'::text, 'FEMININE'::text, 'NEUTER'::text, 'UNKNOWN'::text, 'UNSPECIFIED'::text]));
