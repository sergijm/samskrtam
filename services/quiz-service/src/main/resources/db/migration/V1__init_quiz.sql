/*
 Navicat Premium Data Transfer

 Source Server         : mdm-dev-vm
 Source Server Type    : PostgreSQL
 Source Server Version : 170009 (170009)
 Source Host           : mdm-dev:5432
 Source Catalog        : samskrtam
 Source Schema         : quiz

 Target Server Type    : PostgreSQL
 Target Server Version : 170009 (170009)
 File Encoding         : 65001

 Date: 04/07/2026 18:57:18
*/


-- ----------------------------
-- Table structure for grammar_form_score
-- ----------------------------
DROP TABLE IF EXISTS "quiz"."grammar_form_score";
CREATE TABLE "quiz"."grammar_form_score" (
                                             "id" uuid NOT NULL DEFAULT gen_random_uuid(),
                                             "user_id" uuid NOT NULL,
                                             "lesson_id" uuid NOT NULL,
                                             "case_type" varchar COLLATE "pg_catalog"."default" NOT NULL,
                                             "number_type" varchar COLLATE "pg_catalog"."default" NOT NULL,
                                             "score" int4 NOT NULL DEFAULT 0,
                                             "updated_at" timestamptz(6) NOT NULL DEFAULT now(),
                                             "gender" varchar COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for outbox_events
-- ----------------------------
DROP TABLE IF EXISTS "quiz"."outbox_events";
CREATE TABLE "quiz"."outbox_events" (
                                        "id" uuid NOT NULL DEFAULT gen_random_uuid(),
                                        "aggregate_type" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
                                        "aggregate_id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
                                        "event_type" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
                                        "payload" text COLLATE "pg_catalog"."default" NOT NULL,
                                        "created_at" timestamptz(6) NOT NULL,
                                        "status" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
                                        "error_message" text COLLATE "pg_catalog"."default",
                                        "retry_count" int4 NOT NULL DEFAULT 0,
                                        "processed_at" timestamptz(6)
)
;

-- ----------------------------
-- Table structure for quiz_answers
-- ----------------------------
DROP TABLE IF EXISTS "quiz"."quiz_answers";
CREATE TABLE "quiz"."quiz_answers" (
                                       "id" uuid NOT NULL DEFAULT gen_random_uuid(),
                                       "session_id" uuid NOT NULL,
                                       "question_id" uuid NOT NULL,
                                       "selected_option_id" uuid,
                                       "is_correct" bool NOT NULL,
                                       "response_time_ms" int4 NOT NULL,
                                       "answered_at" timestamptz(6) NOT NULL,
                                       "selected_form_iast" varchar(255) COLLATE "pg_catalog"."default",
                                       "correct_form_iast" varchar(255) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for quiz_session
-- ----------------------------
DROP TABLE IF EXISTS "quiz"."quiz_session";
CREATE TABLE "quiz"."quiz_session" (
                                       "id" uuid NOT NULL DEFAULT gen_random_uuid(),
                                       "user_id" uuid NOT NULL,
                                       "lesson_id" uuid NOT NULL,
                                       "lesson_type" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                       "total_questions" int4 NOT NULL,
                                       "answered_questions" int4 NOT NULL,
                                       "score" int4 NOT NULL,
                                       "status" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
                                       "started_at" timestamptz(6) NOT NULL,
                                       "completed_at" timestamptz(6),
                                       "vocabulary_words_json" text COLLATE "pg_catalog"."default",
                                       "generated_quiz_data_id" uuid NOT NULL
)
;

-- ----------------------------
-- Table structure for session_questions
-- ----------------------------
DROP TABLE IF EXISTS "quiz"."session_questions";
CREATE TABLE "quiz"."session_questions" (
                                            "id" uuid NOT NULL DEFAULT gen_random_uuid(),
                                            "session_id" uuid NOT NULL,
                                            "question_id" uuid,
                                            "question_number" int4 NOT NULL,
                                            "text" text COLLATE "pg_catalog"."default" NOT NULL,
                                            "explanation_ru" text COLLATE "pg_catalog"."default",
                                            "explanation_en" text COLLATE "pg_catalog"."default",
                                            "declension_stem_id" uuid,
                                            "target_case" varchar(50) COLLATE "pg_catalog"."default",
                                            "target_number" varchar(50) COLLATE "pg_catalog"."default",
                                            "correct_form_iast" varchar(255) COLLATE "pg_catalog"."default",
                                            "correct_form_devanagari" varchar(255) COLLATE "pg_catalog"."default",
                                            "vocabulary_word_id" uuid,
                                            "question_source_language" varchar(50) COLLATE "pg_catalog"."default",
                                            "question_target_language" varchar(50) COLLATE "pg_catalog"."default",
                                            "correct_translation_ru" varchar(255) COLLATE "pg_catalog"."default",
                                            "correct_translation_en" varchar(255) COLLATE "pg_catalog"."default",
                                            "target_gender" varchar(50) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for word_score
-- ----------------------------
DROP TABLE IF EXISTS "quiz"."word_score";
CREATE TABLE "quiz"."word_score" (
                                     "id" uuid NOT NULL DEFAULT gen_random_uuid(),
                                     "user_id" uuid NOT NULL,
                                     "word_id" uuid NOT NULL,
                                     "lesson_id" uuid NOT NULL,
                                     "score" int4 NOT NULL DEFAULT 0,
                                     "updated_at" timestamptz(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Indexes structure for table grammar_form_score
-- ----------------------------
CREATE INDEX "idx_gfs_user_lesson_gender" ON "quiz"."grammar_form_score" USING btree (
                                                                                      "user_id" "pg_catalog"."uuid_ops" ASC NULLS LAST,
                                                                                      "lesson_id" "pg_catalog"."uuid_ops" ASC NULLS LAST,
                                                                                      "gender" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Uniques structure for table grammar_form_score
-- ----------------------------
ALTER TABLE "quiz"."grammar_form_score" ADD CONSTRAINT "uq_grammar_form_score" UNIQUE ("user_id", "lesson_id", "gender", "case_type", "number_type");

-- ----------------------------
-- Checks structure for table grammar_form_score
-- ----------------------------
ALTER TABLE "quiz"."grammar_form_score" ADD CONSTRAINT "grammar_form_score_score_check" CHECK (score >= 0 AND score <= 100);

-- ----------------------------
-- Primary Key structure for table grammar_form_score
-- ----------------------------
ALTER TABLE "quiz"."grammar_form_score" ADD CONSTRAINT "pk_grammar_form_score" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table outbox_events
-- ----------------------------
CREATE INDEX "idx_outbox_events_aggregate_id" ON "quiz"."outbox_events" USING btree (
                                                                                     "aggregate_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "idx_outbox_events_status" ON "quiz"."outbox_events" USING btree (
                                                                               "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table outbox_events
-- ----------------------------
ALTER TABLE "quiz"."outbox_events" ADD CONSTRAINT "outbox_events_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table quiz_answers
-- ----------------------------
CREATE INDEX "idx_quiz_answers_question_id" ON "quiz"."quiz_answers" USING btree (
                                                                                  "question_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
    );
CREATE INDEX "idx_quiz_answers_session_id" ON "quiz"."quiz_answers" USING btree (
                                                                                 "session_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table quiz_answers
-- ----------------------------
ALTER TABLE "quiz"."quiz_answers" ADD CONSTRAINT "quiz_answers_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table quiz_session
-- ----------------------------
CREATE INDEX "idx_quiz_sessions_lesson_id" ON "quiz"."quiz_session" USING btree (
                                                                                 "lesson_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
    );
CREATE INDEX "idx_quiz_sessions_user_id" ON "quiz"."quiz_session" USING btree (
                                                                               "user_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table quiz_session
-- ----------------------------
ALTER TABLE "quiz"."quiz_session" ADD CONSTRAINT "quiz_sessions_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table session_questions
-- ----------------------------
CREATE INDEX "idx_session_questions_gender" ON "quiz"."session_questions" USING btree (
                                                                                       "session_id" "pg_catalog"."uuid_ops" ASC NULLS LAST,
                                                                                       "target_gender" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
                                                                                       "target_case" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
                                                                                       "target_number" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );
CREATE INDEX "idx_session_questions_question_number" ON "quiz"."session_questions" USING btree (
                                                                                                "session_id" "pg_catalog"."uuid_ops" ASC NULLS LAST,
                                                                                                "question_number" "pg_catalog"."int4_ops" ASC NULLS LAST
    );
CREATE INDEX "idx_session_questions_session_id" ON "quiz"."session_questions" USING btree (
                                                                                           "session_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Primary Key structure for table session_questions
-- ----------------------------
ALTER TABLE "quiz"."session_questions" ADD CONSTRAINT "session_questions_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table word_score
-- ----------------------------
CREATE INDEX "idx_word_score_user_lesson" ON "quiz"."word_score" USING btree (
                                                                              "user_id" "pg_catalog"."uuid_ops" ASC NULLS LAST,
                                                                              "lesson_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
    );

-- ----------------------------
-- Uniques structure for table word_score
-- ----------------------------
ALTER TABLE "quiz"."word_score" ADD CONSTRAINT "uq_word_score" UNIQUE ("user_id", "word_id", "lesson_id");

-- ----------------------------
-- Primary Key structure for table word_score
-- ----------------------------
ALTER TABLE "quiz"."word_score" ADD CONSTRAINT "word_score_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table quiz_answers
-- ----------------------------
ALTER TABLE "quiz"."quiz_answers" ADD CONSTRAINT "quiz_answers_session_id_fkey" FOREIGN KEY ("session_id") REFERENCES "quiz"."quiz_session" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table session_questions
-- ----------------------------
ALTER TABLE "quiz"."session_questions" ADD CONSTRAINT "session_questions_session_id_fkey" FOREIGN KEY ("session_id") REFERENCES "quiz"."quiz_session" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
