package sm.selflearn.samskrtam.content.dto;

public enum LessonType {
    DECLENSIONS,
    CONJUGATIONS,
    VOCABULARY,
    VOCABULARY_BASIC,
    VOCABULARY_TEXTS;

    public static boolean isDeclensions(LessonType qt) {
        return qt.equals(DECLENSIONS);
    }

    public static boolean isVocabulary(LessonType qt) {
        return qt.equals(VOCABULARY) ||
                qt.equals(VOCABULARY_BASIC) ||
                qt.equals(VOCABULARY_TEXTS);
    }
}

