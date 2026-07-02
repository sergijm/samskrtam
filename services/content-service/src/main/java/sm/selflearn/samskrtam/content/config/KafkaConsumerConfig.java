package sm.selflearn.samskrtam.content.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Slf4j
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory);
        // Ручное подтверждение (Acknowledgment.acknowledge())
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        // Не останавливать контейнер при ошибке
        factory.setCommonErrorHandler(new CommonErrorHandler() {
            @Override
            public boolean handleOne(Exception thrownException, ConsumerRecord<?, ?> record,
                                  org.apache.kafka.clients.consumer.Consumer<?, ?> consumer,
                                  org.springframework.kafka.listener.MessageListenerContainer container) {
                log.error("Error processing Kafka message: topic={}, offset={}, exception={}",
                        record.topic(), record.offset(), thrownException.getMessage(), thrownException);
                return false; // continue processing other records
            }
        });
        return factory;
    }
}