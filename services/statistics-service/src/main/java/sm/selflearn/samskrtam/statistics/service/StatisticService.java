package sm.selflearn.samskrtam.statistics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.statistics.model.UserQuizSessionStatistic;

import java.util.List;
import java.util.Spliterators;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.KeyValueIterator;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticService {

    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    private ReadOnlyKeyValueStore<String, UserQuizSessionStatistic> getUserQuizStatisticsStore() {
        KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
        if (kafkaStreams == null) {
            throw new IllegalStateException("KafkaStreams instance is not available from factory bean.");
        }
        // Ensure KafkaStreams is in a running state before querying
        if (kafkaStreams.state() != KafkaStreams.State.RUNNING) {
            log.warn("KafkaStreams is not in RUNNING state. Current state: {}", kafkaStreams.state());
            throw new IllegalStateException("Kafka Streams is not fully started. Current state: " + kafkaStreams.state());
        }

        try {
            return kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType("user-quiz-statistics-store", QueryableStoreTypes.keyValueStore())
            );
        } catch (InvalidStateStoreException e) {
            log.error("Kafka Streams state store is not yet ready or not found: {}", e.getMessage());
            throw new IllegalStateException("Statistics service is not fully initialized. Please try again later.", e);
        }
    }

    public Page<UserQuizSessionStatistic> getUserQuizStatistics(UUID userId, Pageable pageable) {
        ReadOnlyKeyValueStore<String, UserQuizSessionStatistic> store = getUserQuizStatisticsStore();

        List<UserQuizSessionStatistic> userStatistics;
        try (KeyValueIterator<String, UserQuizSessionStatistic> iterator = store.all()) {
            userStatistics = StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false)
                    .filter(kv -> kv.key.startsWith(userId.toString() + "-"))
                    .map(kv -> kv.value)
                    .collect(Collectors.toList());
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), userStatistics.size());
        List<UserQuizSessionStatistic> pagedStatistics = userStatistics.subList(start, end);

        return new PageImpl<>(pagedStatistics, pageable, userStatistics.size());
    }

    public UserQuizSessionStatistic getUserQuizStatistic(UUID userId, UUID quizId) {
        ReadOnlyKeyValueStore<String, UserQuizSessionStatistic> store = getUserQuizStatisticsStore();
        String key = userId.toString() + "-" + quizId.toString();
        return store.get(key);
    }
}
