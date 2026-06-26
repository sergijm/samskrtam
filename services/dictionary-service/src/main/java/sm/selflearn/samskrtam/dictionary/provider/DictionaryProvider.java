package sm.selflearn.samskrtam.dictionary.provider;

import sm.selflearn.samskrtam.dictionary.model.DictionaryEntry;
import sm.selflearn.samskrtam.dictionary.model.DictionarySource;
import sm.selflearn.samskrtam.dictionary.model.WordSearchResult;
import java.util.List;

public interface DictionaryProvider {
    DictionarySource getSource();
    List<WordSearchResult> search(String slp1Query);
    List<DictionaryEntry> getEntries(String slp1Key);
}