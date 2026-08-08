package sm.selflearn.samskrtam.curriculum.questitem.controller;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
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

    // ------------------------------------------------------------------
    // GET /api/v2/curriculum/quest-items
    // ------------------------------------------------------------------

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
    void getQuestItems_limitAboveMax_isCappedTo100() {
        when(topicRepository.existsById(TOPIC_ID)).thenReturn(true);
        when(questItemRepository.findRandomByTopicIdAndItemType(TOPIC_ID, "DECLENSION_FORM", 100))
                .thenReturn(List.of());

        controller.getQuestItems(TOPIC_ID, "DECLENSION_FORM", 500);

        verify(questItemRepository).findRandomByTopicIdAndItemType(TOPIC_ID, "DECLENSION_FORM", 100);
    }

    @Test
    void getQuestItems_unknownTopic_throwsEntityNotFound() {
        when(topicRepository.existsById(TOPIC_ID)).thenReturn(false);

        assertThatThrownBy(() -> controller.getQuestItems(TOPIC_ID, "DECLENSION_FORM", 5))
                .isInstanceOf(EntityNotFoundException.class);
        verify(questItemRepository, never()).findRandomByTopicIdAndItemType(any(), any(), anyInt());
    }

    // ------------------------------------------------------------------
    // POST /api/v2/curriculum/quest-items/regenerate
    // ------------------------------------------------------------------

    @Test
    void regenerate_formType_deletesAndRegeneratesForms() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(java.util.Optional.of(topic()));
        when(generator.generateFormsForTopic(TOPIC_ID)).thenReturn(24);

        var response = controller.regenerate(TOPIC_ID, "DECLENSION_FORM");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).containsEntry("generated", 24);
        verify(questItemRepository).deleteByTopicIdAndItemType(TOPIC_ID, "DECLENSION_FORM");
        verify(generator).generateFormsForTopic(TOPIC_ID);
    }

    @Test
    void regenerate_formChoiceType_usesSameFormsPass() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(java.util.Optional.of(topic()));
        when(generator.generateFormsForTopic(TOPIC_ID)).thenReturn(24);

        controller.regenerate(TOPIC_ID, "DECLENSION_FORM_CHOICE");

        verify(generator).generateFormsForTopic(TOPIC_ID);
    }

    @Test
    void regenerate_caseRecognition_callsCaseGroup() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(java.util.Optional.of(topic()));
        when(generator.generateCaseRecognitionForTopic(TOPIC_ID)).thenReturn(24);

        var response = controller.regenerate(TOPIC_ID, "CASE_RECOGNITION");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(generator).generateCaseRecognitionForTopic(TOPIC_ID);
    }

    @Test
    void regenerate_match_callsMatchGroup() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(java.util.Optional.of(topic()));
        when(generator.generateMatchForTopic(TOPIC_ID)).thenReturn(5);

        var response = controller.regenerate(TOPIC_ID, "DECLENSION_MATCH");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(generator).generateMatchForTopic(TOPIC_ID);
    }

    @Test
    void regenerate_unknownItemType_throwsIllegalArgument() {
        assertThatThrownBy(() -> controller.regenerate(TOPIC_ID, "VOCABULARY_WORD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported quest itemType");
        verify(questItemRepository, never()).deleteByTopicIdAndItemType(any(), any());
    }

    @Test
    void regenerate_unknownTopic_throwsEntityNotFound() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> controller.regenerate(TOPIC_ID, "DECLENSION_FORM"))
                .isInstanceOf(EntityNotFoundException.class);
        verify(questItemRepository, never()).deleteByTopicIdAndItemType(any(), any());
    }

    @Test
    void regenerate_requiresAdminRole() throws Exception {
        var preAuthorize = QuestItemController.class
                .getMethod("regenerate", UUID.class, String.class)
                .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('ADMIN')");
    }

    private static sm.selflearn.samskrtam.curriculum.model.Topic topic() {
        sm.selflearn.samskrtam.curriculum.model.Topic t = new sm.selflearn.samskrtam.curriculum.model.Topic();
        t.setId(TOPIC_ID);
        t.setCode("a-stem-masc");
        return t;
    }
}
