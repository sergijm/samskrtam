package sm.selflearn.samskrtam.quiz.localization;

import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.content.model.Gender;
import sm.selflearn.samskrtam.content.model.NumberType;

/**
 * Единая точка локализации CaseType / NumberType / Gender.
 * Разрешает enum-ы или строковые имена в русские и английские названия.
 * <p>
 * Переиспользует существующие enum-словари ({@link CaseType#getRuName()} и т.д.),
 * не плодит копии переводов.
 */
public final class CaseNumberGenderLocalizer {

    private CaseNumberGenderLocalizer() {
        // utility class
    }

    // ── CaseType ──

    public static String caseTypeRu(CaseType ct) {
        return ct != null ? ct.getRuName() : null;
    }

    public static String caseTypeEn(CaseType ct) {
        return ct != null ? ct.getEnName() : null;
    }

    public static String caseTypeRu(String caseTypeName) {
        try {
            return CaseType.valueOf(caseTypeName).getRuName();
        } catch (IllegalArgumentException | NullPointerException e) {
            return caseTypeName;
        }
    }

    public static String caseTypeEn(String caseTypeName) {
        try {
            return CaseType.valueOf(caseTypeName).getEnName();
        } catch (IllegalArgumentException | NullPointerException e) {
            return caseTypeName;
        }
    }

    // ── NumberType ──

    public static String numberTypeRu(NumberType nt) {
        return nt != null ? nt.getRuName() : null;
    }

    public static String numberTypeEn(NumberType nt) {
        return nt != null ? nt.getEnName() : null;
    }

    public static String numberTypeRu(String numberTypeName) {
        try {
            return NumberType.valueOf(numberTypeName).getRuName();
        } catch (IllegalArgumentException | NullPointerException e) {
            return numberTypeName;
        }
    }

    public static String numberTypeEn(String numberTypeName) {
        try {
            return NumberType.valueOf(numberTypeName).getEnName();
        } catch (IllegalArgumentException | NullPointerException e) {
            return numberTypeName;
        }
    }

    // ── Gender ──

    public static String genderRu(Gender g) {
        return g != null ? g.getRuName() : null;
    }

    public static String genderEn(Gender g) {
        return g != null ? g.getEnName() : null;
    }

    public static String genderRu(String genderName) {
        try {
            return Gender.valueOf(genderName).getRuName();
        } catch (IllegalArgumentException | NullPointerException e) {
            return genderName;
        }
    }

    public static String genderEn(String genderName) {
        try {
            return Gender.valueOf(genderName).getEnName();
        } catch (IllegalArgumentException | NullPointerException e) {
            return genderName;
        }
    }
}
