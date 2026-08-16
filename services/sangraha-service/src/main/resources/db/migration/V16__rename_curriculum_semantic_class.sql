-- V16: curriculum_semantic_topic -> curriculum_semantic_class
-- Editable mirror of the curriculum semantic taxonomy (V7); renamed in sync with
-- curriculum-service V29 (semantic_topic -> semantic_class, see lexical-curriculum.md §3).

ALTER TABLE "sangraha"."curriculum_semantic_topic" RENAME TO "curriculum_semantic_class";

ALTER TABLE "sangraha"."curriculum_semantic_class"
    RENAME CONSTRAINT "pk_curriculum_semantic_topic" TO "pk_curriculum_semantic_class";

ALTER TABLE "sangraha"."curriculum_semantic_class"
    RENAME CONSTRAINT "fk_curriculum_semantic_topic_parent" TO "fk_curriculum_semantic_class_parent";

COMMENT ON TABLE "sangraha"."curriculum_semantic_class" IS 'Editable mirror of the curriculum semantic taxonomy (lexical-curriculum.md §3): 9 roots (parent_code NULL) + 33 leaves = 42 rows.';