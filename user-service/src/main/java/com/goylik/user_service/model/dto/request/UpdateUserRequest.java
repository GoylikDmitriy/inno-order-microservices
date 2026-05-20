package com.goylik.user_service.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateUserRequest(
        @Size(max = 127) String name,
        @Size(max = 127) String surname,
        @Past LocalDate birthDate,
        @Email @Size(max = 255) String email
) {
}
