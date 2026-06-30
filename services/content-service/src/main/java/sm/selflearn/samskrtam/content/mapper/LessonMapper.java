package sm.selflearn.samskrtam.content.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import sm.selflearn.samskrtam.content.dto.LessonItemResponse;
import sm.selflearn.samskrtam.content.model.Lesson;

@Mapper(componentModel = "spring")
public interface LessonMapper {

    @Mapping(target = "title", source = "lesson.titleEn")
    @Mapping(target = "description", source = "lesson.descriptionEn")
    @Mapping(target = "wordCount", source = "wordCount")
    LessonItemResponse toLessonItemResponse(Lesson lesson, int wordCount);
}
