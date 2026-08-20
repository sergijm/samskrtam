package sm.selflearn.samskrtam.content.model;

public enum VowelType {
    A_STEM,
    AA_STEM, // -ā
    I_STEM,
    II_STEM, // -ī
    U_STEM,
    UU_STEM, // -ū
    R_STEM,  // -ṛ
    // Consonant-final classes (paradigm expansion)
    IN_STEM,   // -in (yogin, mālin)
    AN_STEM,   // -an (rājan, ātman, nāman, brahman)
    AS_STEM,   // -as (manas, tapas, tejas)
    IS_STEM,   // -is (havis, varcis)
    US_STEM,   // -us (cakṣus)
    ANT_STEM,  // -ant/-at present participles (bhavant, gacchant)
    VAT_STEM,  // -vat/-mant (bhagavat, guṇavat)
    ROOT_STEM, // root/consonant-final stems (vāc, marut, āp, dhi)
    O_STEM,    // -o (go)
    AU_STEM,   // -au (nau, glāu)
    // Pronouns (ADR-008)
    PRON_AHAM,       // ахам (я)
    PRON_TVAM,       // твам (ты)
    PRON_TAD,        // tad (тот, он) — указательное
    PRON_ETAD,       // etad (этот) — указательное ближнее
    PRON_IDAM,       // idam (этот) — указательное ближнее
    PRON_KIM,        // kim (кто? что?) — вопросительное
    PRON_YAD,        // yad (который) — относительное
    PRON_REFLEXIVE   // возвратное местоимение
}

