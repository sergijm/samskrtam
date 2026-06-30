package sm.selflearn.samskrtam.content.mapper;

import org.mapstruct.Mapper;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.content.model.VocabularyWord;

@Mapper(componentModel = "spring")
public interface VocabularyMapper {

    VocabularyWordDto toDto(VocabularyWord word);
}