package sm.selflearn.samskrtam.curriculum.lexicon.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexiconFrequencyDto;
import sm.selflearn.samskrtam.curriculum.lexicon.model.FrequencyBand;

/**
 * Maps a {@link FrequencyBand} into a home-page frequency-band card. from/to
 * come from the rank range; wordCount is the real slice size; masteredCount is
 * the per-user mastered subset.
 */
@Mapper(componentModel = "spring")
public interface LexiconFrequencyMapper {

    @Mapping(target = "id", source = "band.code")
    @Mapping(target = "from", source = "band.minRank")
    @Mapping(target = "to", source = "band.maxRank")
    @Mapping(target = "wordCount", source = "wordCount")
    @Mapping(target = "masteredCount", source = "masteredCount")
    LexiconFrequencyDto toDto(FrequencyBand band, int wordCount, int masteredCount);
}