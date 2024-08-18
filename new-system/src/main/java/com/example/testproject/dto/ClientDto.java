package com.example.testproject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record ClientDto(
        @NotNull
        UUID guid,
        @NotBlank
        String agency,
        @NotBlank
        String firstName,
        @NotBlank
        String lastName,
        @NotBlank
        String status,
        LocalDate dob,
        @NotNull
        LocalDateTime createdDateTime
) {
}
