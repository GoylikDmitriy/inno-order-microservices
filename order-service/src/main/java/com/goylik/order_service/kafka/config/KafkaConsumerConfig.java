package com.goylik.order_service.kafka.config;

import com.goylik.order_service.kafka.event.PaymentCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {
    private final KafkaProperties kafkaProperties;

    @Bean
    public ConsumerFactory<String, PaymentCreatedEvent> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentCreatedEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, PaymentCreatedEvent> consumerFactory,
            KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, PaymentCreatedEvent>();
        factory.setConsumerFactory(consumerFactory);

        Integer concurrency = kafkaProperties.getListener().getConcurrency();
        factory.setConcurrency(concurrency == null ? 1 : concurrency);

        factory.setCommonErrorHandler(new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate),
                new FixedBackOff(1000L, 3)
        ));

        return factory;
    }
}
