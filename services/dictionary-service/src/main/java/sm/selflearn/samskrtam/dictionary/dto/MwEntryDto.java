package sm.selflearn.samskrtam.dictionary.dto;

import lombok.Builder;
import lombok.Data;
import sm.selflearn.samskrtam.monierwilliams.dto.MwDictionaryEntryDto;

import java.util.List;

@Data
@Builder
public class MwEntryDto {
    private List<MwDictionaryEntryDto> entries;
}
