package sm.selflearn.samskrtam.curriculum.questitem.controller;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.questgen.DeclensionQuestItemBatchGenerator;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.dto.QuestItemDto;
import sm.selflearn.samskrtam.curriculum.questitem.mapper.QuestItemMapper;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestItemControllerTest {

    private static final UUID TOPIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private QuestItemRepository questItemRepository;
    private TopicRepository topicRepository;
    private QuestItemMapper mapper;
    private DeclensionQuestItemBatchGenerator generator;
    private QuestItemController controller;

    @BeforeEach
    void setUp() {
        questItemRepository = mock(QuestItemRepository.class);
        topicRepository = mock(TopicRepository.class);
        mapper = mock(QuestItemMapper.class);
        generator = mock(DeclensionQuestItemBatchGenerator.class);
        controller = new QuestItemController(questItemRepository, topicRepository, mapper, generator);
    }

    @Test
    void getQuestItems_knownTopic_returnsMappedItems() {
        QuestItem item = new QuestItem();
        when(topicRepository.existsById(TOPIC_ID)).thenReturn(true);
        when(questItemRepository.findRandomByTopicIdAndItemType(TOPIC_ID, "DECLENSION_FORM", 5))
                .thenReturn(List.of(item));
        QuestItemDto dto = new QuestItemDto(UUID.randomUUID(), "DECLENSION_FORM", "FREE_TEXT",
                "p", "a", List.of(), null);
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
    void regenerate_allTopics_allTypes() {
        Topic t1 = topic("a-stem-masc");
        when(topicRepository.findAll()).thenReturn(List.of(t1));
        when(generator.generateForTopic(eq(t1.getId()), anyInt())).thenReturn(77);

        var response = controller.regenerate(new RegenerateRequest(0));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).containsEntry("generated", 77);
        verify(questItemRepository).deleteByTopicId(t1.getId());
        verify(generator).generateForTopic(eq(t1.getId()), eq(Integer.MAX_VALUE));
    }

    @Test
    void regenerate_withLexemeLimit_passesLimit() {
        Topic t1 = topic("a-stem-masc");
        when(topicRepository.findAll()).thenReturn(List.of(t1));
        when(generator.generateForTopic(eq(t1.getId()), eq(5))).thenReturn(20);

        controller.regenerate(new RegenerateRequest(5));

        verify(generator).generateForTopic(eq(t1.getId()), eq(5));
    }

    private static Topic topic(String code) {
        Topic t = new Topic();
        t.setId(UUID.randomUUID());
        t.setCode(code);
        return t;
    }
}