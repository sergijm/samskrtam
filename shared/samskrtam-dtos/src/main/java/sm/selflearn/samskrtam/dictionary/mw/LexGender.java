package sm.selflearn.samskrtam.dictionary.mw;

import sm.selflearn.samskrtam.morphology.Gender;

/**
 * Одна грамматическая (родовая) характеристика слова, извлечённая либо
 * из тега {@code <lex>...</lex>} в теле статьи, либо из атрибута
 * {@code <info lex="m:f#ikA:n"/>}.
 *
 * Примеры значений:
 *   gender="m"                       -> mascul.
 *   gender="f", stem="ikA"           -> f(ikA) - женская форма на -ikA
 *   gender="ind"                     -> indeclinable (неизменяемое)
 *   gender="inh"                     -> унаследовано от предыдущей подстатьи
 */
public class LexGender {

    /** m, f, n, mfn, ind, inh, pron, card, ... */
    private String gender;

    /** Типизированное представление грамматического рода (если применимо). */
    private Gender genderEnum;

    /** Дополнительная основа для рода, если указана (напр. "ikA" в m:f#ikA:n) */
    private String stem;

    /**
     * Значение атрибута type у тега <lex type="X">, если применимо:
     * hw, hwalt, hwifc, hwinfo, nhw, part, phw. null, если это основной <lex>.
     */
    private String type;

    public LexGender() {
    }

    public LexGender(String gender, String stem, String type) {
        this.gender = gender;
        this.stem = stem;
        this.type = type;
        this.genderEnum = Gender.fromCode(gender);
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
        this.genderEnum = Gender.fromCode(gender);
    }

    public Gender getGenderEnum() {
        return genderEnum;
    }

    public void setGenderEnum(Gender genderEnum) {
        this.genderEnum = genderEnum;
    }

    public String getStem() {
        return stem;
    }

    public void setStem(String stem) {
        this.stem = stem;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(gender == null ? "?" : gender);
        if (stem != null) {
            sb.append('#').append(stem);
        }
        if (type != null) {
            sb.append('[').append(type).append(']');
        }
        return sb.toString();
    }
}
