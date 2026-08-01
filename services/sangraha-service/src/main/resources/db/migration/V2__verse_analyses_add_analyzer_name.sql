-- Add analyzer_name column to verse_analyses
ALTER TABLE sangraha.verse_analyses ADD COLUMN analyzer_name varchar(200);

-- Backfill existing rows with model_name value
UPDATE sangraha.verse_analyses SET analyzer_name = model_name;

-- Set NOT NULL constraint
ALTER TABLE sangraha.verse_analyses ALTER COLUMN analyzer_name SET NOT NULL;
