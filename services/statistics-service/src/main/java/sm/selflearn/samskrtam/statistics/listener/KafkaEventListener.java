package sm.selflearn.samskrtam.statistics.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.events.AnswerSubmitted; // Import AnswerSubmitted event
import sm.selflearn.samskrtam.events.SessionCompleted;
import sm.selflearn.samskrtam.statistics.service.StatisticService;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventListener {

    private final StatisticService statisticService;

//    @KafkaListener(topics = "quiz.session.completed", groupId = "statistics-service")
//    public void listenSessionCompleted(SessionCompleted event) {
//        log.info("Received SessionCompleted event: {}", event);
//        statisticService.processSessionCompleted(event);
//    }

    @KafkaListener(topics = "quiz.answer.submitted", groupId = "statistics-service")
    public void listenAnswerSubmitted(AnswerSubmitted event) {
        log.info("Received AnswerSubmitted event: {}", event);
        statisticService.processAnswerSubmitted(event);
    }
}
