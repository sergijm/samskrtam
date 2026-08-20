package sm.selflearn.samskrtam.curriculum.lexicon.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconPosDto;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PartOfSpeech;

/**
 * Maps a {@link PartOfSpeech} into a home-page chip. id is the stable pos code;
 * labels are localised on the node. wordCount is the real lexeme count.
 */
@Mapper(componentModel = "spring")
public interface LexiconPosMapper {

    @Mapping(target = "id", source = "pos.code")
    @Mapping(target = "nameRu", source = "pos.nameRu")
    @Mapping(target = "nameEn", source = "pos.nameEn")
    @Mapping(target = "wordCount", source = "wordCount")
    LexiconPosDto toDto(PartOfSpeech pos, int wordCount);
}