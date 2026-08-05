package sm.selflearn.samskrtam.curriculum.mapper;

import org.mapstruct.Mapper;
import sm.selflearn.samskrtam.curriculum.dto.ComplexQuizSummaryDto;
import sm.selflearn.samskrtam.curriculum.model.ComplexQuiz;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface ComplexQuizMapper {

    ComplexQuizSummaryDto toSummary(ComplexQuiz quiz, int topicCount);

    default OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
