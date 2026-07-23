ALTER TABLE sangraha.verse_words
    ADD COLUMN vocabulary_word_id UUID,
    ADD COLUMN vocab_sync_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
