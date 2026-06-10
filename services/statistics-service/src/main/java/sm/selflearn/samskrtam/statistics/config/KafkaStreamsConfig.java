package sm.selflearn.samskrtam.statistics.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JsonSerde;
import sm.selflearn.samskrtam.content.dto.QuizType;
import sm.selflearn.samskrtam.quiz.event.QuizAnsweredEvent;
import sm.selflearn.samskrtam.quiz.event.QuizSessionStatusChangedEvent;
import sm.selflearn.samskrtam.quiz.event.StatisticEvent;
import sm.selflearn.samskrtam.statistics.model.UserQuizSessionStatistic;

import java.time.Instant;
import java.util.Arrays; // Added import for Arrays
import java.util.UUID;

@Configuration
@EnableKafkaStreams
@RequiredArgsConstructor
@Slf4j
public class KafkaStreamsConfig {

    private final ObjectMapper objectMapper;

    // Serde for StatisticEvent (interface with JsonTypeInfo)
    @Bean
    public Serde<StatisticEvent> statisticEventSerde() {
        JsonSerde<StatisticEvent> serde = new JsonSerde<>(StatisticEvent.class, objectMapper);
        // Ensure that the deserializer is configured to use type headers if they are added by the producer
        // This is handled by SamskrtamJsonDeserializer if it's used, but JsonSerde might need explicit configuration
        // For now, assuming default Spring Kafka JsonSerde works with JsonTypeInfo
        return serde;
    }

    // Serde for UserQuizSessionStatistic
    @Bean
    public Serde<UserQuizSessionStatistic> userQuizSessionStatisticSerde() {
        return new JsonSerde<>(UserQuizSessionStatistic.class, objectMapper);
    }

    @Bean
    public KTable<String, UserQuizSessionStatistic> processQuizStatistics(
            StreamsBuilder builder,
            Serde<StatisticEvent> statisticEventSerde,
            Serde<UserQuizSessionStatistic> userQuizSessionStatisticSerde) {

        // Consume both event types as a single StatisticEvent stream
        KStream<String, StatisticEvent> statisticEvents = builder.stream(
                Arrays.asList("quiz-answered-events", "quiz-session-status-changed-events"),
                Consumed.with(Serdes.String(), statisticEventSerde)
        );

        // Group by userId-quizId and aggregate into a KTable
        KTable<String, UserQuizSessionStatistic> userQuizStatisticsTable = statisticEvents
                .groupBy((key, event) -> event.userId().toString() + "-" + event.quizId().toString(),
                        Grouped.with(Serdes.String(), statisticEventSerde))
                .aggregate(
                        UserQuizSessionStatistic::new, // Initializer
                        (key, event, aggregate) -> { // Aggregator
                            // Initialize aggregate if it's the first event for this key
                            if (aggregate.getUserId() == null) {
                                aggregate.setId(UUID.randomUUID()); // Generate ID for new statistic entry
                                aggregate.setUserId(event.userId());
                                aggregate.setQuizId(event.quizId());
                                aggregate.setQuizType(event.quizType()); // Set quizType from the event
                                aggregate.setTotalSessions(0);
                                aggregate.setTotalQuestionsAnswered(0);
                                aggregate.setTotalCorrectAnswers(0);
                                aggregate.setTotalScore(0);
                                aggregate.setAverageScore(0.0);
                                aggregate.setLastCompletedAt(null);
                            }

                            // Update based on event type
                            if (event instanceof QuizAnsweredEvent answeredEvent) {
                                aggregate.setTotalQuestionsAnswered(aggregate.getTotalQuestionsAnswered() + 1);
                                if (answeredEvent.isCorrect()) {
                                    aggregate.setTotalCorrectAnswers(aggregate.getTotalCorrectAnswers() + 1);
                                    aggregate.setTotalScore(aggregate.getTotalScore() + 1);
                                }
                                aggregate.setAverageScore((double) aggregate.getTotalCorrectAnswers() / aggregate.getTotalQuestionsAnswered());
                            } else if (event instanceof QuizSessionStatusChangedEvent statusChangedEvent) {
                                // Only update if session is completed
                                if ("COMPLETED".equals(statusChangedEvent.newStatus())) {
                                    aggregate.setTotalSessions(aggregate.getTotalSessions() + 1);
                                    aggregate.setLastCompletedAt(statusChangedEvent.timestamp());
                                }
                                // Ensure quizType is set, as it's available in this event
                                aggregate.setQuizType(statusChangedEvent.quizType());
                            }
                            return aggregate;
                        },
                        Materialized.<String, UserQuizSessionStatistic, org.apache.kafka.streams.state.KeyValueStore<org.apache.kafka.common.utils.Bytes, byte[]>>as("user-quiz-statistics-store")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(userQuizSessionStatisticSerde)
                );

        // Write the final aggregated statistics to an output topic
        userQuizStatisticsTable.toStream()
                .to("user-quiz-statistics-output", Produced.with(Serdes.String(), userQuizSessionStatisticSerde));

        return userQuizStatisticsTable;
    }
}
