package com.goylik.payment_service.kafka;

import com.goylik.payment_service.model.entity.Payment;
import com.goylik.payment_service.kafka.event.PaymentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {
    private final KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate;

    @Value("${kafka.topics.payment-created}")
    private String paymentCreatedTopic;

    public void sendPaymentCreatedEvent(Payment payment) {
        var event = new PaymentCreatedEvent(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getStatus(),
                payment.getPaymentAmount(),
                payment.getTimestamp()
        );

        kafkaTemplate.send(paymentCreatedTopic, String.valueOf(payment.getOrderId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send PaymentCreatedEvent for paymentId={}, orderId={}. Error: {}",
                                payment.getId(), payment.getOrderId(), ex.getMessage());
                    } else {
                        log.info("PaymentCreatedEvent sent: paymentId={}, orderId={}, status={}, partition={}",
                                payment.getId(),
                                payment.getOrderId(),
                                payment.getStatus(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
