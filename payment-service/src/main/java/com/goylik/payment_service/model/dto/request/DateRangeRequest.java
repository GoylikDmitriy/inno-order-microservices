package com.goylik.payment_service.model.dto.request;

import com.goylik.payment_service.exception.InvalidDateRangeException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record DateRangeRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @NotNull @PastOrPresent LocalDateTime from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @NotNull LocalDateTime to
) {
    public DateRangeRequest {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidDateRangeException("'from' must not be after 'to'");
        }
    }
}
