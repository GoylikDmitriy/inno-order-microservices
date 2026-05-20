package com.goylik.user_service.mapper;

import com.goylik.user_service.model.dto.request.CreateCardRequest;
import com.goylik.user_service.model.dto.request.UpdateCardRequest;
import com.goylik.user_service.model.dto.response.CardResponse;
import com.goylik.user_service.model.entity.PaymentCard;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CardMapper {
    PaymentCard toEntity(CreateCardRequest request);

    @Mapping(
            target = "userId",
            source = "card.user.id"
    )
    CardResponse toResponse(PaymentCard card, String decryptedNumber);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCardFromDto(UpdateCardRequest request, @MappingTarget PaymentCard card);
}
