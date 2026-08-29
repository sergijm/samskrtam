package sm.selflearn.samskrtam.dictionary.mw;

/**
 * Родственное (когнатное) слово из другого языка, напр.
 * {@code <lang>Gk.</lang> <gk>ἀ</gk>} или {@code <lang>Lat.</lang> <etym>in</etym>}.
 */
public class CognateWord {

    /** Название языка, как оно напечатано (напр. "Gk.", "Lat.", "Goth.", "Germ.", "Eng.") */
    private String language;

    /** Само слово (может быть в греческом/арабском письме или в латинице) */
    private String word;

    /** Скрипт, если указан явно (напр. "Arabic"), иначе null */
    private String script;

    public CognateWord() {
    }

    public CognateWord(String language, String word, String script) {
        this.language = language;
        this.word = word;
        this.script = script;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    @Override
    public String toString() {
        return (language == null ? "" : language + " ") + word;
    }
}
