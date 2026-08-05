-- Adds domain discriminator to curriculum.topic so Lexical Topics can be
-- registered as ordinary Topic rows (GRAMMAR vs LEXICON), reusing the same
-- graph/learningLevel/ComplexQuiz machinery. See lexicon.md §0, lexical-curriculum.md §1.

ALTER TABLE curriculum.topic
    ADD COLUMN domain VARCHAR(10) NOT NULL DEFAULT 'GRAMMAR';

ALTER TABLE curriculum.topic
    ADD CONSTRAINT chk_topic_domain CHECK (domain IN ('GRAMMAR', 'LEXICON'));

COMMENT ON COLUMN curriculum.topic.domain IS 'GRAMMAR = original curriculum.md topics, LEXICON = lexical topics backed by curriculum.lexical_topic_binding (see lexical-curriculum.md §1).';

CREATE INDEX idx_topic_domain ON curriculum.topic (domain);
