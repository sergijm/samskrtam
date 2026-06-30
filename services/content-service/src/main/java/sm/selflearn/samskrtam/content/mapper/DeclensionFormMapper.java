package sm.selflearn.samskrtam.content.mapper;

import org.mapstruct.Mapper;
import sm.selflearn.samskrtam.content.dto.DeclensionFormDto;
import sm.selflearn.samskrtam.content.model.DeclensionForm;

@Mapper(componentModel = "spring")
public interface DeclensionFormMapper {

    DeclensionFormDto toDeclensionFormDto(DeclensionForm form);
}