package sm.selflearn.samskrtam.quiz.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sm.selflearn.samskrtam.quiz.dto.QuizProgressByCaseDto;

/**
 * Маппер для преобразования данных агрегации прогресса по падежам в DTO.
 * Согласно conventions.md §16.
 */
@Mapper(componentModel = "spring")
public interface QuizProgressMapper {

    @Mapping(target = "caseType", source = "caseType")
    @Mapping(target = "aggregatedProgress", source = "aggregatedProgress")
    @Mapping(target = "totalCombinations", source = "totalCombinations")
    @Mapping(target = "learnedCombinations", source = "learnedCombinations")
    QuizProgressByCaseDto toDto(String caseType, int aggregatedProgress, int totalCombinations, int learnedCombinations);
}
