package sm.selflearn.samskrtam.curriculum.mapper;

import org.mapstruct.Mapper;
import sm.selflearn.samskrtam.curriculum.dto.VerbalEndingDto;
import sm.selflearn.samskrtam.curriculum.lexicon.lingua.VerbalEnding;

@Mapper(componentModel = "spring")
public interface VerbalEndingMapper {
    VerbalEndingDto toDto(VerbalEnding entity);
}