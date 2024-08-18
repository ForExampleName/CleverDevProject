package com.example.oldsystem.dto;

import lombok.Builder;

@Builder
public record ClientDto(
        String guid,
        String agency,
        String firstName,
        String lastName,
        String status,
        String dob,
        String createdDateTime
) {
}
