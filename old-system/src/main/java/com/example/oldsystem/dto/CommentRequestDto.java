package com.example.oldsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class CommentRequestDto {
    private String agency;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private UUID clientGuid;
}
