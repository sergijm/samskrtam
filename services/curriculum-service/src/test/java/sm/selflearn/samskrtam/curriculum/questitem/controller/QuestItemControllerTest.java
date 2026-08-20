package sm.selflearn.samskrtam.curriculum.questitem.controller;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.questgen.QuizItemGenerationService;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.dto.QuestItemDto;
import sm.selflearn.samskrtam.curriculum.questitem.mapper.QuestItemMapper;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;
import sm.selflearn.samskrtam.quest.AnswerMode;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestItemControllerTest {

    private static final UUID TOPIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private QuestItemRepository questItemRepository;
    private TopicRepository topicRepository;
    private QuestItemMapper mapper;
    private QuizItemGenerationService generationService;
    private QuestItemController controller;

    @BeforeEach
    void setUp() {
        questItemRepository = org.mockito.Mockito.mock(QuestItemRepository.class);
        topicRepository = org.mockito.Mockito.mock(TopicRepository.class);
        mapper = org.mockito.Mockito.mock(QuestItemMapper.class);
        generationService = org.mockito.Mockito.mock(QuizItemGenerationService.class);
        controller = new QuestItemController(questItemRepository, topicRepository, mapper, generationService);
    }

    @Test
    void getQuestItems_knownTopic_returnsMappedItems() {
        QuestItem item = new QuestItem();
        when(topicRepository.existsById(TOPIC_ID)).thenReturn(true);
        when(questItemRepository.findRandomByTopicIdAndItemType(TOPIC_ID, "DECLENSION_FORM", 5))
                .thenReturn(List.of(item));
        QuestItemDto dto = new QuestItemDto(UUID.randomUUID(), "DECLENSION_FORM", AnswerMode.FREE_TEXT,
                "p", null, "a", null, List.of(), null, null, "NOMINATIVE|SINGULAR|FEMININE", null);
        when(mapper.toDto(item)).thenReturn(dto);

        List<QuestItemDto> result = controller.getQuestItems(TOPIC_ID, "DECLENSION_FORM", 5);

        assertThat(result).containsExactly(dto);
    }

    @Test
    void getQuestItems_unknownTopic_throwsEntityNotFound() {
        when(topicRepository.existsById(TOPIC_ID)).thenReturn(false);
        assertThatThrownBy(() -> controller.getQuestItems(TOPIC_ID, "DECLENSION_FORM", 5))
                .isInstanceOf(EntityNotFoundException.class);
        verify(questItemRepository, never()).findRandomByTopicIdAndItemType(any(), any(), anyInt());
    }

    @Test
    void regenerate_noArgs_returnsPerTopicStats() {
        Map<String, Map<String, Integer>> stats = Map.of(
                "a-stem", Map.of("generated", 77, "uniqueProgressTags", 24),
                "i-u-stems", Map.of("generated", 152, "uniqueProgressTags", 18));
        when(generationService.regenerate()).thenReturn(stats);

        var response = controller.regenerate();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody())
                .containsEntry("a-stem", Map.of("generated", 77, "uniqueProgressTags", 24))
                .containsEntry("i-u-stems", Map.of("generated", 152, "uniqueProgressTags", 18));
        verify(generationService).regenerate();
    }

    private static Topic topic(String code) {
        Topic t = new Topic();
        t.setId(UUID.randomUUID());
        t.setCode(code);
        return t;
    }
}