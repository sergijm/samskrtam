DROP TABLE IF EXISTS sangraha.outbox_events;

ALTER TABLE sangraha.verse_words
DROP COLUMN IF EXISTS vocabulary_word_id,
DROP COLUMN IF EXISTS vocab_sync_status;
