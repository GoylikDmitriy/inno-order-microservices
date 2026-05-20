package com.goylik.payment_service.kafka;

import com.goylik.payment_service.kafka.event.PaymentCreatedEvent;
import com.goylik.payment_service.model.entity.Payment;
import com.goylik.payment_service.model.enums.PaymentStatus;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventProducerTest {
    @Mock private KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate;

    @Captor private ArgumentCaptor<PaymentCreatedEvent> eventCaptor;

    private PaymentEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new PaymentEventProducer(kafkaTemplate);
        ReflectionTestUtils.setField(producer, "paymentCreatedTopic", "payment.created");
    }

    @Test
    void sendPaymentCreatedEvent_ShouldCallKafkaTemplateWithCorrectValues() {
        Payment payment = createPayment();
        PaymentCreatedEvent expectedEvent = new PaymentCreatedEvent(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getStatus(),
                payment.getPaymentAmount(),
                payment.getTimestamp()
        );

        CompletableFuture<SendResult<String, PaymentCreatedEvent>> future = new CompletableFuture<>();
        doReturn(future).when(kafkaTemplate)
                .send(eq("payment.created"), eq(String.valueOf(payment.getOrderId())), any(PaymentCreatedEvent.class));

        producer.sendPaymentCreatedEvent(payment);

        verify(kafkaTemplate).send(eq("payment.created"), eq(String.valueOf(payment.getOrderId())), eventCaptor.capture());
        PaymentCreatedEvent capturedEvent = eventCaptor.getValue();

        assertThat(capturedEvent).usingRecursiveComparison().isEqualTo(expectedEvent);
    }

    @Test
    void sendPaymentCreatedEvent_ShouldLogSuccessWhenSendCompletesSuccessfully() {
        Payment payment = createPayment();
        RecordMetadata metadata = mock(RecordMetadata.class);
        when(metadata.partition()).thenReturn(42);
        SendResult<String, PaymentCreatedEvent> sendResult = new SendResult<>(null, metadata);
        CompletableFuture<SendResult<String, PaymentCreatedEvent>> future = CompletableFuture.completedFuture(sendResult);
        doReturn(future).when(kafkaTemplate)
                .send(eq("payment.created"), eq(String.valueOf(payment.getOrderId())), any(PaymentCreatedEvent.class));

        producer.sendPaymentCreatedEvent(payment);

        verify(kafkaTemplate).send(eq("payment.created"), eq(String.valueOf(payment.getOrderId())), any(PaymentCreatedEvent.class));
    }

    @Test
    void sendPaymentCreatedEvent_ShouldLogErrorWhenSendFails() {
        Payment payment = createPayment();
        CompletableFuture<SendResult<String, PaymentCreatedEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka is down"));
        doReturn(future).when(kafkaTemplate)
                .send(eq("payment.created"), eq(String.valueOf(payment.getOrderId())), any(PaymentCreatedEvent.class));

        producer.sendPaymentCreatedEvent(payment);

        verify(kafkaTemplate).send(eq("payment.created"), eq(String.valueOf(payment.getOrderId())), any(PaymentCreatedEvent.class));
    }

    private Payment createPayment() {
        Payment payment = new Payment();
        payment.setId("p123");
        payment.setOrderId(456L);
        payment.setUserId(789L);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentAmount(BigDecimal.valueOf(199.99));
        payment.setTimestamp(null);
        return payment;
    }
}