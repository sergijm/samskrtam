package sm.selflearn.samskrtam.curriculum.lexicon.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconSourceDto;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Source;

/**
 * Maps a {@link Source} into a "vocabulary by text" card. wordCount comes from
 * the source unique-lemma cache; masteredCount is injected per user (random).
 */
@Mapper(componentModel = "spring")
public interface LexiconSourceMapper {

    @Mapping(target = "id", source = "source.id")
    @Mapping(target = "titleRu", source = "source.titleRu")
    @Mapping(target = "titleEn", source = "source.titleEn")
    @Mapping(target = "devanagari", ignore = true)
    @Mapping(target = "wordCount", source = "source.uniqueLemmaCountCache")
    @Mapping(target = "masteredCount", source = "masteredCount")
    LexiconSourceDto toDto(Source source, int masteredCount);
}