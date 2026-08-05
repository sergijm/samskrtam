package sm.selflearn.samskrtam.curriculum.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.dto.ComplexQuizDto;
import sm.selflearn.samskrtam.curriculum.dto.ComplexQuizSummaryDto;
import sm.selflearn.samskrtam.curriculum.dto.TopicDto;
import sm.selflearn.samskrtam.curriculum.dto.UpsertComplexQuizRequest;
import sm.selflearn.samskrtam.curriculum.exception.InvalidComplexQuizCompositionException;
import sm.selflearn.samskrtam.curriculum.mapper.ComplexQuizMapper;
import sm.selflearn.samskrtam.curriculum.mapper.TopicMapper;
import sm.selflearn.samskrtam.curriculum.model.ComplexQuiz;
import sm.selflearn.samskrtam.curriculum.model.ComplexQuizTopic;
import sm.selflearn.samskrtam.curriculum.model.ComplexQuizTopicId;
import sm.selflearn.samskrtam.curriculum.model.ComplexQuizType;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.repository.ComplexQuizRepository;
import sm.selflearn.samskrtam.curriculum.repository.ComplexQuizTopicRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplexQuizService {

    private final ComplexQuizRepository complexQuizRepository;
    private final ComplexQuizTopicRepository complexQuizTopicRepository;
    private final TopicRepository topicRepository;
    private final ComplexQuizMapper complexQuizMapper;
    private final TopicMapper topicMapper;

    public List<ComplexQuizSummaryDto> listComplexQuizzes(LearningLevel level, ComplexQuizType type) {
        List<ComplexQuiz> quizzes;
        if (level != null && type != null) {
            quizzes = complexQuizRepository.findByLearningLevelAndType(level, type);
        } else if (level != null) {
            quizzes = complexQuizRepository.findByLearningLevel(level);
        } else if (type != null) {
            quizzes = complexQuizRepository.findByType(type);
        } else {
            quizzes = complexQuizRepository.findAll();
        }
        return quizzes.stream()
                .map(quiz -> complexQuizMapper.toSummary(
                        quiz, (int) complexQuizTopicRepository.countByIdComplexQuizId(quiz.getId())))
                .toList();
    }

    public ComplexQuizDto getComplexQuiz(UUID id) {
        ComplexQuiz quiz = complexQuizRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ComplexQuiz not found: " + id));
        return toFullDto(quiz);
    }

    @Transactional
    public ComplexQuizDto createComplexQuiz(UpsertComplexQuizRequest request) {
        validateComposition(request.type(), request.topicIds());
        ComplexQuiz quiz = new ComplexQuiz();
        quiz.setType(request.type());
        quiz.setLearningLevel(request.learningLevel());
        quiz.setTitleRu(request.titleRu());
        quiz.setTitleEn(request.titleEn());
        quiz.setQuestionCountHint(request.questionCountHint());
        ComplexQuiz saved = complexQuizRepository.save(quiz);
        saveComposition(saved.getId(), request.topicIds());
        return toFullDto(saved);
    }

    @Transactional
    public ComplexQuizDto updateComplexQuiz(UUID id, UpsertComplexQuizRequest request) {
        ComplexQuiz quiz = complexQuizRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ComplexQuiz not found: " + id));
        validateComposition(request.type(), request.topicIds());
        quiz.setType(request.type());
        quiz.setLearningLevel(request.learningLevel());
        quiz.setTitleRu(request.titleRu());
        quiz.setTitleEn(request.titleEn());
        quiz.setQuestionCountHint(request.questionCountHint());
        complexQuizTopicRepository.deleteAll(complexQuizTopicRepository.findByIdComplexQuizId(id));
        saveComposition(id, request.topicIds());
        return toFullDto(quiz);
    }

    @Transactional
    public void deleteComplexQuiz(UUID id) {
        complexQuizRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ComplexQuiz not found: " + id));
        complexQuizRepository.deleteById(id);
    }

    public void validateComposition(ComplexQuizType type, List<UUID> topicIds) {
        if (topicIds.stream().distinct().count() != topicIds.size()) {
            throw new InvalidComplexQuizCompositionException("ComplexQuiz topicIds must be unique");
        }
        for (UUID topicId : topicIds) {
            if (!topicRepository.existsById(topicId)) {
                throw new EntityNotFoundException("Topic not found: " + topicId);
            }
        }
        int size = topicIds.size();
        if (type == ComplexQuizType.MIXED_PRACTICE && (size < 2 || size > 4)) {
            throw new InvalidComplexQuizCompositionException(
                    "MIXED_PRACTICE requires 2-4 topics, got " + size);
        }
        if (type == ComplexQuizType.LEVEL_ASSESSMENT && (size < 5 || size > 7)) {
            throw new InvalidComplexQuizCompositionException(
                    "LEVEL_ASSESSMENT requires 5-7 topics, got " + size);
        }
    }

    public List<LearningLevel> resolveAppearsInLevels(UUID topicId, LearningLevel ownLevel) {
        List<UUID> quizIds = complexQuizTopicRepository.findByIdTopicId(topicId).stream()
                .map(row -> row.getId().getComplexQuizId())
                .toList();
        if (quizIds.isEmpty()) {
            return List.of(ownLevel);
        }
        LinkedHashSet<LearningLevel> levels = new LinkedHashSet<>();
        levels.add(ownLevel);
        complexQuizRepository.findAllById(quizIds).stream()
                .map(ComplexQuiz::getLearningLevel)
                .sorted()
                .forEach(levels::add);
        return List.copyOf(levels);
    }

    private void saveComposition(UUID quizId, List<UUID> topicIds) {
        for (UUID topicId : topicIds) {
            ComplexQuizTopicId key = new ComplexQuizTopicId();
            key.setComplexQuizId(quizId);
            key.setTopicId(topicId);
            ComplexQuizTopic row = new ComplexQuizTopic();
            row.setId(key);
            complexQuizTopicRepository.save(row);
        }
    }

    private ComplexQuizDto toFullDto(ComplexQuiz quiz) {
        List<UUID> topicIds = complexQuizTopicRepository.findByIdComplexQuizId(quiz.getId()).stream()
                .map(row -> row.getId().getTopicId())
                .toList();
        Map<UUID, Topic> topicById = topicRepository.findAllById(topicIds).stream()
                .collect(Collectors.toMap(Topic::getId, topic -> topic));
        List<TopicDto> topics = topicIds.stream()
                .map(topicById::get)
                .map(topicMapper::toDto)
                .toList();
        return new ComplexQuizDto(
                quiz.getId(),
                quiz.getType(),
                quiz.getLearningLevel(),
                quiz.getTitleRu(),
                quiz.getTitleEn(),
                quiz.getQuestionCountHint(),
                topics,
                complexQuizMapper.toOffsetDateTime(quiz.getCreatedAt()),
                complexQuizMapper.toOffsetDateTime(quiz.getUpdatedAt()));
    }
}
