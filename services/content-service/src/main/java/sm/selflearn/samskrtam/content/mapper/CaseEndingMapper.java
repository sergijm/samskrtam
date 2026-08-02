package sm.selflearn.samskrtam.content.mapper;

import org.mapstruct.Mapper;
import sm.selflearn.samskrtam.content.dto.CaseEndingDto;
import sm.selflearn.samskrtam.content.model.CaseEnding;

@Mapper(componentModel = "spring")
public interface CaseEndingMapper {

    CaseEndingDto toCaseEndingDto(CaseEnding caseEnding);
}
