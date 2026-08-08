package sm.selflearn.samskrtam.curriculum.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * UI filter/icon group of a curriculum topic on the learning map page.
 * Mirrors the frontend `TypeGroup` union in src/config/learnGraph.ts.
 */
public enum TopicTypeGroup {
    VOCABULARY,
    DECLENSION,
    SANDHI,
    CONJUGATION,
    SYNTAX,
    OTHER;

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}