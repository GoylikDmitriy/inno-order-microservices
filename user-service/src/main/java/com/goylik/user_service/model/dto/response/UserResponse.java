package com.goylik.user_service.model.dto.response;

import java.time.LocalDate;

public record UserResponse(
        Long id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        Boolean active
) {
}
