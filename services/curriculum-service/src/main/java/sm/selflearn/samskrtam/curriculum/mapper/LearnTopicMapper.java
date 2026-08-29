package sm.selflearn.samskrtam.curriculum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sm.selflearn.samskrtam.curriculum.dto.LearnTopicDto;
import sm.selflearn.samskrtam.curriculum.dto.LearnTopicStatus;
import sm.selflearn.samskrtam.curriculum.model.Topic;

import java.util.List;

/**
 * Maps a {@link Topic} into a learning-map card DTO. typeGroup and route are
 * derived from the stable topic code (see {@link LearnTopicDeriver});
 * status/progress are injected per user.
 */
@Mapper(componentModel = "spring")
public interface LearnTopicMapper {

    @Mapping(target = "typeGroup", expression = "java(sm.selflearn.samskrtam.curriculum.mapper.LearnTopicDeriver.classifyTypeGroup(topic.getCode()))")
    @Mapping(target = "route", expression = "java(sm.selflearn.samskrtam.curriculum.mapper.LearnTopicDeriver.resolveRoute(topic.getCode(), topic.getDomainType()))")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "progressPercent", source = "progressPercent")
    @Mapping(target = "prerequisites", source = "prerequisites")
    LearnTopicDto toDto(Topic topic, LearnTopicStatus status, Integer progressPercent, List<String> prerequisites);
}