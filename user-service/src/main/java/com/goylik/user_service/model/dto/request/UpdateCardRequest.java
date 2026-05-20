package com.goylik.user_service.model.dto.request;

import jakarta.validation.constraints.Size;

import java.time.YearMonth;

public record UpdateCardRequest(
        @Size(max = 255) String number,
        @Size(max = 255) String holder,
        YearMonth expirationDate
) {
}
