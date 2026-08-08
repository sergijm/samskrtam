package sm.selflearn.samskrtam.curriculum.questitem.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.dto.QuestItemDto;

/**
 * Maps a persisted {@link QuestItem} to its read model. {@code distractors} and
 * {@code payload} are deserialized from the JSONB columns; {@code correctAnswer}
 * is suppressed for MATCHING items via {@link QuestItemJsonSupport#correctAnswer}.
 */
@Mapper(componentModel = "spring", uses = QuestItemJsonSupport.class)
public interface QuestItemMapper {

    @Mapping(target = "distractors", source = "distractors", qualifiedByName = "jsonList")
    @Mapping(target = "payload", source = "payload", qualifiedByName = "jsonObject")
    @Mapping(target = "correctAnswer", expression = "java(questItemJsonSupport.correctAnswer(questItem))")
    QuestItemDto toDto(QuestItem questItem);
}