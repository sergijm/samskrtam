package sm.selflearn.samskrtam.curriculum.lexicon.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconSemanticClassDto;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SemanticClass;

/**
 * Maps a {@link SemanticClass} into a home-page topic card. id is the stable
 * code; localised labels are copied verbatim so the UI renders them directly.
 * wordCount / masteredCount are the real counts computed by the dashboard
 * service.
 */
@Mapper(componentModel = "spring")
public interface LexiconSemanticClassMapper {

    @Mapping(target = "id", source = "topic.code")
    @Mapping(target = "nameRu", source = "topic.nameRu")
    @Mapping(target = "nameEn", source = "topic.nameEn")
    @Mapping(target = "wordCount", source = "wordCount")
    @Mapping(target = "masteredCount", source = "masteredCount")
    LexiconSemanticClassDto toDto(SemanticClass topic, int wordCount, int masteredCount);
}