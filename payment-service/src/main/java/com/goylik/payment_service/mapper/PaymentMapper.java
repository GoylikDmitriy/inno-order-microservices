package com.goylik.payment_service.mapper;

import com.goylik.payment_service.model.dto.request.CreatePaymentRequest;
import com.goylik.payment_service.model.dto.response.PaymentResponse;
import com.goylik.payment_service.model.entity.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    Payment toEntity(CreatePaymentRequest request);
    PaymentResponse toResponse(Payment payment);
}
