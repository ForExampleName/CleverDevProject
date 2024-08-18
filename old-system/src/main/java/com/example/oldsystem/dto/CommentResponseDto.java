package com.example.oldsystem.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CommentResponseDto(String comments, UUID guid, LocalDateTime modifiedDateTime, UUID clientGuid,
                                 LocalDateTime datetime, String loggedUser, LocalDateTime createdDateTime) {
}
