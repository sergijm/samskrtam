-- V21: reassign GRAMMAR topics to the new fine-grained domains per the curriculum
-- taxonomy.  First widen the column and drop the old CHECK constraint, then
-- reassign topics and add a fresh constraint with the new enum values.

ALTER TABLE curriculum.topic DROP CONSTRAINT IF EXISTS chk_topic_domain;
ALTER TABLE curriculum.topic ALTER COLUMN domain TYPE VARCHAR(25);

UPDATE curriculum.topic SET domain = 'PHONOLOGY_SCRIPT'  WHERE code IN (
    'deva-svara', 'deva-vyanjana', 'matra-conjuncts', 'deva-diacritics',
    'articulation-places', 'vowel-length-oppositions');

UPDATE curriculum.topic SET domain = 'SANDHI'  WHERE code IN (
    'sandhi-vowels-external', 'sandhi-consonants', 'sandhi-visarga',
    'sandhi-vowels-internal', 'sandhi-consonants-internal',
    'complex-sandhi-combinations');

UPDATE curriculum.topic SET domain = 'GRAMMAR_FOUNDATIONS'  WHERE code IN (
    'stem-case-concept');

UPDATE curriculum.topic SET domain = 'NOMINAL_MORPHOLOGY'  WHERE code IN (
    'a-stem', 'a-stem-fem', 'i-u-stems', 'r-stems',
    'noun-adjective-agreement', 'irregular-stems-declension');

UPDATE curriculum.topic SET domain = 'PRONOUNS'  WHERE code IN (
    'personal-pronouns', 'pronoun-stems-declension',
    'demonstrative-pronouns', 'interrogative-pronouns', 'relative-pronouns');

UPDATE curriculum.topic SET domain = 'VERBAL_MORPHOLOGY'  WHERE code IN (
    'verb-root-stem-ending',
    'present-parasmaipada-formation', 'present-atmanepada',
    'present-parasmaipada-usage', 'imperfect', 'future', 'perfect',
    'aorist', 'imperative', 'optative', 'verb-root-classes-overview');

UPDATE curriculum.topic SET domain = 'NONFINITE_FORMS'  WHERE code IN (
    'present-active-participle', 'past-passive-participle',
    'participle-past-active', 'participle-future',
    'absolutive-ktva', 'absolutive-ya',
    'participial-constructions', 'derivation-tva-ta');

UPDATE curriculum.topic SET domain = 'NUMERALS'  WHERE code IN (
    'numerals-1-4', 'numerals-5-10', 'numeral-agreement');

UPDATE curriculum.topic SET domain = 'CASE_SYNTAX'  WHERE code IN (
    'case-meanings-basic', 'karaka-semantic-roles', 'case-as-karaka');

UPDATE curriculum.topic SET domain = 'SYNTAX'  WHERE code IN (
    'simple-sentence-svo', 'conditional-constructions', 'reported-speech',
    'complex-subordinate-clauses', 'complex-noun-phrases',
    'absolute-constructions', 'complex-relative-constructions',
    'relative-constructions', 'correlative-constructions');

UPDATE curriculum.topic SET domain = 'WORD_FORMATION'  WHERE code IN (
    'compound-words-basics', 'tatpurusha', 'karmadharaya', 'bahuvrihi', 'dvandva',
    'derivation-in-vant-mat');

UPDATE curriculum.topic SET domain = 'ADVANCED_READING'  WHERE code IN (
    'poetic-word-order', 'ellipsis-implied-forms',
    'syntactic-analysis-of-text');

ALTER TABLE curriculum.topic
    ADD CONSTRAINT chk_topic_domain CHECK (domain IN (
        'GRAMMAR', 'LEXICON', 'CONJUNCTION',
        'PHONOLOGY_SCRIPT', 'SANDHI', 'GRAMMAR_FOUNDATIONS',
        'NOMINAL_MORPHOLOGY', 'PRONOUNS', 'VERBAL_MORPHOLOGY',
        'NONFINITE_FORMS', 'NUMERALS', 'CASE_SYNTAX',
        'SYNTAX', 'WORD_FORMATION', 'ADVANCED_READING'));