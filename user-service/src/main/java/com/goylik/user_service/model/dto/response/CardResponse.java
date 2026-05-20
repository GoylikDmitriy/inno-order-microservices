package com.goylik.user_service.model.dto.response;

import java.time.YearMonth;

public record CardResponse(
        Long id,
        Long userId,
        String number,
        String holder,
        YearMonth expirationDate,
        Boolean active
) {
}
