package com.goylik.payment_service.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Value("${kafka.topics.payment-created}")
    private String paymentCreatedTopic;

    @Bean
    public NewTopic paymentCreatedTopic() {
        return TopicBuilder.name(paymentCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
