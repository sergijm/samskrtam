package sm.selflearn.samskrtam.content.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sm.selflearn.samskrtam.content.dto.DeclensionStemDto;
import sm.selflearn.samskrtam.content.model.DeclensionStem;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface DeclensionStemMapper {

    @Mapping(target = "lessonId", source = "lessonId")
    @Mapping(target = "slug", source = "stem.stemIast")
    DeclensionStemDto toDeclensionStemDto(DeclensionStem stem, UUID lessonId);
}
