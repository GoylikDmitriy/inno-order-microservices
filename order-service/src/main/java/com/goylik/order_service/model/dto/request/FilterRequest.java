package com.goylik.order_service.model.dto.request;

import com.goylik.order_service.exception.InvalidDateRangeException;
import com.goylik.order_service.model.enums.OrderStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

public record FilterRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime to,
        List<OrderStatus> statuses
) {
    public FilterRequest {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidDateRangeException("'from' date must not be after 'to' date");
        }
    }
}
