package com.example.oldsystem.api;

import com.example.oldsystem.dto.CommentRequestDto;
import com.example.oldsystem.dto.CommentResponseDto;
import com.example.oldsystem.repository.old.CommentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(value = {"/api/v1/notes", "/api/v1/comments"})
public class NoteController {
    private final CommentRepository commentRepository;

    public NoteController(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @PostMapping // why not get ? because post is in the task description
    public ResponseEntity<List<CommentResponseDto>> getClientComments(@RequestBody CommentRequestDto commentRequestDto) {
        List<CommentResponseDto> responseDto = commentRepository.findByClient_IdAndClient_Agency_NameAndCreatedAtBetween(
                        commentRequestDto.getClientGuid(), commentRequestDto.getAgency(),
                        commentRequestDto.getDateFrom().atStartOfDay(),
                        commentRequestDto.getDateTo().atTime(23, 59, 59)
                ).stream()
                .map(comment -> CommentResponseDto.builder()
                        .comments(comment.getComment())
                        .guid(comment.getId())
                        .modifiedDateTime(comment.getModifiedAt())
                        .clientGuid(comment.getClient().getId())
                        .datetime(LocalDateTime.now()) // not used, padding
                        .loggedUser(comment.getUser())
                        .createdDateTime(comment.getCreatedAt())
                        .build())
                .toList();

        // 404 not_found is absent because logic is like get all items (empty list the result is possible)

        return ResponseEntity.ok(responseDto);
    }
}
