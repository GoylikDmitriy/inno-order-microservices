package com.goylik.user_service.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.YearMonth;

public record CreateCardRequest(
        @NotNull @PositiveOrZero Long userId,
        @NotBlank @Size(max = 255) String number,
        @NotBlank @Size(max = 255) String holder,
        @NotNull YearMonth expirationDate
) {
}
