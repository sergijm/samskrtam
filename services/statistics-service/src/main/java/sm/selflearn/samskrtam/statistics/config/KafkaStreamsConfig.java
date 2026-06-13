package sm.selflearn.samskrtam.statistics.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerde;
import sm.selflearn.samskrtam.quiz.event.QuizAnsweredEvent;
import sm.selflearn.samskrtam.quiz.event.QuizSessionStatusChangedEvent;
import sm.selflearn.samskrtam.quiz.event.StatisticEvent;
import sm.selflearn.samskrtam.statistics.model.UserQuizSessionStatistic;
import org.apache.kafka.streams.StreamsConfig;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableKafkaStreams
@RequiredArgsConstructor
@Slf4j
public class KafkaStreamsConfig {

    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.streams.application-id}")
    private String applicationId;

    @Bean
    public NewTopic quizAnsweredEventsTopic() {
        return TopicBuilder.name("quiz-answered-events").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic quizSessionStatusChangedEventsTopic() {
        return TopicBuilder.name("quiz-session-status-changed-events").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic userQuizStatisticsOutputTopic() {
        return TopicBuilder.name("user-quiz-statistics-output").partitions(1).replicas(1).build();
    }

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, StatisticEvent.class.getName());
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class);
        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public Serde<UserQuizSessionStatistic> userQuizSessionStatisticSerde() {
        return new JsonSerde<>(UserQuizSessionStatistic.class, this.objectMapper);
    }

    @Bean
    public KTable<String, UserQuizSessionStatistic> processQuizStatistics(
            StreamsBuilder builder,
            Serde<UserQuizSessionStatistic> userQuizSessionStatisticSerde) {

        KStream<String, StatisticEvent> statisticEvents = builder.stream(
                Arrays.asList("quiz-answered-events", "quiz-session-status-changed-events")
        );

        Materialized<String, UserQuizSessionStatistic, KeyValueStore<Bytes, byte[]>> materialized =
                Materialized.<String, UserQuizSessionStatistic, KeyValueStore<Bytes, byte[]>>as("user-quiz-statistics-store")
                        .withValueSerde(userQuizSessionStatisticSerde);

        Produced<String, UserQuizSessionStatistic> produced = Produced.with(Serdes.String(), userQuizSessionStatisticSerde);

        KTable<String, UserQuizSessionStatistic> userQuizStatisticsTable = statisticEvents
                .groupBy((key, event) -> {
                    if (event == null) {
                        log.warn("groupBy received null event with key: {}. Grouping as invalid.", key);
                        return "invalid-event-key";
                    }
                    if (event.userId() == null || event.quizId() == null) {
                        log.warn("groupBy received event with null userId or quizId: {}. Grouping as invalid.", event);
                        return "invalid-event-key";
                    }
                    return event.userId().toString() + "-" + event.quizId().toString();
                })
                .aggregate(
                        UserQuizSessionStatistic::new,
                        (key, event, aggregate) -> {
                            if (event == null) {
                                log.warn("aggregate received null event with key: {}. Skipping.", key);
                                return aggregate;
                            }
                            if (aggregate.getUserId() == null) {
                                aggregate.setId(UUID.randomUUID());
                                aggregate.setUserId(event.userId());
                                aggregate.setQuizId(event.quizId());
                                aggregate.setQuizType(event.quizType());
                                aggregate.setTotalSessions(0);
                                aggregate.setTotalQuestionsAnswered(0);
                                aggregate.setTotalCorrectAnswers(0);
                                aggregate.setTotalScore(0);
                                aggregate.setAverageScore(0.0);
                                aggregate.setLastCompletedAt(null);
                            }

                            if (event instanceof QuizAnsweredEvent answeredEvent) {
                                aggregate.setTotalQuestionsAnswered(aggregate.getTotalQuestionsAnswered() + 1);
                                if (answeredEvent.isCorrect()) {
                                    aggregate.setTotalCorrectAnswers(aggregate.getTotalCorrectAnswers() + 1);
                                    aggregate.setTotalScore(aggregate.getTotalScore() + 1);
                                }
                                if (aggregate.getTotalQuestionsAnswered() > 0) {
                                    aggregate.setAverageScore((double) aggregate.getTotalCorrectAnswers() / aggregate.getTotalQuestionsAnswered());
                                }
                            } else if (event instanceof QuizSessionStatusChangedEvent statusChangedEvent) {
                                if ("COMPLETED".equals(statusChangedEvent.newStatus())) {
                                    aggregate.setTotalSessions(aggregate.getTotalSessions() + 1);
                                    aggregate.setLastCompletedAt(statusChangedEvent.timestamp());
                                }
                                if (statusChangedEvent.quizType() != null) {
                                    aggregate.setQuizType(statusChangedEvent.quizType());
                                }
                            }
                            return aggregate;
                        },
                        materialized
                );

        userQuizStatisticsTable.toStream()
                .to("user-quiz-statistics-output", produced);

        return userQuizStatisticsTable;
    }
}
