package sm.selflearn.samskrtam.curriculum.mapper;

import org.mapstruct.Mapper;
import sm.selflearn.samskrtam.curriculum.dto.CaseEndingDto;
import sm.selflearn.samskrtam.curriculum.lexicon.lingua.CaseEnding;

@Mapper(componentModel = "spring")
public interface CaseEndingMapper {
    CaseEndingDto toDto(CaseEnding entity);
}
