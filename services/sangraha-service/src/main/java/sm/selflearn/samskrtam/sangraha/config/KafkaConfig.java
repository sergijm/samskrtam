package sm.selflearn.samskrtam.sangraha.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;

@Slf4j
@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public NewTopic sangrahaVocabularyEventsTopic() {
        return TopicBuilder.name("sangraha-vocabulary-events")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> sangrahaKafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(new CommonErrorHandler() {
            @Override
            public boolean handleOne(Exception thrownException, ConsumerRecord<?, ?> record,
                                  org.apache.kafka.clients.consumer.Consumer<?, ?> consumer,
                                  org.springframework.kafka.listener.MessageListenerContainer container) {
                log.error("Error processing Kafka message: topic={}, offset={}, exception={}",
                        record.topic(), record.offset(), thrownException.getMessage(), thrownException);
                return false;
            }
        });
        return factory;
    }
}