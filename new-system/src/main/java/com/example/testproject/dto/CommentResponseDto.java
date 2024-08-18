package com.example.testproject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CommentResponseDto(
        @NotBlank
        String comments,
        @NotNull
        UUID guid,
        @NotNull
        UUID clientGuid,
        LocalDateTime datetime,
        @NotBlank
        String loggedUser,
        @NotNull
        LocalDateTime createdDateTime,
        LocalDateTime modifiedDateTime
) {

}
