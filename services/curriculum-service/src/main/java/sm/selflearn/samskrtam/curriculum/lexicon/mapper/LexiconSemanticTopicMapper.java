package sm.selflearn.samskrtam.curriculum.lexicon.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconSemanticTopicDto;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SemanticTopic;

/**
 * Maps a {@link SemanticTopic} into a home-page topic card. id is the stable
 * code; localised labels are copied verbatim so the UI renders them directly.
 * wordCount / masteredCount are injected per user (currently random).
 */
@Mapper(componentModel = "spring")
public interface LexiconSemanticTopicMapper {

    @Mapping(target = "id", source = "topic.code")
    @Mapping(target = "nameRu", source = "topic.nameRu")
    @Mapping(target = "nameEn", source = "topic.nameEn")
    @Mapping(target = "wordCount", source = "wordCount")
    @Mapping(target = "masteredCount", source = "masteredCount")
    LexiconSemanticTopicDto toDto(SemanticTopic topic, int wordCount, int masteredCount);
}