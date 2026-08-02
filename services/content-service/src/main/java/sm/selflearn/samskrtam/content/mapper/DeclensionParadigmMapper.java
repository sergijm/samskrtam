package sm.selflearn.samskrtam.content.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmDto;
import sm.selflearn.samskrtam.content.model.DeclensionForm;
import sm.selflearn.samskrtam.content.model.DeclensionStem;

import java.util.List;

@Mapper(componentModel = "spring", uses = DeclensionFormMapper.class)
public interface DeclensionParadigmMapper {

    @Mapping(target = "stemId", source = "stem.id")
    @Mapping(target = "stemIast", source = "stem.stemIast")
    @Mapping(target = "stemDevanagari", source = "stem.stemDevanagari")
    @Mapping(target = "translationRu", source = "stem.translationRu")
    @Mapping(target = "translationEn", source = "stem.translationEn")
    @Mapping(target = "gender", source = "stem.gender")
    @Mapping(target = "vowelType", source = "stem.vowelType")
    DeclensionParadigmDto toDeclensionParadigmDto(DeclensionStem stem, List<DeclensionForm> forms);
}
