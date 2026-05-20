package com.goylik.order_service.model.dto.client;

import java.time.LocalDate;

public record UserResponse(
        Long id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        Boolean active
) {}
