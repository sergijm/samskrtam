package sm.selflearn.samskrtam.content.service;

import sm.selflearn.samskrtam.content.model.VowelType;

import java.util.Collections;
import java.util.List;

/**
 * Maps lesson slugs to their corresponding VowelType(s).
 *
 * <p>Handles both single-type slugs (e.g. {@code declensions-a-masc} → {@code [A_STEM]})
 * and compound slugs (e.g. {@code declensions-i-u} → {@code [I_STEM, U_STEM]}).
 *
 * <p>The VowelType enum at the data level is NOT changed — individual stems still store
 * their exact vowel type (I_STEM, U_STEM, etc.). This mapper operates at the lesson/presentation
 * level, aggregating multiple vowel types into a single compound lesson.
 *
 * <p>Compound slug rules:
 * <ul>
 *   <li>{@code declensions-i-u} → I_STEM + U_STEM (identical declension pattern)</li>
 *   <li>{@code declensions-ii-uu} → II_STEM + UU_STEM (identical declension pattern)</li>
 * </ul>
 */
public final class SlugToVowelTypeMapper {

    private SlugToVowelTypeMapper() {
        // utility class
    }

    /**
     * Maps a lesson slug to a list of VowelTypes.
     *
     * @param slug lesson slug (e.g. "declensions-i-u", "declensions-a-masc")
     * @return list of VowelType(s); empty list for unknown/all slugs
     */
    public static List<VowelType> mapSlugToVowelTypes(String slug) {
        if (slug == null) {
            return Collections.emptyList();
        }

        // --- Compound slugs: new unified lessons ---
        if (slug.equals("declensions-i-u")) {
            return List.of(VowelType.I_STEM, VowelType.U_STEM);
        }
        if (slug.equals("declensions-ii-uu")) {
            return List.of(VowelType.II_STEM, VowelType.UU_STEM);
        }

        // --- Legacy single-type slugs (kept for backward compatibility) ---
        if (slug.startsWith("declensions-a-") || slug.equals("declensions-a-masc") || slug.equals("declensions-a-neut")) {
            return List.of(VowelType.A_STEM);
        }
        if (slug.startsWith("declensions-aa-") || slug.equals("declensions-a-fem")) {
            return List.of(VowelType.AA_STEM);
        }
        if (slug.startsWith("declensions-ii-") || slug.equals("declensions-ii") || slug.equals("declensions-ii-fem")) {
            return List.of(VowelType.II_STEM);
        }
        if (slug.startsWith("declensions-i-") || slug.equals("declensions-i")) {
            return List.of(VowelType.I_STEM);
        }
        if (slug.startsWith("declensions-uu-") || slug.equals("declensions-uu") || slug.equals("declensions-uu-fem")) {
            return List.of(VowelType.UU_STEM);
        }
        if (slug.startsWith("declensions-u-") || slug.equals("declensions-u")) {
            return List.of(VowelType.U_STEM);
        }
        if (slug.startsWith("declensions-r-") || slug.equals("declensions-r")) {
            return List.of(VowelType.R_STEM);
        }

        // "declensions-all" or unknown slug → no specific vowel type filter
        return Collections.emptyList();
    }

    /**
     * Checks whether any of the given vowel types belong to the "unspecified gender" group
     * (I_STEM, II_STEM, U_STEM, UU_STEM, R_STEM).
     *
     * <p>For these stem types, case endings are gender-invariant — the database stores
     * them with {@code gender = UNSPECIFIED}.
     */
    public static boolean isUnspecifiedGenderType(List<VowelType> vowelTypes) {
        if (vowelTypes == null || vowelTypes.isEmpty()) {
            return false;
        }
        return vowelTypes.stream().anyMatch(vt ->
                vt == VowelType.I_STEM
                        || vt == VowelType.II_STEM
                        || vt == VowelType.U_STEM
                        || vt == VowelType.UU_STEM
                        || vt == VowelType.R_STEM);
    }

    /**
     * @deprecated Use {@link #mapSlugToVowelTypes(String)} for new code.
     * Returns the first VowelType from the list, or {@code null} if the list is empty.
     */
    @Deprecated
    public static VowelType mapSlugToVowelType(String slug) {
        List<VowelType> types = mapSlugToVowelTypes(slug);
        return types.isEmpty() ? null : types.get(0);
    }
}
