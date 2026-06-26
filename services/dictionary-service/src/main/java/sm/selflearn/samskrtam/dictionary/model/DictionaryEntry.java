package sm.selflearn.samskrtam.dictionary.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class DictionaryEntry {
    private String key;          // slp1 ключ
    private String keyIast;      // IAST
    private String displayTitle;
    private String mainTranslation;
    private String rawBody;      // исходный XML/текст
    private DictionarySource source;
    private Object sourceSpecific; // специфичные данные провайдера (MwDictionaryEntryDto и т.д.)
}