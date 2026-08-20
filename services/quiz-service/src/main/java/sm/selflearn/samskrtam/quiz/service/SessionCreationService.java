package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.quiz.mapper.SessionQuestionMapper;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionQuestion;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;
import sm.selflearn.samskrtam.quiz.repository.SessionQuestionRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Отвечает за создание новых квиз-сессий (plain lesson-based).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionCreationService {

    private final QuizSessionRepository quizSessionRepository;
    private final SessionQuestionRepository sessionQuestionRepository;
    private final ContentClient contentClient;
    private final SessionFactory sessionFactory;
    private final SessionQuestionMapper sessionQuestionMapper;
    private final SessionPublisher sessionPublisher;
    private final QuizDataAssembler quizDataAssembler;

    @Transactional
    public Mono<sm.selflearn.samskrtam.quiz.dto.StartOrResumeResponse> createNewSession(
            UUID lessonId, UUID userId, String userLocale) {
        return contentClient.generateQuizData(lessonId, userLocale)
                .flatMap(generatedQuizData -> {
                    QuizSession newSession = sessionFactory.createSession(lessonId, userId, generatedQuizData);
                    return quizSessionRepository.save(newSession)
                            .flatMap(savedSession -> {
                                List<SessionQuestion> sessionQuestions = generatedQuizData.getGeneratedQuestions()
                                        .stream()
                                        .map(q -> sessionQuestionMapper.fromDto(q, savedSession.getId()))
                                        .collect(Collectors.toList());
                                return sessionQuestionRepository.saveAll(sessionQuestions)
                                        .then(sessionPublisher.publishStarted(savedSession))
                                        .then(quizDataAssembler.assembleResponse(
                                                savedSession,
                                                generatedQuizData.getGeneratedQuestions(),
                                                generatedQuizData.getVocabularyWords(),
                                                userLocale));
                            });
                });
    }
}