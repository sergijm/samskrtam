-- ================================================================
-- Lesson 4: declensions-i — I_STEM, UNSPECIFIED
-- ================================================================
INSERT INTO content.case_endings (vowel_type, gender, case_type, number_type, ending_iast, ending_devanagari)
VALUES ('I_STEM', 'UNSPECIFIED', 'NOMINATIVE', 'SINGULAR', 'iḥ', NULL),
       ('I_STEM', 'UNSPECIFIED', 'ACCUSATIVE', 'SINGULAR', 'im', NULL),
       ('I_STEM', 'UNSPECIFIED', 'INSTRUMENTAL', 'SINGULAR', 'inā', NULL),
       ('I_STEM', 'UNSPECIFIED', 'DATIVE', 'SINGULAR', 'aye', NULL),
       ('I_STEM', 'UNSPECIFIED', 'ABLATIVE', 'SINGULAR', 'eḥ', NULL),
       ('I_STEM', 'UNSPECIFIED', 'GENITIVE', 'SINGULAR', 'eḥ', NULL),
       ('I_STEM', 'UNSPECIFIED', 'LOCATIVE', 'SINGULAR', 'au', NULL),
       ('I_STEM', 'UNSPECIFIED', 'VOCATIVE', 'SINGULAR', 'e', NULL),
       ('I_STEM', 'UNSPECIFIED', 'NOMINATIVE', 'DUAL', 'ī', NULL),
       ('I_STEM', 'UNSPECIFIED', 'ACCUSATIVE', 'DUAL', 'ī', NULL),
       ('I_STEM', 'UNSPECIFIED', 'INSTRUMENTAL', 'DUAL', 'ibhyām', NULL),
       ('I_STEM', 'UNSPECIFIED', 'DATIVE', 'DUAL', 'ibhyām', NULL),
       ('I_STEM', 'UNSPECIFIED', 'ABLATIVE', 'DUAL', 'ibhyām', NULL),
       ('I_STEM', 'UNSPECIFIED', 'GENITIVE', 'DUAL', 'yoḥ', NULL),
       ('I_STEM', 'UNSPECIFIED', 'LOCATIVE', 'DUAL', 'yoḥ', NULL),
       ('I_STEM', 'UNSPECIFIED', 'VOCATIVE', 'DUAL', 'ī', NULL),
       ('I_STEM', 'UNSPECIFIED', 'NOMINATIVE', 'PLURAL', 'ayaḥ', NULL),
       ('I_STEM', 'UNSPECIFIED', 'ACCUSATIVE', 'PLURAL', 'īn', NULL),
       ('I_STEM', 'UNSPECIFIED', 'INSTRUMENTAL', 'PLURAL', 'ibhiḥ', NULL),
       ('I_STEM', 'UNSPECIFIED', 'DATIVE', 'PLURAL', 'ibhyaḥ', NULL),
       ('I_STEM', 'UNSPECIFIED', 'ABLATIVE', 'PLURAL', 'ibhyaḥ', NULL),
       ('I_STEM', 'UNSPECIFIED', 'GENITIVE', 'PLURAL', 'īnām', NULL),
       ('I_STEM', 'UNSPECIFIED', 'LOCATIVE', 'PLURAL', 'iṣu', NULL),
       ('I_STEM', 'UNSPECIFIED', 'VOCATIVE', 'PLURAL', 'ayaḥ', NULL)
ON CONFLICT DO NOTHING;

-- ================================================================
-- Lesson 5: declensions-ii — II_STEM, FEMININE (e.g. devī)
-- ================================================================
INSERT INTO content.case_endings (vowel_type, gender, case_type, number_type, ending_iast, ending_devanagari)
VALUES ('II_STEM', 'FEMININE', 'NOMINATIVE', 'SINGULAR', 'ī', NULL),
       ('II_STEM', 'FEMININE', 'ACCUSATIVE', 'SINGULAR', 'īm', NULL),
       ('II_STEM', 'FEMININE', 'INSTRUMENTAL', 'SINGULAR', 'yā', NULL),
       ('II_STEM', 'FEMININE', 'DATIVE', 'SINGULAR', 'yai', NULL),
       ('II_STEM', 'FEMININE', 'ABLATIVE', 'SINGULAR', 'yāḥ', NULL),
       ('II_STEM', 'FEMININE', 'GENITIVE', 'SINGULAR', 'yāḥ', NULL),
       ('II_STEM', 'FEMININE', 'LOCATIVE', 'SINGULAR', 'yām', NULL),
       ('II_STEM', 'FEMININE', 'VOCATIVE', 'SINGULAR', 'i', NULL),
       ('II_STEM', 'FEMININE', 'NOMINATIVE', 'DUAL', 'yau', NULL),
       ('II_STEM', 'FEMININE', 'ACCUSATIVE', 'DUAL', 'yau', NULL),
       ('II_STEM', 'FEMININE', 'INSTRUMENTAL', 'DUAL', 'ībhyām', NULL),
       ('II_STEM', 'FEMININE', 'DATIVE', 'DUAL', 'ībhyām', NULL),
       ('II_STEM', 'FEMININE', 'ABLATIVE', 'DUAL', 'ībhyām', NULL),
       ('II_STEM', 'FEMININE', 'GENITIVE', 'DUAL', 'yoḥ', NULL),
       ('II_STEM', 'FEMININE', 'LOCATIVE', 'DUAL', 'yoḥ', NULL),
       ('II_STEM', 'FEMININE', 'VOCATIVE', 'DUAL', 'yau', NULL),
       ('II_STEM', 'FEMININE', 'NOMINATIVE', 'PLURAL', 'yaḥ', NULL),
       ('II_STEM', 'FEMININE', 'ACCUSATIVE', 'PLURAL', 'īḥ', NULL),
       ('II_STEM', 'FEMININE', 'INSTRUMENTAL', 'PLURAL', 'ībhiḥ', NULL),
       ('II_STEM', 'FEMININE', 'DATIVE', 'PLURAL', 'ībhyaḥ', NULL),
       ('II_STEM', 'FEMININE', 'ABLATIVE', 'PLURAL', 'ībhyaḥ', NULL),
       ('II_STEM', 'FEMININE', 'GENITIVE', 'PLURAL', 'īnām', NULL),
       ('II_STEM', 'FEMININE', 'LOCATIVE', 'PLURAL', 'īṣu', NULL),
       ('II_STEM', 'FEMININE', 'VOCATIVE', 'PLURAL', 'yaḥ', NULL)
ON CONFLICT DO NOTHING;

-- ================================================================
-- Lesson 6: declensions-u — U_STEM, UNSPECIFIED
-- ================================================================
INSERT INTO content.case_endings (vowel_type, gender, case_type, number_type, ending_iast, ending_devanagari)
VALUES ('U_STEM', 'UNSPECIFIED', 'NOMINATIVE', 'SINGULAR', 'uḥ', NULL),
       ('U_STEM', 'UNSPECIFIED', 'ACCUSATIVE', 'SINGULAR', 'um', NULL),
       ('U_STEM', 'UNSPECIFIED', 'INSTRUMENTAL', 'SINGULAR', 'unā', NULL),
       ('U_STEM', 'UNSPECIFIED', 'DATIVE', 'SINGULAR', 'ave', NULL),
       ('U_STEM', 'UNSPECIFIED', 'ABLATIVE', 'SINGULAR', 'oḥ', NULL),
       ('U_STEM', 'UNSPECIFIED', 'GENITIVE', 'SINGULAR', 'oḥ', NULL),
       ('U_STEM', 'UNSPECIFIED', 'LOCATIVE', 'SINGULAR', 'au', NULL),
       ('U_STEM', 'UNSPECIFIED', 'VOCATIVE', 'SINGULAR', 'o', NULL),
       ('U_STEM', 'UNSPECIFIED', 'NOMINATIVE', 'DUAL', 'ū', NULL),
       ('U_STEM', 'UNSPECIFIED', 'ACCUSATIVE', 'DUAL', 'ū', NULL),
       ('U_STEM', 'UNSPECIFIED', 'INSTRUMENTAL', 'DUAL', 'ubhyām', NULL),
       ('U_STEM', 'UNSPECIFIED', 'DATIVE', 'DUAL', 'ubhyām', NULL),
       ('U_STEM', 'UNSPECIFIED', 'ABLATIVE', 'DUAL', 'ubhyām', NULL),
       ('U_STEM', 'UNSPECIFIED', 'GENITIVE', 'DUAL', 'voḥ', NULL),
       ('U_STEM', 'UNSPECIFIED', 'LOCATIVE', 'DUAL', 'voḥ', NULL),
       ('U_STEM', 'UNSPECIFIED', 'VOCATIVE', 'DUAL', 'ū', NULL),
       ('U_STEM', 'UNSPECIFIED', 'NOMINATIVE', 'PLURAL', 'avaḥ', NULL),
       ('U_STEM', 'UNSPECIFIED', 'ACCUSATIVE', 'PLURAL', 'ūn', NULL),
       ('U_STEM', 'UNSPECIFIED', 'INSTRUMENTAL', 'PLURAL', 'ubhiḥ', NULL),
       ('U_STEM', 'UNSPECIFIED', 'DATIVE', 'PLURAL', 'ubhyaḥ', NULL),
       ('U_STEM', 'UNSPECIFIED', 'ABLATIVE', 'PLURAL', 'ubhyaḥ', NULL),
       ('U_STEM', 'UNSPECIFIED', 'GENITIVE', 'PLURAL', 'ūnām', NULL),
       ('U_STEM', 'UNSPECIFIED', 'LOCATIVE', 'PLURAL', 'uṣu', NULL),
       ('U_STEM', 'UNSPECIFIED', 'VOCATIVE', 'PLURAL', 'avaḥ', NULL)
ON CONFLICT DO NOTHING;


-- ================================================================
-- Lesson 7: declensions-uu — UU_STEM, FEMININE (e.g. vadhū)
-- ================================================================
INSERT INTO content.case_endings (vowel_type, gender, case_type, number_type, ending_iast, ending_devanagari)
VALUES ('UU_STEM', 'FEMININE', 'NOMINATIVE', 'SINGULAR', 'ūḥ', NULL),
       ('UU_STEM', 'FEMININE', 'ACCUSATIVE', 'SINGULAR', 'ūm', NULL),
       ('UU_STEM', 'FEMININE', 'INSTRUMENTAL', 'SINGULAR', 'vā', NULL),
       ('UU_STEM', 'FEMININE', 'DATIVE', 'SINGULAR', 'vai', NULL),
       ('UU_STEM', 'FEMININE', 'ABLATIVE', 'SINGULAR', 'vāḥ', NULL),
       ('UU_STEM', 'FEMININE', 'GENITIVE', 'SINGULAR', 'vāḥ', NULL),
       ('UU_STEM', 'FEMININE', 'LOCATIVE', 'SINGULAR', 'vām', NULL),
       ('UU_STEM', 'FEMININE', 'VOCATIVE', 'SINGULAR', 'vā', NULL),
       ('UU_STEM', 'FEMININE', 'NOMINATIVE', 'DUAL', 'vau', NULL),
       ('UU_STEM', 'FEMININE', 'ACCUSATIVE', 'DUAL', 'vau', NULL),
       ('UU_STEM', 'FEMININE', 'INSTRUMENTAL', 'DUAL', 'ūbhyām', NULL),
       ('UU_STEM', 'FEMININE', 'DATIVE', 'DUAL', 'ūbhyām', NULL),
       ('UU_STEM', 'FEMININE', 'ABLATIVE', 'DUAL', 'ūbhyām', NULL),
       ('UU_STEM', 'FEMININE', 'GENITIVE', 'DUAL', 'voḥ', NULL),
       ('UU_STEM', 'FEMININE', 'LOCATIVE', 'DUAL', 'voḥ', NULL),
       ('UU_STEM', 'FEMININE', 'VOCATIVE', 'DUAL', 'vau', NULL),
       ('UU_STEM', 'FEMININE', 'NOMINATIVE', 'PLURAL', 'vaḥ', NULL),
       ('UU_STEM', 'FEMININE', 'ACCUSATIVE', 'PLURAL', 'ūḥ', NULL),
       ('UU_STEM', 'FEMININE', 'INSTRUMENTAL', 'PLURAL', 'ūbhiḥ', NULL),
       ('UU_STEM', 'FEMININE', 'DATIVE', 'PLURAL', 'ūbhyaḥ', NULL),
       ('UU_STEM', 'FEMININE', 'ABLATIVE', 'PLURAL', 'ūbhyaḥ', NULL),
       ('UU_STEM', 'FEMININE', 'GENITIVE', 'PLURAL', 'ūnām', NULL),
       ('UU_STEM', 'FEMININE', 'LOCATIVE', 'PLURAL', 'ūṣu', NULL),
       ('UU_STEM', 'FEMININE', 'VOCATIVE', 'PLURAL', 'vaḥ', NULL)
ON CONFLICT DO NOTHING;


-- ================================================================
-- Lesson 8: declensions-r — R_STEM, UNSPECIFIED (e.g. pitṛ)
-- ================================================================
INSERT INTO content.case_endings (vowel_type, gender, case_type, number_type, ending_iast, ending_devanagari)
VALUES ('R_STEM', 'UNSPECIFIED', 'NOMINATIVE', 'SINGULAR', 'ā', NULL),
       ('R_STEM', 'UNSPECIFIED', 'ACCUSATIVE', 'SINGULAR', 'aram', NULL),
       ('R_STEM', 'UNSPECIFIED', 'INSTRUMENTAL', 'SINGULAR', 'rā', NULL),
       ('R_STEM', 'UNSPECIFIED', 'DATIVE', 'SINGULAR', 're', NULL),
       ('R_STEM', 'UNSPECIFIED', 'ABLATIVE', 'SINGULAR', 'ruḥ', NULL),
       ('R_STEM', 'UNSPECIFIED', 'GENITIVE', 'SINGULAR', 'ruḥ', NULL),
       ('R_STEM', 'UNSPECIFIED', 'LOCATIVE', 'SINGULAR', 'rari', NULL),
       ('R_STEM', 'UNSPECIFIED', 'VOCATIVE', 'SINGULAR', 'an', NULL),
       ('R_STEM', 'UNSPECIFIED', 'NOMINATIVE', 'DUAL', 'ārau', NULL),
       ('R_STEM', 'UNSPECIFIED', 'ACCUSATIVE', 'DUAL', 'ārau', NULL),
       ('R_STEM', 'UNSPECIFIED', 'INSTRUMENTAL', 'DUAL', 'ṛbhyām', NULL),
       ('R_STEM', 'UNSPECIFIED', 'DATIVE', 'DUAL', 'ṛbhyām', NULL),
       ('R_STEM', 'UNSPECIFIED', 'ABLATIVE', 'DUAL', 'ṛbhyām', NULL),
       ('R_STEM', 'UNSPECIFIED', 'GENITIVE', 'DUAL', 'roḥ', NULL),
       ('R_STEM', 'UNSPECIFIED', 'LOCATIVE', 'DUAL', 'roḥ', NULL),
       ('R_STEM', 'UNSPECIFIED', 'VOCATIVE', 'DUAL', 'ārau', NULL),
       ('R_STEM', 'UNSPECIFIED', 'NOMINATIVE', 'PLURAL', 'āraḥ', NULL),
       ('R_STEM', 'UNSPECIFIED', 'ACCUSATIVE', 'PLURAL', 'ṝn', NULL),
       ('R_STEM', 'UNSPECIFIED', 'INSTRUMENTAL', 'PLURAL', 'ṛbhiḥ', NULL),
       ('R_STEM', 'UNSPECIFIED', 'DATIVE', 'PLURAL', 'ṛbhyaḥ', NULL),
       ('R_STEM', 'UNSPECIFIED', 'ABLATIVE', 'PLURAL', 'ṛbhyaḥ', NULL),
       ('R_STEM', 'UNSPECIFIED', 'GENITIVE', 'PLURAL', 'ṝṇām', NULL),
       ('R_STEM', 'UNSPECIFIED', 'LOCATIVE', 'PLURAL', 'ṛṣu', NULL),
       ('R_STEM', 'UNSPECIFIED', 'VOCATIVE', 'PLURAL', 'āraḥ', NULL)
ON CONFLICT DO NOTHING;
