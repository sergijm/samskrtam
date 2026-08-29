-- ===================================================================
-- 1. УКАЗАТЕЛЬНОЕ / 3-Е ЛИЦО: tad (तद् — "тот / он / оно")
-- ===================================================================

-- Мужской род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('tad', 'PRON_TAD_MASC', 'NOMINATIVE', 'SINGULAR', 'saḥ', 'सः', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'NOMINATIVE', 'DUAL', 'tau', 'तौ', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'NOMINATIVE', 'PLURAL', 'te', 'ते', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'ACCUSATIVE', 'SINGULAR', 'tam', 'तम्', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'ACCUSATIVE', 'DUAL', 'tau', 'तौ', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'ACCUSATIVE', 'PLURAL', 'tān', 'तान्', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'INSTRUMENTAL', 'SINGULAR', 'tena', 'तेन', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'INSTRUMENTAL', 'DUAL', 'tābhyām', 'ताभ्याम्', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'INSTRUMENTAL', 'PLURAL', 'taiḥ', 'तैः', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'DATIVE', 'SINGULAR', 'tasmai', 'तस्मै', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'DATIVE', 'DUAL', 'tābhyām', 'ताभ्याम्', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'DATIVE', 'PLURAL', 'tebhyaḥ', 'तेभ्यः', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'ABLATIVE', 'SINGULAR', 'tasmāt', 'तस्मात्', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'ABLATIVE', 'DUAL', 'tābhyām', 'ताभ्याम्', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'ABLATIVE', 'PLURAL', 'tebhyaḥ', 'तेभ्यः', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'GENITIVE', 'SINGULAR', 'tasya', 'तस्य', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'GENITIVE', 'DUAL', 'tayoḥ', 'तयोः', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'GENITIVE', 'PLURAL', 'teṣām', 'तेषाम्', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'LOCATIVE', 'SINGULAR', 'tasmin', 'तस्मिन्', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'LOCATIVE', 'DUAL', 'tayoḥ', 'तयोः', 'HIGH'),
       ('tad', 'PRON_TAD_MASC', 'LOCATIVE', 'PLURAL', 'teṣu', 'तेषु', 'HIGH');

-- Средний род (полный набор 21 формы)
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('tad', 'PRON_TAD_NEUT', 'NOMINATIVE', 'SINGULAR', 'tat', 'तत्', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'NOMINATIVE', 'DUAL', 'te', 'ते', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'NOMINATIVE', 'PLURAL', 'tāni', 'तानि', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'ACCUSATIVE', 'SINGULAR', 'tat', 'तत्', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'ACCUSATIVE', 'DUAL', 'te', 'ते', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'ACCUSATIVE', 'PLURAL', 'tāni', 'तानि', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'INSTRUMENTAL', 'SINGULAR', 'tena', 'तेन', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'INSTRUMENTAL', 'DUAL', 'tābhyām', 'ताभ्याम्', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'INSTRUMENTAL', 'PLURAL', 'taiḥ', 'तैः', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'DATIVE', 'SINGULAR', 'tasmai', 'तस्मै', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'DATIVE', 'DUAL', 'tābhyām', 'ताभ्याम्', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'DATIVE', 'PLURAL', 'tebhyaḥ', 'तेभ्यः', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'ABLATIVE', 'SINGULAR', 'tasmāt', 'तस्मात्', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'ABLATIVE', 'DUAL', 'tābhyām', 'ताभ्याम्', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'ABLATIVE', 'PLURAL', 'tebhyaḥ', 'तेभ्यः', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'GENITIVE', 'SINGULAR', 'tasya', 'तस्य', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'GENITIVE', 'DUAL', 'tayoḥ', 'तयोः', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'GENITIVE', 'PLURAL', 'teṣām', 'तेषाम्', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'LOCATIVE', 'SINGULAR', 'tasmin', 'तस्मिन्', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'LOCATIVE', 'DUAL', 'tayoḥ', 'तयोः', 'HIGH'),
       ('tad', 'PRON_TAD_NEUT', 'LOCATIVE', 'PLURAL', 'teṣu', 'तेषु', 'HIGH');

-- Женский род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('tad', 'PRON_TAD_FEM', 'NOMINATIVE', 'SINGULAR', 'sā', 'सा', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'NOMINATIVE', 'DUAL', 'te', 'ते', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'NOMINATIVE', 'PLURAL', 'tāḥ', 'ताः', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'ACCUSATIVE', 'SINGULAR', 'tām', 'ताम्', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'ACCUSATIVE', 'DUAL', 'te', 'ते', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'ACCUSATIVE', 'PLURAL', 'tāḥ', 'ताः', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'INSTRUMENTAL', 'SINGULAR', 'tayā', 'तया', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'INSTRUMENTAL', 'DUAL', 'tābhyām', 'ताभ्याम्', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'INSTRUMENTAL', 'PLURAL', 'tābhiḥ', 'ताभिः', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'DATIVE', 'SINGULAR', 'tasyai', 'तस्यै', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'DATIVE', 'DUAL', 'tābhyām', 'ताभ्याम्', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'DATIVE', 'PLURAL', 'tābhyaḥ', 'ताभ्यः', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'ABLATIVE', 'SINGULAR', 'tasyāḥ', 'तस्याः', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'ABLATIVE', 'DUAL', 'tābhyām', 'ताभ्याम्', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'ABLATIVE', 'PLURAL', 'tābhyaḥ', 'ताभ्यः', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'GENITIVE', 'SINGULAR', 'tasyāḥ', 'तस्याः', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'GENITIVE', 'DUAL', 'tayoḥ', 'तयोः', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'GENITIVE', 'PLURAL', 'tāsām', 'तासाम्', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'LOCATIVE', 'SINGULAR', 'tasyām', 'तस्याम्', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'LOCATIVE', 'DUAL', 'tayoḥ', 'तयोः', 'HIGH'),
       ('tad', 'PRON_TAD_FEM', 'LOCATIVE', 'PLURAL', 'tāsu', 'तासु', 'HIGH');


-- ===================================================================
-- 2. УКАЗАТЕЛЬНОЕ: etad (एतद् — "этот")
-- ===================================================================

-- Мужской род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('etad', 'PRON_TAD_MASC', 'NOMINATIVE', 'SINGULAR', 'eṣaḥ', 'एषः', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'NOMINATIVE', 'DUAL', 'etau', 'एतौ', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'NOMINATIVE', 'PLURAL', 'ete', 'एते', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'ACCUSATIVE', 'SINGULAR', 'etam', 'एतम्', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'ACCUSATIVE', 'DUAL', 'etau', 'एतौ', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'ACCUSATIVE', 'PLURAL', 'etān', 'एतान्', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'INSTRUMENTAL', 'SINGULAR', 'etena', 'एतेन', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'INSTRUMENTAL', 'DUAL', 'etābhyām', 'एताभ्याम्', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'INSTRUMENTAL', 'PLURAL', 'etaiḥ', 'एतैः', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'DATIVE', 'SINGULAR', 'etasmai', 'एतस्मै', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'DATIVE', 'DUAL', 'etābhyām', 'एताभ्याम्', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'DATIVE', 'PLURAL', 'etebhyaḥ', 'एतेभ्यः', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'ABLATIVE', 'SINGULAR', 'etasmāt', 'एतस्मात्', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'ABLATIVE', 'DUAL', 'etābhyām', 'एताभ्याम्', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'ABLATIVE', 'PLURAL', 'etebhyaḥ', 'एतेभ्यः', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'GENITIVE', 'SINGULAR', 'etasya', 'एतस्य', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'GENITIVE', 'DUAL', 'etayoḥ', 'एतयोः', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'GENITIVE', 'PLURAL', 'eteṣām', 'एतेषाम्', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'LOCATIVE', 'SINGULAR', 'etasmin', 'एतस्मिन्', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'LOCATIVE', 'DUAL', 'etayoḥ', 'एतयोः', 'HIGH'),
       ('etad', 'PRON_TAD_MASC', 'LOCATIVE', 'PLURAL', 'eteṣu', 'एतेषु', 'HIGH');

-- Средний род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('etad', 'PRON_TAD_NEUT', 'NOMINATIVE', 'SINGULAR', 'etat', 'एतत्', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'NOMINATIVE', 'DUAL', 'ete', 'एते', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'NOMINATIVE', 'PLURAL', 'etāni', 'एतानि', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'ACCUSATIVE', 'SINGULAR', 'etat', 'एतत्', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'ACCUSATIVE', 'DUAL', 'ete', 'एते', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'ACCUSATIVE', 'PLURAL', 'etāni', 'एतानि', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'INSTRUMENTAL', 'SINGULAR', 'etena', 'एतेन', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'INSTRUMENTAL', 'DUAL', 'etābhyām', 'एताभ्याम्', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'INSTRUMENTAL', 'PLURAL', 'etaiḥ', 'एतैः', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'DATIVE', 'SINGULAR', 'etasmai', 'एतस्मै', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'DATIVE', 'DUAL', 'etābhyām', 'एताभ्याम्', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'DATIVE', 'PLURAL', 'etebhyaḥ', 'एतेभ्यः', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'ABLATIVE', 'SINGULAR', 'etasmāt', 'एतस्मात्', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'ABLATIVE', 'DUAL', 'etābhyām', 'एताभ्याम्', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'ABLATIVE', 'PLURAL', 'etebhyaḥ', 'एतेभ्यः', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'GENITIVE', 'SINGULAR', 'etasya', 'एतस्य', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'GENITIVE', 'DUAL', 'etayoḥ', 'एतयोः', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'GENITIVE', 'PLURAL', 'eteṣām', 'एतेषाम्', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'LOCATIVE', 'SINGULAR', 'etasmin', 'एतस्मिन्', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'LOCATIVE', 'DUAL', 'etayoḥ', 'एतयोः', 'HIGH'),
       ('etad', 'PRON_TAD_NEUT', 'LOCATIVE', 'PLURAL', 'eteṣu', 'एतेषु', 'HIGH');

-- Женский род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('etad', 'PRON_TAD_FEM', 'NOMINATIVE', 'SINGULAR', 'eṣā', 'एषा', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'NOMINATIVE', 'DUAL', 'ete', 'एते', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'NOMINATIVE', 'PLURAL', 'etāḥ', 'एताः', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'ACCUSATIVE', 'SINGULAR', 'etām', 'एताम्', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'ACCUSATIVE', 'DUAL', 'ete', 'एते', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'ACCUSATIVE', 'PLURAL', 'etāḥ', 'एताः', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'INSTRUMENTAL', 'SINGULAR', 'etayā', 'एतया', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'INSTRUMENTAL', 'DUAL', 'etābhyām', 'एताभ्याम्', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'INSTRUMENTAL', 'PLURAL', 'etābhiḥ', 'एताभिः', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'DATIVE', 'SINGULAR', 'etasyai', 'एतस्यै', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'DATIVE', 'DUAL', 'etābhyām', 'एताभ्याम्', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'DATIVE', 'PLURAL', 'etābhyaḥ', 'एताभ्यः', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'ABLATIVE', 'SINGULAR', 'etasyāḥ', 'एतस्याः', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'ABLATIVE', 'DUAL', 'etābhyām', 'एताभ्याम्', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'ABLATIVE', 'PLURAL', 'etābhyaḥ', 'एताभ्यः', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'GENITIVE', 'SINGULAR', 'etasyāḥ', 'एतस्याः', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'GENITIVE', 'DUAL', 'etayoḥ', 'एतयोः', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'GENITIVE', 'PLURAL', 'etāsām', 'एतासाम्', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'LOCATIVE', 'SINGULAR', 'etasyām', 'एतस्याम्', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'LOCATIVE', 'DUAL', 'etayoḥ', 'एतयोः', 'HIGH'),
       ('etad', 'PRON_TAD_FEM', 'LOCATIVE', 'PLURAL', 'etāsu', 'एतासु', 'HIGH');


-- ===================================================================
-- 3. ОТНОСИТЕЛЬНОЕ: yad (यद् — "который")
-- ===================================================================

-- Мужской род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('yad', 'PRON_TAD_MASC', 'NOMINATIVE', 'SINGULAR', 'yaḥ', 'यः', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'NOMINATIVE', 'DUAL', 'yau', 'यौ', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'NOMINATIVE', 'PLURAL', 'ye', 'ये', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'ACCUSATIVE', 'SINGULAR', 'yam', 'यम्', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'ACCUSATIVE', 'DUAL', 'yau', 'यौ', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'ACCUSATIVE', 'PLURAL', 'yān', 'यान्', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'INSTRUMENTAL', 'SINGULAR', 'yena', 'येन', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'INSTRUMENTAL', 'DUAL', 'yābhyām', 'याभ्याम्', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'INSTRUMENTAL', 'PLURAL', 'yaiḥ', 'यैः', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'DATIVE', 'SINGULAR', 'yasmai', 'यस्मै', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'DATIVE', 'DUAL', 'yābhyām', 'याभ्याम्', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'DATIVE', 'PLURAL', 'yebhyaḥ', 'येभ्यः', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'ABLATIVE', 'SINGULAR', 'yasmāt', 'यस्मात्', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'ABLATIVE', 'DUAL', 'yābhyām', 'याभ्याम्', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'ABLATIVE', 'PLURAL', 'yebhyaḥ', 'येभ्यः', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'GENITIVE', 'SINGULAR', 'yasya', 'यस्य', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'GENITIVE', 'DUAL', 'yayoḥ', 'ययोः', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'GENITIVE', 'PLURAL', 'yeṣām', 'येषाम्', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'LOCATIVE', 'SINGULAR', 'yasmin', 'यस्मिन्', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'LOCATIVE', 'DUAL', 'yayoḥ', 'ययोः', 'HIGH'),
       ('yad', 'PRON_TAD_MASC', 'LOCATIVE', 'PLURAL', 'yeṣu', 'येषु', 'HIGH');

-- Средний род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('yad', 'PRON_TAD_NEUT', 'NOMINATIVE', 'SINGULAR', 'yat', 'यत्', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'NOMINATIVE', 'DUAL', 'ye', 'ये', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'NOMINATIVE', 'PLURAL', 'yāni', 'यानि', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'ACCUSATIVE', 'SINGULAR', 'yat', 'यत्', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'ACCUSATIVE', 'DUAL', 'ye', 'ये', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'ACCUSATIVE', 'PLURAL', 'yāni', 'यानि', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'INSTRUMENTAL', 'SINGULAR', 'yena', 'येन', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'INSTRUMENTAL', 'DUAL', 'yābhyām', 'याभ्याम्', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'INSTRUMENTAL', 'PLURAL', 'yaiḥ', 'यैः', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'DATIVE', 'SINGULAR', 'yasmai', 'यस्मै', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'DATIVE', 'DUAL', 'yābhyām', 'याभ्याम्', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'DATIVE', 'PLURAL', 'yebhyaḥ', 'येभ्यः', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'ABLATIVE', 'SINGULAR', 'yasmāt', 'यस्मात्', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'ABLATIVE', 'DUAL', 'yābhyām', 'याभ्याम्', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'ABLATIVE', 'PLURAL', 'yebhyaḥ', 'येभ्यः', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'GENITIVE', 'SINGULAR', 'yasya', 'यस्य', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'GENITIVE', 'DUAL', 'yayoḥ', 'ययोः', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'GENITIVE', 'PLURAL', 'yeṣām', 'येषाम्', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'LOCATIVE', 'SINGULAR', 'yasmin', 'यस्मिन्', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'LOCATIVE', 'DUAL', 'yayoḥ', 'ययोः', 'HIGH'),
       ('yad', 'PRON_TAD_NEUT', 'LOCATIVE', 'PLURAL', 'yeṣu', 'येषु', 'HIGH');

-- Женский род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('yad', 'PRON_TAD_FEM', 'NOMINATIVE', 'SINGULAR', 'yā', 'या', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'NOMINATIVE', 'DUAL', 'ye', 'ये', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'NOMINATIVE', 'PLURAL', 'yāḥ', 'याः', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'ACCUSATIVE', 'SINGULAR', 'yām', 'याम्', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'ACCUSATIVE', 'DUAL', 'ye', 'ये', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'ACCUSATIVE', 'PLURAL', 'yāḥ', 'याः', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'INSTRUMENTAL', 'SINGULAR', 'yayā', 'यया', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'INSTRUMENTAL', 'DUAL', 'yābhyām', 'याभ्याम्', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'INSTRUMENTAL', 'PLURAL', 'yābhiḥ', 'याभिः', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'DATIVE', 'SINGULAR', 'yasyai', 'यस्यै', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'DATIVE', 'DUAL', 'yābhyām', 'याभ्याम्', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'DATIVE', 'PLURAL', 'yābhyaḥ', 'याभ्यः', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'ABLATIVE', 'SINGULAR', 'yasyāḥ', 'यस्याः', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'ABLATIVE', 'DUAL', 'yābhyām', 'याभ्याम्', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'ABLATIVE', 'PLURAL', 'yābhyaḥ', 'याभ्यः', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'GENITIVE', 'SINGULAR', 'yasyāḥ', 'यस्याः', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'GENITIVE', 'DUAL', 'yayoḥ', 'ययोः', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'GENITIVE', 'PLURAL', 'yāsām', 'यासाम्', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'LOCATIVE', 'SINGULAR', 'yasyām', 'यस्याम्', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'LOCATIVE', 'DUAL', 'yayoḥ', 'ययोः', 'HIGH'),
       ('yad', 'PRON_TAD_FEM', 'LOCATIVE', 'PLURAL', 'yāsu', 'यासु', 'HIGH');


-- ===================================================================
-- 4. ВОПРОСИТЕЛЬНОЕ: kim (किम् — "кто? / что?")
-- ===================================================================

-- Мужской род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('kim', 'PRON_TAD_MASC', 'NOMINATIVE', 'SINGULAR', 'kaḥ', 'कः', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'NOMINATIVE', 'DUAL', 'kau', 'कौ', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'NOMINATIVE', 'PLURAL', 'ke', 'के', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'ACCUSATIVE', 'SINGULAR', 'kam', 'कम्', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'ACCUSATIVE', 'DUAL', 'kau', 'कौ', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'ACCUSATIVE', 'PLURAL', 'kān', 'कान्', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'INSTRUMENTAL', 'SINGULAR', 'kena', 'केन', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'INSTRUMENTAL', 'DUAL', 'kābhyām', 'काभ्याम्', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'INSTRUMENTAL', 'PLURAL', 'kaiḥ', 'कैः', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'DATIVE', 'SINGULAR', 'kasmai', 'कस्मै', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'DATIVE', 'DUAL', 'kābhyām', 'काभ्याम्', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'DATIVE', 'PLURAL', 'kebhyaḥ', 'केभ्यः', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'ABLATIVE', 'SINGULAR', 'kasmāt', 'कस्मात्', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'ABLATIVE', 'DUAL', 'kābhyām', 'काभ्याम्', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'ABLATIVE', 'PLURAL', 'kebhyaḥ', 'केभ्यः', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'GENITIVE', 'SINGULAR', 'kasya', 'कस्य', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'GENITIVE', 'DUAL', 'kayoḥ', 'कयोः', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'GENITIVE', 'PLURAL', 'keṣām', 'केषाम्', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'LOCATIVE', 'SINGULAR', 'kasmin', 'कस्मिन्', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'LOCATIVE', 'DUAL', 'kayoḥ', 'कयोः', 'HIGH'),
       ('kim', 'PRON_TAD_MASC', 'LOCATIVE', 'PLURAL', 'keṣu', 'केषु', 'HIGH');

-- Средний род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('kim', 'PRON_TAD_NEUT', 'NOMINATIVE', 'SINGULAR', 'kim', 'किम्', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'NOMINATIVE', 'DUAL', 'ke', 'के', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'NOMINATIVE', 'PLURAL', 'kāni', 'कानि', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'ACCUSATIVE', 'SINGULAR', 'kim', 'किम्', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'ACCUSATIVE', 'DUAL', 'ke', 'के', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'ACCUSATIVE', 'PLURAL', 'kāni', 'कानि', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'INSTRUMENTAL', 'SINGULAR', 'kena', 'केन', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'INSTRUMENTAL', 'DUAL', 'kābhyām', 'काभ्याम्', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'INSTRUMENTAL', 'PLURAL', 'kaiḥ', 'कैः', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'DATIVE', 'SINGULAR', 'kasmai', 'कस्मै', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'DATIVE', 'DUAL', 'kābhyām', 'काभ्याम्', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'DATIVE', 'PLURAL', 'kebhyaḥ', 'केभ्यः', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'ABLATIVE', 'SINGULAR', 'kasmāt', 'कस्मात्', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'ABLATIVE', 'DUAL', 'kābhyām', 'काभ्याम्', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'ABLATIVE', 'PLURAL', 'kebhyaḥ', 'केभ्यः', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'GENITIVE', 'SINGULAR', 'kasya', 'कस्य', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'GENITIVE', 'DUAL', 'kayoḥ', 'कयोः', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'GENITIVE', 'PLURAL', 'keṣām', 'केषाम्', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'LOCATIVE', 'SINGULAR', 'kasmin', 'कस्मिन्', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'LOCATIVE', 'DUAL', 'kayoḥ', 'कयोः', 'HIGH'),
       ('kim', 'PRON_TAD_NEUT', 'LOCATIVE', 'PLURAL', 'keṣu', 'केषु', 'HIGH');

-- Женский род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('kim', 'PRON_TAD_FEM', 'NOMINATIVE', 'SINGULAR', 'kā', 'का', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'NOMINATIVE', 'DUAL', 'ke', 'के', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'NOMINATIVE', 'PLURAL', 'kāḥ', 'काः', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'ACCUSATIVE', 'SINGULAR', 'kām', 'काम्', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'ACCUSATIVE', 'DUAL', 'ke', 'के', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'ACCUSATIVE', 'PLURAL', 'kāḥ', 'काः', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'INSTRUMENTAL', 'SINGULAR', 'kayā', 'कया', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'INSTRUMENTAL', 'DUAL', 'kābhyām', 'काभ्याम्', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'INSTRUMENTAL', 'PLURAL', 'kābhiḥ', 'काभिः', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'DATIVE', 'SINGULAR', 'kasyai', 'कस्यै', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'DATIVE', 'DUAL', 'kābhyām', 'काभ्याम्', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'DATIVE', 'PLURAL', 'kābhyaḥ', 'काभ्यः', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'ABLATIVE', 'SINGULAR', 'kasyāḥ', 'कस्याः', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'ABLATIVE', 'DUAL', 'kābhyām', 'काभ्याम्', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'ABLATIVE', 'PLURAL', 'kābhyaḥ', 'काभ्यः', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'GENITIVE', 'SINGULAR', 'kasyāḥ', 'कस्याः', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'GENITIVE', 'DUAL', 'kayoḥ', 'कयोः', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'GENITIVE', 'PLURAL', 'kāsām', 'कासाम्', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'LOCATIVE', 'SINGULAR', 'kasyām', 'कस्याम्', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'LOCATIVE', 'DUAL', 'kayoḥ', 'कयोः', 'HIGH'),
       ('kim', 'PRON_TAD_FEM', 'LOCATIVE', 'PLURAL', 'kāsu', 'कासु', 'HIGH');


-- ===================================================================
-- 5. НЕОПРЕДЕЛЕННОЕ: kaścit (कश्चित् — "кто-то / некий")
-- ===================================================================

-- Мужской род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('kaścit', 'PRON_TAD_MASC', 'NOMINATIVE', 'SINGULAR', 'kaścit', 'कश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'NOMINATIVE', 'DUAL', 'kaucit', 'कौचित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'NOMINATIVE', 'PLURAL', 'kecit', 'केचित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'ACCUSATIVE', 'SINGULAR', 'kañcit', 'कञ्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'ACCUSATIVE', 'DUAL', 'kaucit', 'कौचित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'ACCUSATIVE', 'PLURAL', 'kāṁścit', 'कांश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'INSTRUMENTAL', 'SINGULAR', 'kenacit', 'केनचित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'INSTRUMENTAL', 'DUAL', 'kābhyāñcit', 'काभ्याञ्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'INSTRUMENTAL', 'PLURAL', 'kaiścit', 'कैश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'DATIVE', 'SINGULAR', 'kasmaicit', 'कस्मैचित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'DATIVE', 'DUAL', 'kābhyāñcit', 'काभ्याञ्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'DATIVE', 'PLURAL', 'kebhyaścit', 'केभ्यश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'ABLATIVE', 'SINGULAR', 'kasmāccit', 'कस्माच्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'ABLATIVE', 'DUAL', 'kābhyāñcit', 'काभ्याञ्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'ABLATIVE', 'PLURAL', 'kebhyaścit', 'केभ्यश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'GENITIVE', 'SINGULAR', 'kasyacit', 'कस्यचित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'GENITIVE', 'DUAL', 'kayościt', 'कयोश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'GENITIVE', 'PLURAL', 'keṣāñcit', 'केषाञ्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'LOCATIVE', 'SINGULAR', 'kasmiṁścit', 'कस्मिंश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'LOCATIVE', 'DUAL', 'kayościt', 'कयोश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_MASC', 'LOCATIVE', 'PLURAL', 'keṣucit', 'केषुचित्', 'HIGH');

-- Средний род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('kaścit', 'PRON_TAD_NEUT', 'NOMINATIVE', 'SINGULAR', 'kiñcit', 'किञ्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'NOMINATIVE', 'DUAL', 'kecit', 'केचित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'NOMINATIVE', 'PLURAL', 'kānicit', 'कानिचित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'ACCUSATIVE', 'SINGULAR', 'kiñcit', 'किञ्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'ACCUSATIVE', 'DUAL', 'kecit', 'केचित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'ACCUSATIVE', 'PLURAL', 'kānicit', 'कानिचित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'INSTRUMENTAL', 'SINGULAR', 'kenacit', 'केनचित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'INSTRUMENTAL', 'DUAL', 'kābhyāñcit', 'काभ्याञ्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'INSTRUMENTAL', 'PLURAL', 'kaiścit', 'कैश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'DATIVE', 'SINGULAR', 'kasmaicit', 'कस्मैचित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'DATIVE', 'DUAL', 'kābhyāñcit', 'काभ्याञ्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'DATIVE', 'PLURAL', 'kebhyaścit', 'केभ्यश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'ABLATIVE', 'SINGULAR', 'kasmāccit', 'कस्माच्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'ABLATIVE', 'DUAL', 'kābhyāñcit', 'काभ्याञ्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'ABLATIVE', 'PLURAL', 'kebhyaścit', 'केभ्यश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'GENITIVE', 'SINGULAR', 'kasyacit', 'कस्यचित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'GENITIVE', 'DUAL', 'kayościt', 'कयोश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'GENITIVE', 'PLURAL', 'keṣāñcit', 'केषाञ्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'LOCATIVE', 'SINGULAR', 'kasmiṁścit', 'कस्मिंश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'LOCATIVE', 'DUAL', 'kayościt', 'कयोश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_NEUT', 'LOCATIVE', 'PLURAL', 'keṣucit', 'केषुचित्', 'HIGH');

-- Женский род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('kaścit', 'PRON_TAD_FEM', 'NOMINATIVE', 'SINGULAR', 'kācit', 'काचित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'NOMINATIVE', 'DUAL', 'kecit', 'केचित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'NOMINATIVE', 'PLURAL', 'kāścit', 'काश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'ACCUSATIVE', 'SINGULAR', 'kāñcit', 'काञ्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'ACCUSATIVE', 'DUAL', 'kecit', 'केचित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'ACCUSATIVE', 'PLURAL', 'kāścit', 'काश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'INSTRUMENTAL', 'SINGULAR', 'kayācit', 'कयाचित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'INSTRUMENTAL', 'DUAL', 'kābhyāñcit', 'काभ्याञ्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'INSTRUMENTAL', 'PLURAL', 'kābhiścit', 'काभिश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'DATIVE', 'SINGULAR', 'kasyaiit', 'कस्यैचित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'DATIVE', 'DUAL', 'kābhyāñcit', 'काभ्याञ्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'DATIVE', 'PLURAL', 'kābhyaścit', 'काभ्यश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'ABLATIVE', 'SINGULAR', 'kasyāścit', 'कस्याश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'ABLATIVE', 'DUAL', 'kābhyāñcit', 'काभ्याञ्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'ABLATIVE', 'PLURAL', 'kābhyaścit', 'काभ्यश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'GENITIVE', 'SINGULAR', 'kasyāścit', 'कस्याश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'GENITIVE', 'DUAL', 'kayościt', 'कयोश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'GENITIVE', 'PLURAL', 'kāsāñcit', 'कासाञ्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'LOCATIVE', 'SINGULAR', 'kasyāñcit', 'कस्याञ्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'LOCATIVE', 'DUAL', 'kayościt', 'कयोश्चित्', 'HIGH'),
       ('kaścit', 'PRON_TAD_FEM', 'LOCATIVE', 'PLURAL', 'kāsucit', 'कासुचित्', 'HIGH');


-- ===================================================================
-- 6. НЕОПРЕДЕЛЕННОЕ: kaścana (कश्चन — "кто-нибудь")
-- ===================================================================

-- Мужской род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('kaścana', 'PRON_TAD_MASC', 'NOMINATIVE', 'SINGULAR', 'kaścana', 'कश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'NOMINATIVE', 'DUAL', 'kaucana', 'कौचन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'NOMINATIVE', 'PLURAL', 'kecana', 'केचन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'ACCUSATIVE', 'SINGULAR', 'kañcana', 'कञ्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'ACCUSATIVE', 'DUAL', 'kaucana', 'कौचन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'ACCUSATIVE', 'PLURAL', 'kāṁścana', 'कांश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'INSTRUMENTAL', 'SINGULAR', 'kenacana', 'केनचन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'INSTRUMENTAL', 'DUAL', 'kābhyāñcana', 'काभ्याञ्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'INSTRUMENTAL', 'PLURAL', 'kaiścana', 'कैश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'DATIVE', 'SINGULAR', 'kasmaicana', 'कस्मैचन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'DATIVE', 'DUAL', 'kābhyāñcana', 'काभ्याञ्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'DATIVE', 'PLURAL', 'kebhyaścana', 'केभ्यश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'ABLATIVE', 'SINGULAR', 'kasmāccana', 'कस्माच्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'ABLATIVE', 'DUAL', 'kābhyāñcana', 'काभ्याञ्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'ABLATIVE', 'PLURAL', 'kebhyaścana', 'केभ्यश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'GENITIVE', 'SINGULAR', 'kasyacana', 'कस्यचन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'GENITIVE', 'DUAL', 'kayoścana', 'कयोश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'GENITIVE', 'PLURAL', 'keṣāñcana', 'केषाञ्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'LOCATIVE', 'SINGULAR', 'kasmiṁścana', 'कस्मिंश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'LOCATIVE', 'DUAL', 'kayoścana', 'कयोश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_MASC', 'LOCATIVE', 'PLURAL', 'keṣucana', 'केषुचन', 'HIGH');

-- Средний род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('kaścana', 'PRON_TAD_NEUT', 'NOMINATIVE', 'SINGULAR', 'kiñcana', 'किञ्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'NOMINATIVE', 'DUAL', 'kecana', 'केचन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'NOMINATIVE', 'PLURAL', 'kānicana', 'कानिचन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'ACCUSATIVE', 'SINGULAR', 'kiñcana', 'किञ्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'ACCUSATIVE', 'DUAL', 'kecana', 'केचन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'ACCUSATIVE', 'PLURAL', 'kānicana', 'कानिचन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'INSTRUMENTAL', 'SINGULAR', 'kenacana', 'केनचन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'INSTRUMENTAL', 'DUAL', 'kābhyāñcana', 'काभ्याञ्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'INSTRUMENTAL', 'PLURAL', 'kaiścana', 'कैश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'DATIVE', 'SINGULAR', 'kasmaicana', 'कस्मैचन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'DATIVE', 'DUAL', 'kābhyāñcana', 'काभ्याञ्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'DATIVE', 'PLURAL', 'kebhyaścana', 'केभ्यश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'ABLATIVE', 'SINGULAR', 'kasmāccana', 'कस्माच्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'ABLATIVE', 'DUAL', 'kābhyāñcana', 'काभ्याञ्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'ABLATIVE', 'PLURAL', 'kebhyaścana', 'केभ्यश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'GENITIVE', 'SINGULAR', 'kasyacana', 'कस्यचन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'GENITIVE', 'DUAL', 'kayoścana', 'कयोश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'GENITIVE', 'PLURAL', 'keṣāñcana', 'केषाञ्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'LOCATIVE', 'SINGULAR', 'kasmiṁścana', 'कस्मिंश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'LOCATIVE', 'DUAL', 'kayoścana', 'कयोश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_NEUT', 'LOCATIVE', 'PLURAL', 'keṣucana', 'केषुचन', 'HIGH');

-- Женский род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('kaścana', 'PRON_TAD_FEM', 'NOMINATIVE', 'SINGULAR', 'kācana', 'काचन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'NOMINATIVE', 'DUAL', 'kecana', 'केचन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'NOMINATIVE', 'PLURAL', 'kāścana', 'काश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'ACCUSATIVE', 'SINGULAR', 'kāñcana', 'काञ्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'ACCUSATIVE', 'DUAL', 'kecana', 'केचन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'ACCUSATIVE', 'PLURAL', 'kāścana', 'काश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'INSTRUMENTAL', 'SINGULAR', 'kayācana', 'कयाचन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'INSTRUMENTAL', 'DUAL', 'kābhyāñcana', 'काभ्याञ्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'INSTRUMENTAL', 'PLURAL', 'kābhiścana', 'काभिश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'DATIVE', 'SINGULAR', 'kasyaicana', 'कस्यैचन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'DATIVE', 'DUAL', 'kābhyāñcana', 'काभ्याञ्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'DATIVE', 'PLURAL', 'kābhyaścana', 'काभ्यश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'ABLATIVE', 'SINGULAR', 'kasyāścana', 'कस्याश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'ABLATIVE', 'DUAL', 'kābhyāñcana', 'काभ्याञ्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'ABLATIVE', 'PLURAL', 'kābhyaścana', 'काभ्यश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'GENITIVE', 'SINGULAR', 'kasyāścana', 'कस्याश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'GENITIVE', 'DUAL', 'kayoścana', 'कयोश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'GENITIVE', 'PLURAL', 'kāsāñcana', 'कासाञ्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'LOCATIVE', 'SINGULAR', 'kasyāñcana', 'कस्याञ्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'LOCATIVE', 'DUAL', 'kayoścana', 'कयोश्चन', 'HIGH'),
       ('kaścana', 'PRON_TAD_FEM', 'LOCATIVE', 'PLURAL', 'kāsucana', 'कासुचन', 'HIGH');


-- ===================================================================
-- 7. УКАЗАТЕЛЬНОЕ: idam (इदम् — "этот / сие")
-- ===================================================================

-- Мужской род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('idam', 'PRON_IDAM_MASC', 'NOMINATIVE', 'SINGULAR', 'ayam', 'अयम्', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'NOMINATIVE', 'DUAL', 'imau', 'इमौ', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'NOMINATIVE', 'PLURAL', 'ime', 'इमे', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'ACCUSATIVE', 'SINGULAR', 'imam', 'इमम्', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'ACCUSATIVE', 'DUAL', 'imau', 'इमौ', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'ACCUSATIVE', 'PLURAL', 'imān', 'इमान्', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'INSTRUMENTAL', 'SINGULAR', 'anena', 'अनेन', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'INSTRUMENTAL', 'DUAL', 'ābhyām', 'आभ्याम्', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'INSTRUMENTAL', 'PLURAL', 'ebhiḥ', 'एभिः', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'DATIVE', 'SINGULAR', 'asmai', 'अस्मै', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'DATIVE', 'DUAL', 'ābhyām', 'आभ्याम्', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'DATIVE', 'PLURAL', 'ebhyaḥ', 'एभ्यः', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'ABLATIVE', 'SINGULAR', 'asmāt', 'अस्मात्', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'ABLATIVE', 'DUAL', 'ābhyām', 'आभ्याम्', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'ABLATIVE', 'PLURAL', 'ebhyaḥ', 'एभ्यः', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'GENITIVE', 'SINGULAR', 'asya', 'अस्य', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'GENITIVE', 'DUAL', 'anayoḥ', 'अनयोः', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'GENITIVE', 'PLURAL', 'eṣām', 'एषाम्', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'LOCATIVE', 'SINGULAR', 'asmin', 'अस्मिन्', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'LOCATIVE', 'DUAL', 'anayoḥ', 'अनयोः', 'HIGH'),
       ('idam', 'PRON_IDAM_MASC', 'LOCATIVE', 'PLURAL', 'eṣu', 'एषु', 'HIGH');

-- Средний род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('idam', 'PRON_IDAM_NEUT', 'NOMINATIVE', 'SINGULAR', 'idam', 'इदम्', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'NOMINATIVE', 'DUAL', 'ime', 'इमे', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'NOMINATIVE', 'PLURAL', 'imāni', 'इमानि', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'ACCUSATIVE', 'SINGULAR', 'idam', 'इदम्', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'ACCUSATIVE', 'DUAL', 'ime', 'इमे', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'ACCUSATIVE', 'PLURAL', 'imāni', 'इमानि', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'INSTRUMENTAL', 'SINGULAR', 'anena', 'अनेन', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'INSTRUMENTAL', 'DUAL', 'ābhyām', 'आभ्याम्', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'INSTRUMENTAL', 'PLURAL', 'ebhiḥ', 'एभिः', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'DATIVE', 'SINGULAR', 'asmai', 'अस्मै', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'DATIVE', 'DUAL', 'ābhyām', 'आभ्याम्', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'DATIVE', 'PLURAL', 'ebhyaḥ', 'एभ्यः', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'ABLATIVE', 'SINGULAR', 'asmāt', 'अस्मात्', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'ABLATIVE', 'DUAL', 'ābhyām', 'आभ्याम्', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'ABLATIVE', 'PLURAL', 'ebhyaḥ', 'एभ्यः', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'GENITIVE', 'SINGULAR', 'asya', 'अस्य', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'GENITIVE', 'DUAL', 'anayoḥ', 'अनयोः', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'GENITIVE', 'PLURAL', 'eṣām', 'एषाम्', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'LOCATIVE', 'SINGULAR', 'asmin', 'अस्मिन्', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'LOCATIVE', 'DUAL', 'anayoḥ', 'अनयोः', 'HIGH'),
       ('idam', 'PRON_IDAM_NEUT', 'LOCATIVE', 'PLURAL', 'eṣu', 'एषु', 'HIGH');

-- Женский род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('idam', 'PRON_IDAM_FEM', 'NOMINATIVE', 'SINGULAR', 'iyam', 'इयम्', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'NOMINATIVE', 'DUAL', 'ime', 'इमे', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'NOMINATIVE', 'PLURAL', 'imāḥ', 'इमाः', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'ACCUSATIVE', 'SINGULAR', 'imām', 'इमाम', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'ACCUSATIVE', 'DUAL', 'ime', 'इमे', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'ACCUSATIVE', 'PLURAL', 'imāḥ', 'इमाः', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'INSTRUMENTAL', 'SINGULAR', 'anayā', 'अनया', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'INSTRUMENTAL', 'DUAL', 'ābhyām', 'आभ्याम्', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'INSTRUMENTAL', 'PLURAL', 'ābhiḥ', 'आभिः', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'DATIVE', 'SINGULAR', 'asyai', 'अस्यै', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'DATIVE', 'DUAL', 'ābhyām', 'आभ्याम्', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'DATIVE', 'PLURAL', 'ābhyaḥ', 'आभ्यः', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'ABLATIVE', 'SINGULAR', 'asyāḥ', 'अस्याः', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'ABLATIVE', 'DUAL', 'ābhyām', 'आभ्याम्', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'ABLATIVE', 'PLURAL', 'ābhyaḥ', 'आभ्यः', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'GENITIVE', 'SINGULAR', 'asyāḥ', 'अस्याः', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'GENITIVE', 'DUAL', 'anayoḥ', 'अनयोः', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'GENITIVE', 'PLURAL', 'āsām', 'आसाम्', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'LOCATIVE', 'SINGULAR', 'asyām', 'अस्याम्', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'LOCATIVE', 'DUAL', 'anayoḥ', 'अनयोः', 'HIGH'),
       ('idam', 'PRON_IDAM_FEM', 'LOCATIVE', 'PLURAL', 'āsu', 'आसु', 'HIGH');


-- ===================================================================
-- 8. АНАФОРИЧЕСКОЕ: ena (एन — "он / этот", повторное упоминание)
-- ===================================================================

-- Мужской род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('ena', 'PRON_IDAM_MASC', 'ACCUSATIVE', 'SINGULAR', 'enam', 'एनम्', 'HIGH'),
       ('ena', 'PRON_IDAM_MASC', 'ACCUSATIVE', 'DUAL', 'enau', 'एनौ', 'HIGH'),
       ('ena', 'PRON_IDAM_MASC', 'ACCUSATIVE', 'PLURAL', 'enān', 'एनान्', 'HIGH'),
       ('ena', 'PRON_IDAM_MASC', 'INSTRUMENTAL', 'SINGULAR', 'enena', 'एनेन', 'HIGH'),
       ('ena', 'PRON_IDAM_MASC', 'GENITIVE', 'DUAL', 'enayoḥ', 'एनयोः', 'HIGH'),
       ('ena', 'PRON_IDAM_MASC', 'LOCATIVE', 'DUAL', 'enayoḥ', 'एनयोः', 'HIGH');

-- Средний род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('ena', 'PRON_IDAM_NEUT', 'ACCUSATIVE', 'SINGULAR', 'enat', 'एनत्', 'HIGH'),
       ('ena', 'PRON_IDAM_NEUT', 'ACCUSATIVE', 'DUAL', 'ene', 'एने', 'HIGH'),
       ('ena', 'PRON_IDAM_NEUT', 'ACCUSATIVE', 'PLURAL', 'enāni', 'एनानि', 'HIGH'),
       ('ena', 'PRON_IDAM_NEUT', 'INSTRUMENTAL', 'SINGULAR', 'enena', 'एनेन', 'HIGH'),
       ('ena', 'PRON_IDAM_NEUT', 'GENITIVE', 'DUAL', 'enayoḥ', 'एनयोः', 'HIGH'),
       ('ena', 'PRON_IDAM_NEUT', 'LOCATIVE', 'DUAL', 'enayoḥ', 'एनयोः', 'HIGH');

-- Женский род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('ena', 'PRON_IDAM_FEM', 'ACCUSATIVE', 'SINGULAR', 'enām', 'एनाम', 'HIGH'),
       ('ena', 'PRON_IDAM_FEM', 'ACCUSATIVE', 'DUAL', 'ene', 'एने', 'HIGH'),
       ('ena', 'PRON_IDAM_FEM', 'ACCUSATIVE', 'PLURAL', 'enāḥ', 'एनाः', 'HIGH'),
       ('ena', 'PRON_IDAM_FEM', 'INSTRUMENTAL', 'SINGULAR', 'enayā', 'एनया', 'HIGH'),
       ('ena', 'PRON_IDAM_FEM', 'GENITIVE', 'DUAL', 'enayoḥ', 'एनयोः', 'HIGH'),
       ('ena', 'PRON_IDAM_FEM', 'LOCATIVE', 'DUAL', 'enayoḥ', 'एनयोः', 'HIGH');


-- ===================================================================
-- 9. ДАЛЕКОЕ УКАЗАТЕЛЬНОЕ: adas (अदस् — "вон тот")
-- ===================================================================

-- Мужской род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('adas', 'PRON_ADAS_MASC', 'NOMINATIVE', 'SINGULAR', 'asau', 'असौ', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'NOMINATIVE', 'DUAL', 'amū', 'अमू', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'NOMINATIVE', 'PLURAL', 'amī', 'अमी', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'ACCUSATIVE', 'SINGULAR', 'amum', 'अमुम्', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'ACCUSATIVE', 'DUAL', 'amū', 'अमू', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'ACCUSATIVE', 'PLURAL', 'amūn', 'अमून्', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'INSTRUMENTAL', 'SINGULAR', 'amunā', 'अमुना', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'INSTRUMENTAL', 'DUAL', 'amūbhyām', 'अमूभ्याम्', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'INSTRUMENTAL', 'PLURAL', 'amībhiḥ', 'अमीभिः', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'DATIVE', 'SINGULAR', 'amuṣmai', 'अमुष्मै', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'DATIVE', 'DUAL', 'amūbhyām', 'अमूभ्याम्', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'DATIVE', 'PLURAL', 'amībhyaḥ', 'अमीभ्यः', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'ABLATIVE', 'SINGULAR', 'amuṣmāt', 'अमुष्मात्', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'ABLATIVE', 'DUAL', 'amūbhyām', 'अमूभ्याम्', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'ABLATIVE', 'PLURAL', 'amībhyaḥ', 'अमीभ्यः', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'GENITIVE', 'SINGULAR', 'amuṣya', 'अमुष्य', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'GENITIVE', 'DUAL', 'amuyoḥ', 'अमुयोः', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'GENITIVE', 'PLURAL', 'amīṣām', 'अमीषाम्', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'LOCATIVE', 'SINGULAR', 'amuṣmin', 'अमुष्मिन्', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'LOCATIVE', 'DUAL', 'amuyoḥ', 'अमुयोः', 'HIGH'),
       ('adas', 'PRON_ADAS_MASC', 'LOCATIVE', 'PLURAL', 'amīṣu', 'अमीषु', 'HIGH');

-- Средний род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('adas', 'PRON_ADAS_NEUT', 'NOMINATIVE', 'SINGULAR', 'adaḥ', 'अदः', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'NOMINATIVE', 'DUAL', 'amū', 'अमू', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'NOMINATIVE', 'PLURAL', 'amūni', 'अमूनि', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'ACCUSATIVE', 'SINGULAR', 'adaḥ', 'अदः', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'ACCUSATIVE', 'DUAL', 'amū', 'अमू', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'ACCUSATIVE', 'PLURAL', 'amūni', 'अमूनि', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'INSTRUMENTAL', 'SINGULAR', 'amunā', 'अमुना', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'INSTRUMENTAL', 'DUAL', 'amūbhyām', 'अमूभ्याम्', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'INSTRUMENTAL', 'PLURAL', 'amībhiḥ', 'अमीभिः', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'DATIVE', 'SINGULAR', 'amuṣmai', 'अमुष्मै', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'DATIVE', 'DUAL', 'amūbhyām', 'अमूभ्याम्', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'DATIVE', 'PLURAL', 'amībhyaḥ', 'अमीभ्यः', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'ABLATIVE', 'SINGULAR', 'amuṣmāt', 'अमुष्मात्', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'ABLATIVE', 'DUAL', 'amūbhyām', 'अमूभ्याम्', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'ABLATIVE', 'PLURAL', 'amībhyaḥ', 'अमीभ्यः', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'GENITIVE', 'SINGULAR', 'amuṣya', 'अमुष्य', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'GENITIVE', 'DUAL', 'amuyoḥ', 'अमुयोः', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'GENITIVE', 'PLURAL', 'amīṣām', 'अमीषाम्', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'LOCATIVE', 'SINGULAR', 'amuṣmin', 'अमुष्मिन्', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'LOCATIVE', 'DUAL', 'amuyoḥ', 'अमुयोः', 'HIGH'),
       ('adas', 'PRON_ADAS_NEUT', 'LOCATIVE', 'PLURAL', 'amīṣu', 'अमीषु', 'HIGH');

-- Женский род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('adas', 'PRON_ADAS_FEM', 'NOMINATIVE', 'SINGULAR', 'asau', 'असौ', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'NOMINATIVE', 'DUAL', 'amū', 'अमू', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'NOMINATIVE', 'PLURAL', 'amūḥ', 'अमूः', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'ACCUSATIVE', 'SINGULAR', 'amūm', 'अमूम्', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'ACCUSATIVE', 'DUAL', 'amū', 'अमू', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'ACCUSATIVE', 'PLURAL', 'amūḥ', 'अमूः', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'INSTRUMENTAL', 'SINGULAR', 'amuyā', 'अमुया', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'INSTRUMENTAL', 'DUAL', 'amūbhyām', 'अमूभ्याम्', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'INSTRUMENTAL', 'PLURAL', 'amūbhiḥ', 'अमूभिः', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'DATIVE', 'SINGULAR', 'amuṣyai', 'अमुष्यै', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'DATIVE', 'DUAL', 'amūbhyām', 'अमूभ्याम्', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'DATIVE', 'PLURAL', 'amūbhyaḥ', 'अमूभ्यः', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'ABLATIVE', 'SINGULAR', 'amuṣyāḥ', 'अमुष्यः', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'ABLATIVE', 'DUAL', 'amūbhyām', 'अमूभ्याम्', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'ABLATIVE', 'PLURAL', 'amūbhyaḥ', 'अमूभ्यः', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'GENITIVE', 'SINGULAR', 'amuṣyāḥ', 'अमुष्यः', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'GENITIVE', 'DUAL', 'amuyoḥ', 'अमुयोः', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'GENITIVE', 'PLURAL', 'amūṣām', 'अमूषाम्', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'LOCATIVE', 'SINGULAR', 'amuṣyām', 'अमुष्याम्', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'LOCATIVE', 'DUAL', 'amuyoḥ', 'अमुयोः', 'HIGH'),
       ('adas', 'PRON_ADAS_FEM', 'LOCATIVE', 'PLURAL', 'amūṣu', 'अमूषु', 'HIGH');


-- ===================================================================
-- 10. МЕСТОИМЕННОЕ ПРИЛАГАТЕЛЬНОЕ: sarva (सर्व — "весь / каждый")
-- ===================================================================

-- Мужской род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('sarva', 'PRON_SARVA_MASC', 'NOMINATIVE', 'SINGULAR', 'sarvaḥ', 'सर्वः', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'NOMINATIVE', 'DUAL', 'sarvau', 'सर्वौ', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'NOMINATIVE', 'PLURAL', 'sarve', 'सर्वे', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'ACCUSATIVE', 'SINGULAR', 'sarvam', 'सर्वम्', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'ACCUSATIVE', 'DUAL', 'sarvau', 'सर्वौ', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'ACCUSATIVE', 'PLURAL', 'sarvān', 'सर्वान्', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'INSTRUMENTAL', 'SINGULAR', 'sarvena', 'सर्वेण', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'INSTRUMENTAL', 'DUAL', 'sarvābhyām', 'सर्वाभ्याम्', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'INSTRUMENTAL', 'PLURAL', 'sarvaiḥ', 'सर्वैः', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'DATIVE', 'SINGULAR', 'sarvasmai', 'सर्वस्मै', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'DATIVE', 'DUAL', 'sarvābhyām', 'सर्वाभ्याम्', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'DATIVE', 'PLURAL', 'sarvebhyaḥ', 'सर्वेभ्यः', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'ABLATIVE', 'SINGULAR', 'sarvasmāt', 'सर्वस्मात्', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'ABLATIVE', 'DUAL', 'sarvābhyām', 'सर्वाभ्याम्', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'ABLATIVE', 'PLURAL', 'sarvebhyaḥ', 'सर्वेभ्यः', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'GENITIVE', 'SINGULAR', 'sarvasya', 'सर्वस्य', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'GENITIVE', 'DUAL', 'sarvayoḥ', 'सर्वयोः', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'GENITIVE', 'PLURAL', 'sarveṣām', 'सर्वेषाम्', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'LOCATIVE', 'SINGULAR', 'sarvasmin', 'सर्वस्मिन्', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'LOCATIVE', 'DUAL', 'sarvayoḥ', 'सर्वयोः', 'HIGH'),
       ('sarva', 'PRON_SARVA_MASC', 'LOCATIVE', 'PLURAL', 'sarveṣu', 'सर्वेषु', 'HIGH');

-- Средний род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('sarva', 'PRON_SARVA_NEUT', 'NOMINATIVE', 'SINGULAR', 'sarvam', 'सर्वम्', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'NOMINATIVE', 'DUAL', 'sarve', 'सर्वे', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'NOMINATIVE', 'PLURAL', 'sarvāni', 'सर्वाणि', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'ACCUSATIVE', 'SINGULAR', 'sarvam', 'सर्वम्', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'ACCUSATIVE', 'DUAL', 'sarve', 'सर्वे', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'ACCUSATIVE', 'PLURAL', 'sarvāni', 'सर्वाणि', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'INSTRUMENTAL', 'SINGULAR', 'sarvena', 'सर्वेण', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'INSTRUMENTAL', 'DUAL', 'sarvābhyām', 'सर्वाभ्याम्', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'INSTRUMENTAL', 'PLURAL', 'sarvaiḥ', 'सर्वैः', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'DATIVE', 'SINGULAR', 'sarvasmai', 'सर्वस्मै', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'DATIVE', 'DUAL', 'sarvābhyām', 'सर्वाभ्याम्', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'DATIVE', 'PLURAL', 'sarvebhyaḥ', 'सर्वेभ्यः', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'ABLATIVE', 'SINGULAR', 'sarvasmāt', 'सर्वस्मात्', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'ABLATIVE', 'DUAL', 'sarvābhyām', 'सर्वाभ्याम्', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'ABLATIVE', 'PLURAL', 'sarvebhyaḥ', 'सर्वेभ्यः', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'GENITIVE', 'SINGULAR', 'sarvasya', 'सर्वस्य', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'GENITIVE', 'DUAL', 'sarvayoḥ', 'सर्वयोः', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'GENITIVE', 'PLURAL', 'sarveṣām', 'सर्वेषाम्', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'LOCATIVE', 'SINGULAR', 'sarvasmin', 'सर्वस्मिन्', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'LOCATIVE', 'DUAL', 'sarvayoḥ', 'सर्वयोः', 'HIGH'),
       ('sarva', 'PRON_SARVA_NEUT', 'LOCATIVE', 'PLURAL', 'sarveṣu', 'सर्वेषु', 'HIGH');

-- Женский род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('sarva', 'PRON_SARVA_FEM', 'NOMINATIVE', 'SINGULAR', 'sarvā', 'सर्वा', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'NOMINATIVE', 'DUAL', 'sarve', 'सर्वे', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'NOMINATIVE', 'PLURAL', 'sarvāḥ', 'सर्वाः', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'ACCUSATIVE', 'SINGULAR', 'sarvām', 'सर्वाम्', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'ACCUSATIVE', 'DUAL', 'sarve', 'सर्वे', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'ACCUSATIVE', 'PLURAL', 'sarvāḥ', 'सर्वाः', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'INSTRUMENTAL', 'SINGULAR', 'sarvayā', 'सर्वया', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'INSTRUMENTAL', 'DUAL', 'sarvābhyām', 'सर्वाभ्याम्', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'INSTRUMENTAL', 'PLURAL', 'sarvābhiḥ', 'सर्वाभिः', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'DATIVE', 'SINGULAR', 'sarvasyai', 'सर्वस्यै', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'DATIVE', 'DUAL', 'sarvābhyām', 'सर्वाभ्याम्', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'DATIVE', 'PLURAL', 'sarvābhyaḥ', 'सर्वाभ्यः', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'ABLATIVE', 'SINGULAR', 'sarvasyāḥ', 'सर्वस्याः', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'ABLATIVE', 'DUAL', 'sarvābhyām', 'सर्वाभ्याम्', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'ABLATIVE', 'PLURAL', 'sarvābhyaḥ', 'सर्वाभ्यः', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'GENITIVE', 'SINGULAR', 'sarvasyāḥ', 'सर्वस्याः', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'GENITIVE', 'DUAL', 'sarvayoḥ', 'सर्वयोः', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'GENITIVE', 'PLURAL', 'sarvāsām', 'सर्वासाम्', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'LOCATIVE', 'SINGULAR', 'sarvasyām', 'सर्वस्याम्', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'LOCATIVE', 'DUAL', 'sarvayoḥ', 'सर्वयोः', 'HIGH'),
       ('sarva', 'PRON_SARVA_FEM', 'LOCATIVE', 'PLURAL', 'sarvāsu', 'सर्वासु', 'HIGH');


-- ===================================================================
-- 11. ПОЛУ-МЕСТОИМЕННОЕ: pūrva (पूर्व — "прежний / передний")
-- ===================================================================

-- Мужской род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('pūrva', 'PRON_PURVA_MASC', 'NOMINATIVE', 'SINGULAR', 'pūrvaḥ', 'पूर्वः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'NOMINATIVE', 'DUAL', 'pūrvau', 'पूर्वौ', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'NOMINATIVE', 'PLURAL', 'pūrve', 'पूर्वे', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'ACCUSATIVE', 'SINGULAR', 'pūrvam', 'पूर्वम्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'ACCUSATIVE', 'DUAL', 'pūrvau', 'पूर्वौ', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'ACCUSATIVE', 'PLURAL', 'pūrvān', 'पूर्वान्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'INSTRUMENTAL', 'SINGULAR', 'pūrvena', 'पूर्वेण', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'INSTRUMENTAL', 'DUAL', 'pūrvābhyām', 'पूर्वाभ्याम्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'INSTRUMENTAL', 'PLURAL', 'pūrvaiḥ', 'पूर्वैः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'DATIVE', 'SINGULAR', 'pūrvasmai', 'पूर्वस्मै', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'DATIVE', 'DUAL', 'pūrvābhyām', 'पूर्वाभ्याम्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'DATIVE', 'PLURAL', 'pūrvebhyaḥ', 'पूर्वेभ्यः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'ABLATIVE', 'SINGULAR', 'pūrvasmāt', 'पूर्वस्मात्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'ABLATIVE', 'DUAL', 'pūrvābhyām', 'पूर्वाभ्याम्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'ABLATIVE', 'PLURAL', 'pūrvebhyaḥ', 'पूर्वेभ्यः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'GENITIVE', 'SINGULAR', 'pūrvasya', 'पूर्वस्य', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'GENITIVE', 'DUAL', 'pūrvayoḥ', 'पूर्वयोः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'GENITIVE', 'PLURAL', 'pūrveṣām', 'पूर्वेषाम्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'LOCATIVE', 'SINGULAR', 'pūrvasmin', 'पूर्वस्मिन्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'LOCATIVE', 'DUAL', 'pūrvayoḥ', 'पूर्वयोः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_MASC', 'LOCATIVE', 'PLURAL', 'pūrveṣu', 'पूर्वेषु', 'HIGH');

-- Средний род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('pūrva', 'PRON_PURVA_NEUT', 'NOMINATIVE', 'SINGULAR', 'pūrvam', 'पूर्वम्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'NOMINATIVE', 'DUAL', 'pūrve', 'पूर्वे', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'NOMINATIVE', 'PLURAL', 'pūrvāṇi', 'पूर्वाणि', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'ACCUSATIVE', 'SINGULAR', 'pūrvam', 'पूर्वम्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'ACCUSATIVE', 'DUAL', 'pūrve', 'पूर्वे', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'ACCUSATIVE', 'PLURAL', 'pūrvāṇi', 'पूर्वाणि', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'INSTRUMENTAL', 'SINGULAR', 'pūrvena', 'पूर्वेण', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'INSTRUMENTAL', 'DUAL', 'pūrvābhyām', 'पूर्वाभ्याम्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'INSTRUMENTAL', 'PLURAL', 'pūrvaiḥ', 'पूर्वैः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'DATIVE', 'SINGULAR', 'pūrvasmai', 'पूर्वस्मै', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'DATIVE', 'DUAL', 'pūrvābhyām', 'पूर्वाभ्याम्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'DATIVE', 'PLURAL', 'pūrvebhyaḥ', 'पूर्वेभ्यः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'ABLATIVE', 'SINGULAR', 'pūrvasmāt', 'पूर्वस्मात्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'ABLATIVE', 'DUAL', 'pūrvābhyām', 'पूर्वाभ्याम्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'ABLATIVE', 'PLURAL', 'pūrvebhyaḥ', 'पूर्वेभ्यः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'GENITIVE', 'SINGULAR', 'pūrvasya', 'पूर्वस्य', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'GENITIVE', 'DUAL', 'pūrvayoḥ', 'पूर्वयोः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'GENITIVE', 'PLURAL', 'pūrveṣām', 'पूर्वेषाम्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'LOCATIVE', 'SINGULAR', 'pūrvasmin', 'पूर्वस्मिन्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'LOCATIVE', 'DUAL', 'pūrvayoḥ', 'पूर्वयोः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_NEUT', 'LOCATIVE', 'PLURAL', 'pūrveṣu', 'पूर्वेषु', 'HIGH');

-- Женский род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('pūrva', 'PRON_PURVA_FEM', 'NOMINATIVE', 'SINGULAR', 'pūrvā', 'पूर्वा', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'NOMINATIVE', 'DUAL', 'pūrve', 'पूर्वे', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'NOMINATIVE', 'PLURAL', 'pūrvāḥ', 'पूर्वाः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'ACCUSATIVE', 'SINGULAR', 'pūrvām', 'पूर्वाम्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'ACCUSATIVE', 'DUAL', 'pūrve', 'पूर्वे', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'ACCUSATIVE', 'PLURAL', 'pūrvāḥ', 'पूर्वाः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'INSTRUMENTAL', 'SINGULAR', 'pūrvayā', 'पूर्वया', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'INSTRUMENTAL', 'DUAL', 'pūrvābhyām', 'पूर्वाभ्याम्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'INSTRUMENTAL', 'PLURAL', 'pūrvābhiḥ', 'पूर्वाभिः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'DATIVE', 'SINGULAR', 'pūrvasyai', 'पूर्वस्यै', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'DATIVE', 'DUAL', 'pūrvābhyām', 'पूर्वाभ्याम्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'DATIVE', 'PLURAL', 'pūrvābhyaḥ', 'पूर्वाभ्यः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'ABLATIVE', 'SINGULAR', 'pūrvasyāḥ', 'पूर्वस्याः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'ABLATIVE', 'DUAL', 'pūrvābhyām', 'पूर्वाभ्याम्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'ABLATIVE', 'PLURAL', 'pūrvābhyaḥ', 'पूर्वाभ्यः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'GENITIVE', 'SINGULAR', 'pūrvasyāḥ', 'पूर्वस्याः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'GENITIVE', 'DUAL', 'pūrvayoḥ', 'पूर्वयोः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'GENITIVE', 'PLURAL', 'pūrvāsām', 'पूर्वासाम्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'LOCATIVE', 'SINGULAR', 'pūrvasyām', 'पूर्वस्याम्', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'LOCATIVE', 'DUAL', 'pūrvayoḥ', 'पूर्वयोः', 'HIGH'),
       ('pūrva', 'PRON_PURVA_FEM', 'LOCATIVE', 'PLURAL', 'pūrvāsu', 'पूर्वासु', 'HIGH');


-- ===================================================================
-- 12. ВОЗВРАТНОЕ: ātman (आत्मन् — "сам / себя")
-- ===================================================================

INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('ātman', 'PRON_AN', 'NOMINATIVE', 'SINGULAR', 'ātmā', 'आत्मा', 'HIGH'),
       ('ātman', 'PRON_AN', 'NOMINATIVE', 'DUAL', 'ātmānau', 'आत्मानौ', 'HIGH'),
       ('ātman', 'PRON_AN', 'NOMINATIVE', 'PLURAL', 'ātmānaḥ', 'आत्मानः', 'HIGH'),
       ('ātman', 'PRON_AN', 'ACCUSATIVE', 'SINGULAR', 'ātmānam', 'आत्मानम्', 'HIGH'),
       ('ātman', 'PRON_AN', 'ACCUSATIVE', 'DUAL', 'ātmānau', 'आत्मानौ', 'HIGH'),
       ('ātman', 'PRON_AN', 'ACCUSATIVE', 'PLURAL', 'ātmanaḥ', 'आत्मनः', 'HIGH'),
       ('ātman', 'PRON_AN', 'INSTRUMENTAL', 'SINGULAR', 'ātmanā', 'आत्मना', 'HIGH'),
       ('ātman', 'PRON_AN', 'INSTRUMENTAL', 'DUAL', 'ātmabhyām', 'आत्मभ्याम्', 'HIGH'),
       ('ātman', 'PRON_AN', 'INSTRUMENTAL', 'PLURAL', 'ātmabhiḥ', 'आत्मभिः', 'HIGH'),
       ('ātman', 'PRON_AN', 'DATIVE', 'SINGULAR', 'ātmane', 'आत्मने', 'HIGH'),
       ('ātman', 'PRON_AN', 'DATIVE', 'DUAL', 'ātmabhyām', 'आत्मभ्याम्', 'HIGH'),
       ('ātman', 'PRON_AN', 'DATIVE', 'PLURAL', 'ātmabhyaḥ', 'आत्मभ्यः', 'HIGH'),
       ('ātman', 'PRON_AN', 'ABLATIVE', 'SINGULAR', 'ātmanaḥ', 'आत्मनः', 'HIGH'),
       ('ātman', 'PRON_AN', 'ABLATIVE', 'DUAL', 'ātmabhyām', 'आत्मभ्याम्', 'HIGH'),
       ('ātman', 'PRON_AN', 'ABLATIVE', 'PLURAL', 'ātmabhyaḥ', 'आत्मभ्यः', 'HIGH'),
       ('ātman', 'PRON_AN', 'GENITIVE', 'SINGULAR', 'ātmanaḥ', 'आत्मनः', 'HIGH'),
       ('ātman', 'PRON_AN', 'GENITIVE', 'DUAL', 'ātmanoḥ', 'आत्मनोः', 'HIGH'),
       ('ātman', 'PRON_AN', 'GENITIVE', 'PLURAL', 'ātmanām', 'आत्मनाम्', 'HIGH'),
       ('ātman', 'PRON_AN', 'LOCATIVE', 'SINGULAR', 'ātmani', 'आत्मनि', 'HIGH'),
       ('ātman', 'PRON_AN', 'LOCATIVE', 'DUAL', 'ātmanoḥ', 'आत्मनोः', 'HIGH'),
       ('ātman', 'PRON_AN', 'LOCATIVE', 'PLURAL', 'ātmasu', 'आत्मसु', 'HIGH');


-- ===================================================================
-- 13. УВАЖИТЕЛЬНОЕ ЛИЧНОЕ: bhavat (भवत् — "Вы")
-- ===================================================================

-- Мужской род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('bhavat', 'PRON_VAT_MASC', 'NOMINATIVE', 'SINGULAR', 'bhavān', 'भवान्', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'NOMINATIVE', 'DUAL', 'bhavantau', 'भवन्तौ', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'NOMINATIVE', 'PLURAL', 'bhavantaḥ', 'भवन्तः', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'ACCUSATIVE', 'SINGULAR', 'bhavantam', 'भवन्तम्', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'ACCUSATIVE', 'DUAL', 'bhavantau', 'भवन्तौ', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'ACCUSATIVE', 'PLURAL', 'bhavataḥ', 'भवतः', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'INSTRUMENTAL', 'SINGULAR', 'bhavatā', 'भवता', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'INSTRUMENTAL', 'DUAL', 'bhavadbhyām', 'भवद्भ्याम्', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'INSTRUMENTAL', 'PLURAL', 'bhavadbhiḥ', 'भवद्भिः', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'DATIVE', 'SINGULAR', 'bhavate', 'भवते', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'DATIVE', 'DUAL', 'bhavadbhyām', 'भवद्भ्याम्', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'DATIVE', 'PLURAL', 'bhavadbhyaḥ', 'भवद्भ्यः', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'ABLATIVE', 'SINGULAR', 'bhavataḥ', 'भवतः', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'ABLATIVE', 'DUAL', 'bhavadbhyām', 'भवद्भ्याम्', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'ABLATIVE', 'PLURAL', 'bhavadbhyaḥ', 'भवद्भ्यः', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'GENITIVE', 'SINGULAR', 'bhavataḥ', 'भवतः', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'GENITIVE', 'DUAL', 'bhavatoḥ', 'भवतोः', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'GENITIVE', 'PLURAL', 'bhavatām', 'भवताम्', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'LOCATIVE', 'SINGULAR', 'bhavati', 'भवति', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'LOCATIVE', 'DUAL', 'bhavatoḥ', 'भवतोः', 'HIGH'),
       ('bhavat', 'PRON_VAT_MASC', 'LOCATIVE', 'PLURAL', 'bhavatsu', 'भवत्सु', 'HIGH');

-- Женский род (bhavatī)
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('bhavat', 'PRON_VAT_FEM', 'NOMINATIVE', 'SINGULAR', 'bhavatī', 'भवती', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'NOMINATIVE', 'DUAL', 'bhavatyau', 'भवत्यौ', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'NOMINATIVE', 'PLURAL', 'bhavatyaḥ', 'भवत्यः', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'ACCUSATIVE', 'SINGULAR', 'bhavatīm', 'भवतीम्', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'ACCUSATIVE', 'DUAL', 'bhavatyau', 'भवत्यौ', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'ACCUSATIVE', 'PLURAL', 'bhavatīḥ', 'भवतीः', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'INSTRUMENTAL', 'SINGULAR', 'bhavatyā', 'भवत्या', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'INSTRUMENTAL', 'DUAL', 'bhavatībhyām', 'भवतीभ्याम्', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'INSTRUMENTAL', 'PLURAL', 'bhavatībhiḥ', 'भवतीभिः', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'DATIVE', 'SINGULAR', 'bhavatyai', 'भवत्यै', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'DATIVE', 'DUAL', 'bhavatībhyām', 'भवतीभ्याम्', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'DATIVE', 'PLURAL', 'bhavatībhyaḥ', 'भवतीभ्यः', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'ABLATIVE', 'SINGULAR', 'bhavatyāḥ', 'भवत्याः', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'ABLATIVE', 'DUAL', 'bhavatībhyām', 'भवतीभ्याम्', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'ABLATIVE', 'PLURAL', 'bhavatībhyaḥ', 'भवतीभ्यः', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'GENITIVE', 'SINGULAR', 'bhavatyāḥ', 'भवत्याः', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'GENITIVE', 'DUAL', 'bhavatyoḥ', 'भवत्योः', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'GENITIVE', 'PLURAL', 'bhavatīnām', 'भवतीनाम्', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'LOCATIVE', 'SINGULAR', 'bhavatyām', 'भवत्याम्', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'LOCATIVE', 'DUAL', 'bhavatyoḥ', 'भवत्योः', 'HIGH'),
       ('bhavat', 'PRON_VAT_FEM', 'LOCATIVE', 'PLURAL', 'bhavatīṣu', 'भवतीषु', 'HIGH');


-- ===================================================================
-- 14. ВОПРОСИТЕЛЬНОЕ / КОЛИЧЕСТВЕННОЕ: kati (कति — "сколько?")
-- ===================================================================

INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('kati', 'PRON_KATI', 'NOMINATIVE', 'PLURAL', 'kati', 'कति', 'HIGH'),
       ('kati', 'PRON_KATI', 'ACCUSATIVE', 'PLURAL', 'kati', 'कति', 'HIGH'),
       ('kati', 'PRON_KATI', 'INSTRUMENTAL', 'PLURAL', 'katibhiḥ', 'कतिभिः', 'HIGH'),
       ('kati', 'PRON_KATI', 'DATIVE', 'PLURAL', 'katibhyaḥ', 'कतिभ्यः', 'HIGH'),
       ('kati', 'PRON_KATI', 'ABLATIVE', 'PLURAL', 'katibhyaḥ', 'कतिभ्यः', 'HIGH'),
       ('kati', 'PRON_KATI', 'GENITIVE', 'PLURAL', 'katīnām', 'कतीनाम्', 'HIGH'),
       ('kati', 'PRON_KATI', 'LOCATIVE', 'PLURAL', 'katiṣu', 'कतिषु', 'HIGH');


-- ===================================================================
-- 15. КОЛИЧЕСТВЕННОЕ: ubha (उभ — "оба")
-- ===================================================================

-- Мужской род
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('ubha', 'PRON_UBHA_MASC', 'NOMINATIVE', 'DUAL', 'ubhau', 'उभौ', 'HIGH'),
       ('ubha', 'PRON_UBHA_MASC', 'ACCUSATIVE', 'DUAL', 'ubhau', 'उभौ', 'HIGH'),
       ('ubha', 'PRON_UBHA_MASC', 'INSTRUMENTAL', 'DUAL', 'ubhābhyām', 'उभाभ्याम्', 'HIGH'),
       ('ubha', 'PRON_UBHA_MASC', 'DATIVE', 'DUAL', 'ubhābhyām', 'उभाभ्याम्', 'HIGH'),
       ('ubha', 'PRON_UBHA_MASC', 'ABLATIVE', 'DUAL', 'ubhābhyām', 'उभाभ्याम्', 'HIGH'),
       ('ubha', 'PRON_UBHA_MASC', 'GENITIVE', 'DUAL', 'ubhayoḥ', 'उभयोः', 'HIGH'),
       ('ubha', 'PRON_UBHA_MASC', 'LOCATIVE', 'DUAL', 'ubhayoḥ', 'उभयोः', 'HIGH');

-- Женский и Средний род (идентичные формы)
INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES ('ubha', 'PRON_UBHA_FN', 'NOMINATIVE', 'DUAL', 'ubhe', 'उभे', 'HIGH'),
       ('ubha', 'PRON_UBHA_FN', 'ACCUSATIVE', 'DUAL', 'ubhe', 'उभे', 'HIGH'),
       ('ubha', 'PRON_UBHA_FN', 'INSTRUMENTAL', 'DUAL', 'ubhābhyām', 'उभाभ्याम्', 'HIGH'),
       ('ubha', 'PRON_UBHA_FN', 'DATIVE', 'DUAL', 'ubhābhyām', 'उभाभ्याम्', 'HIGH'),
       ('ubha', 'PRON_UBHA_FN', 'ABLATIVE', 'DUAL', 'ubhābhyām', 'उभाभ्याम्', 'HIGH'),
       ('ubha', 'PRON_UBHA_FN', 'GENITIVE', 'DUAL', 'ubhayoḥ', 'उभयोः', 'HIGH'),
       ('ubha', 'PRON_UBHA_FN', 'LOCATIVE', 'DUAL', 'ubhayoḥ', 'उभयोः', 'HIGH');



INSERT INTO "curriculum"."declension_form" ("lemma_iast", "vowel_type", "case_type", "number_type", "form_iast",
                                            "form_devanagari", "confidence")
VALUES
-- =================================================================
-- PRON_ASMAD (1-е лицо: Я / Мы)
-- =================================================================
-- Singular
('asmad', 'PRON_ASMAD', 'NOMINATIVE', 'SINGULAR', 'aham', 'अहम्', 'HIGH'),
('asmad', 'PRON_ASMAD', 'ACCUSATIVE', 'SINGULAR', 'mām / mā', 'माम् / मा', 'HIGH'),
('asmad', 'PRON_ASMAD', 'INSTRUMENTAL', 'SINGULAR', 'mayā', 'मया', 'HIGH'),
('asmad', 'PRON_ASMAD', 'DATIVE', 'SINGULAR', 'mahyam / me', 'मह्यम् / मे', 'HIGH'),
('asmad', 'PRON_ASMAD', 'ABLATIVE', 'SINGULAR', 'mat', 'मत्', 'HIGH'),
('asmad', 'PRON_ASMAD', 'GENITIVE', 'SINGULAR', 'mama / me', 'मम / मे', 'HIGH'),
('asmad', 'PRON_ASMAD', 'LOCATIVE', 'SINGULAR', 'mayi', 'मयि', 'HIGH'),

-- Dual
('asmad', 'PRON_ASMAD', 'NOMINATIVE', 'DUAL', 'āvām', 'आवाम्', 'HIGH'),
('asmad', 'PRON_ASMAD', 'ACCUSATIVE', 'DUAL', 'āvām / nau', 'आवाम् / नौ', 'HIGH'),
('asmad', 'PRON_ASMAD', 'INSTRUMENTAL', 'DUAL', 'āvābhyām', 'आवाभ्याम्', 'HIGH'),
('asmad', 'PRON_ASMAD', 'DATIVE', 'DUAL', 'āvābhyām / nau', 'आवाभ्याम् / नौ', 'HIGH'),
('asmad', 'PRON_ASMAD', 'ABLATIVE', 'DUAL', 'āvābhyām', 'आवाभ्याम्', 'HIGH'),
('asmad', 'PRON_ASMAD', 'GENITIVE', 'DUAL', 'āvayoḥ / nau', 'आवयोः / नौ', 'HIGH'),
('asmad', 'PRON_ASMAD', 'LOCATIVE', 'DUAL', 'āvayoḥ', 'आवयोः', 'HIGH'),

-- Plural
('asmad', 'PRON_ASMAD', 'NOMINATIVE', 'PLURAL', 'vayam', 'वयम्', 'HIGH'),
('asmad', 'PRON_ASMAD', 'ACCUSATIVE', 'PLURAL', 'asmān / naḥ', 'अस्मान् / नः', 'HIGH'),
('asmad', 'PRON_ASMAD', 'INSTRUMENTAL', 'PLURAL', 'asmābhiḥ', 'अस्माभिः', 'HIGH'),
('asmad', 'PRON_ASMAD', 'DATIVE', 'PLURAL', 'asmabhyam / naḥ', 'अस्मभ्यम् / नः', 'HIGH'),
('asmad', 'PRON_ASMAD', 'ABLATIVE', 'PLURAL', 'asmat', 'अस्मत्', 'HIGH'),
('asmad', 'PRON_ASMAD', 'GENITIVE', 'PLURAL', 'asmākam / naḥ', 'अस्माकम् / नः', 'HIGH'),
('asmad', 'PRON_ASMAD', 'LOCATIVE', 'PLURAL', 'asmāsu', 'अस्मासु', 'HIGH'),

-- =================================================================
-- PRON_YUSMAD (2-е лицо: Ты / Вы)
-- =================================================================
-- Singular
('yuṣmad', 'PRON_YUSMAD', 'NOMINATIVE', 'SINGULAR', 'tvam', 'त्वम्', 'HIGH'),
('yuṣmad', 'PRON_YUSMAD', 'ACCUSATIVE', 'SINGULAR', 'tvām / tvā', 'त्वाम् / त्वा', 'HIGH'),
('yuṣmad', 'PRON_YUSMAD', 'INSTRUMENTAL', 'SINGULAR', 'tvayā', 'त्वया', 'HIGH'),
('yuṣmad', 'PRON_YUSMAD', 'DATIVE', 'SINGULAR', 'tubhyam / te', 'तुभ्यम् / ते', 'HIGH'),
('yuṣmad', 'PRON_YUSMAD', 'ABLATIVE', 'SINGULAR', 'tvat', 'त्वत्', 'HIGH'),
('yuṣmad', 'PRON_YUSMAD', 'GENITIVE', 'SINGULAR', 'tava / te', 'तव / ते', 'HIGH'),
('yuṣmad', 'PRON_YUSMAD', 'LOCATIVE', 'SINGULAR', 'tvayi', 'त्वयि', 'HIGH'),

-- Dual
('yuṣmad', 'PRON_YUSMAD', 'NOMINATIVE', 'DUAL', 'yuvām', 'युवाम्', 'HIGH'),
('yuṣmad', 'PRON_YUSMAD', 'ACCUSATIVE', 'DUAL', 'yuvām / vām', 'युवाम् / वाम्', 'HIGH'),
('yuṣmad', 'PRON_YUSMAD', 'INSTRUMENTAL', 'DUAL', 'yuvābhyām', 'युवाभ्याम्', 'HIGH'),
('yuṣmad', 'PRON_YUSMAD', 'DATIVE', 'DUAL', 'yuvābhyām / vām', 'युवाभ्याम् / वाम्', 'HIGH'),
('yuṣmad', 'PRON_YUSMAD', 'ABLATIVE', 'DUAL', 'yuvābhyām', 'युवाभ्याम्', 'HIGH'),
('yuṣmad', 'PRON_YUSMAD', 'GENITIVE', 'DUAL', 'yuvayoḥ / vām', 'युवयोः / वाम्', 'HIGH'),
('yuṣmad', 'PRON_YUSMAD', 'LOCATIVE', 'DUAL', 'yuvayoḥ', 'युवयोः', 'HIGH'),

-- Plural
('yuṣmad', 'PRON_YUSMAD', 'NOMINATIVE', 'PLURAL', 'yūyam', 'यूयम्', 'HIGH'),
('yuṣmad', 'PRON_YUSMAD', 'ACCUSATIVE', 'PLURAL', 'yuṣmān / vaḥ', 'युष्मान् / वः', 'HIGH'),
('yuṣmad', 'PRON_YUSMAD', 'INSTRUMENTAL', 'PLURAL', 'yuṣmābhiḥ', 'युष्माभिः', 'HIGH'),
('yuṣmad', 'PRON_YUSMAD', 'DATIVE', 'PLURAL', 'yuṣmabhyam / vaḥ', 'युष्मभ्यम् / वः', 'HIGH'),
('yuṣmad', 'PRON_YUSMAD', 'ABLATIVE', 'PLURAL', 'yuṣmat', 'युष्मत्', 'HIGH'),
('yuṣmad', 'PRON_YUSMAD', 'GENITIVE', 'PLURAL', 'yuṣmākam / vaḥ', 'युष्माकम् / वः', 'HIGH'),
('yuṣmad', 'PRON_YUSMAD', 'LOCATIVE', 'PLURAL', 'yuṣmāsu', 'युष्मासु', 'HIGH');