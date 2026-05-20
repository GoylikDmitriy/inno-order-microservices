package com.goylik.order_service.kafka;

import com.goylik.order_service.exception.OrderNotFoundException;
import com.goylik.order_service.kafka.event.PaymentCreatedEvent;
import com.goylik.order_service.model.enums.OrderStatus;
import com.goylik.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {
    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "${kafka.topics.payment-created}",
            groupId = "order-service"
    )
    public void handlePaymentCreatedEvent(PaymentCreatedEvent event) {
        log.info("Received PaymentCreatedEvent: paymentId={}, orderId={}, status={}",
                event.paymentId(), event.orderId(), event.status());

        processPaymentEvent(event);
    }

    private void processPaymentEvent(PaymentCreatedEvent event) {
        var order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id = " + event.orderId()));

        OrderStatus newStatus = resolveOrderStatus(event.status());
        order.setStatus(newStatus);
        orderRepository.save(order);

        log.info("Order {} status updated to {} based on payment {}",
                event.orderId(), newStatus, event.paymentId());
    }

    private OrderStatus resolveOrderStatus(String paymentStatus) {
        return switch (paymentStatus) {
            case "SUCCESS" -> OrderStatus.PAID;
            case "FAILED" -> OrderStatus.PAYMENT_FAILED;
            default -> {
                log.warn("Unknown payment status: {}", paymentStatus);
                yield OrderStatus.PENDING;
            }
        };
    }
}
