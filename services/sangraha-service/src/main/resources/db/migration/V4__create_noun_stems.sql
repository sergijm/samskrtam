-- noun_stems (1:N с verse_words) — классификация словоизменительного класса слова
-- (verse-word-grammar.md §1а): на одно слово может быть несколько строк (история
-- классификаций разными model/confidence/createdAt). Не заполняется VerseAnalysisSaver.
CREATE TABLE "sangraha"."noun_stems" (
    "id" uuid NOT NULL DEFAULT gen_random_uuid(),
    "verse_word_id" uuid NOT NULL,
    "stem_iast" varchar(200) NOT NULL,
    "stem_class" varchar(20) NOT NULL,
    "confidence" varchar(10) NOT NULL,
    "model" varchar(100) NOT NULL,
    "created_at" timestamptz(6) NOT NULL DEFAULT now(),
    CONSTRAINT "pk_noun_stems" PRIMARY KEY ("id"),
    CONSTRAINT "fk_noun_stems_word" FOREIGN KEY ("verse_word_id")
        REFERENCES "sangraha"."verse_words" ("id") ON DELETE CASCADE,
    CONSTRAINT "ck_noun_stem_class" CHECK (
        stem_class::text = ANY (ARRAY['A_STEM', 'AA_STEM', 'I_STEM', 'II_STEM', 'U_STEM', 'UU_STEM', 'R_STEM'])
    ),
    CONSTRAINT "ck_noun_stem_confidence" CHECK (
        confidence::text = ANY (ARRAY['HIGH', 'MEDIUM', 'LOW'])
    )
);

CREATE INDEX "idx_noun_stems_verse_word_id" ON "sangraha"."noun_stems" ("verse_word_id");
CREATE INDEX "idx_noun_stems_stem_class" ON "sangraha"."noun_stems" ("stem_class");
