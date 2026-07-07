-- V5: Add raw_text column to verses table
-- raw_text stores the raw user input before script detection
ALTER TABLE "sangraha"."verses"
    ADD COLUMN "raw_text" varchar;