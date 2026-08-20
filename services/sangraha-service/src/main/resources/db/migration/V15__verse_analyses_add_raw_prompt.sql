-- Add raw_prompt column to verse_analyses: the exact prompt/request sent to the LLM
-- before the call (for debugging and reproducibility).
ALTER TABLE sangraha.verse_analyses ADD COLUMN raw_prompt text;
