package sm.selflearn.samskrtam.dictionaryentries;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DictionaryEntriesResponse {
    private String dictionary;
    private List<Object> entries;
}
