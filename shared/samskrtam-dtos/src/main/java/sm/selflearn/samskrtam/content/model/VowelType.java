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
    // Pronouns — suppletive and pronominal paradigms, keyed by base + gender (ADR-008)
    PRON_TAD_MASC,   // tad/etad/yad/kim/kaścit/kaścana — класс. парадигма на -a/-ā, м.р.
    PRON_TAD_NEUT,   // tad/etad/yad/kim/kaścit/kaścana — ср.р.
    PRON_TAD_FEM,    // tad/etad/yad/kim/kaścit/kaścana — ж.р.
    PRON_IDAM_MASC,  // idam/ena — указательное/анафорическое, м.р.
    PRON_IDAM_NEUT,  // idam/ena — ср.р.
    PRON_IDAM_FEM,   // idam/ena — ж.р.
    PRON_ADAS_MASC,  // adas — далёкое указательное, м.р.
    PRON_ADAS_NEUT,  // adas — ср.р.
    PRON_ADAS_FEM,   // adas — ж.р.
    PRON_ASMAD,      // asmad (ахам) — личное 1-го лица, супплетивное, вне родовой парадигмы
    PRON_YUSMAD,     // yuṣmad (твам) — личное 2-го лица, супплетивное, вне родовой парадигмы
    PRON_SARVA_MASC, // sarva — местоименное прилагательное, м.р.
    PRON_SARVA_NEUT, // sarva — ср.р.
    PRON_SARVA_FEM,  // sarva — ж.р.
    PRON_PURVA_MASC, // pūrva — полу-местоименное, м.р.
    PRON_PURVA_NEUT, // pūrva — ср.р.
    PRON_PURVA_FEM,  // pūrva — ж.р.
    PRON_VAT_MASC,   // bhavat — уважительное, м.р.
    PRON_VAT_FEM,    // bhavat — ж.р. (ср.р. не используется)
    PRON_UBHA_MASC,  // ubha — количественное (дв.), м.р.
    PRON_UBHA_FN,    // ubha — ж.р./ср.р. (формы идентичны)
    PRON_AN,         // ātman — возвратное, единый род
    PRON_KATI        // kati — вопр./колич., одна форма всех родов (мн.ч.)
}

