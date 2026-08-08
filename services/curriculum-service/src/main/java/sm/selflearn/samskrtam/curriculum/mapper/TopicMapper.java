package sm.selflearn.samskrtam.curriculum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sm.selflearn.samskrtam.curriculum.dto.CreateTopicRequest;
import sm.selflearn.samskrtam.curriculum.dto.TopicDto;
import sm.selflearn.samskrtam.curriculum.model.Topic;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface TopicMapper {

    @Mapping(target = "appearsInLevels", ignore = true)
    @Mapping(target = "isEvergreen", source = "evergreen")
    TopicDto toDto(Topic topic);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "targetItemCount", ignore = true)
    @Mapping(target = "domain", ignore = true)
    @Mapping(target = "evergreen", source = "isEvergreen")
    Topic toEntity(CreateTopicRequest request);

    default OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
