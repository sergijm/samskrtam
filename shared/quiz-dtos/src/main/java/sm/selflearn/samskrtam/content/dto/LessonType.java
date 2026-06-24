package sm.selflearn.samskrtam.content.dto;

public enum LessonType {
    DECLENSIONS, // Combined declensions quiz
    A_STEM_DECLENSIONS,
    AA_STEM_DECLENSIONS,
    I_STEM_DECLENSIONS,
    II_STEM_DECLENSIONS,
    U_STEM_DECLENSIONS,
    UU_STEM_DECLENSIONS,
    R_STEM_DECLENSIONS,
    CONJUGATIONS,
    VOCABULARY,
    VOCABULARY_BASIC,
    VOCABULARY_TEXTS;

    public static boolean isDeclensions(LessonType qt) {
        return qt.equals(DECLENSIONS) ||
                qt.equals(A_STEM_DECLENSIONS) ||
                qt.equals(AA_STEM_DECLENSIONS) ||
                qt.equals(I_STEM_DECLENSIONS) ||
                qt.equals(II_STEM_DECLENSIONS) ||
                qt.equals(U_STEM_DECLENSIONS) ||
                qt.equals(UU_STEM_DECLENSIONS) ||
                qt.equals(R_STEM_DECLENSIONS);
    }

    public static boolean isVocabulary(LessonType qt) {
        return qt.equals(VOCABULARY) ||
                qt.equals(VOCABULARY_BASIC) ||
                qt.equals(VOCABULARY_TEXTS);
    }

}
