package com.goylik.order_service.mapper;

import com.goylik.order_service.model.dto.response.OrderItemResponse;
import com.goylik.order_service.model.dto.response.OrderResponse;
import com.goylik.order_service.model.entity.Order;
import com.goylik.order_service.model.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    @Mapping(target = "user", ignore = true)
    OrderResponse toResponse(Order order);

    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "itemName", source = "item.name")
    @Mapping(target = "itemPrice", source = "item.price")
    OrderItemResponse toItemResponse(OrderItem orderItem);
}
