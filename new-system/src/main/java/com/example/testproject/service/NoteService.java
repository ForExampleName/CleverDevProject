package com.example.testproject.service;

import com.example.testproject.dto.ClientDto;
import com.example.testproject.dto.CommentResponseDto;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public interface NoteService {
    void synchronizeNotesConcurrently(List<ClientDto> clients) throws ExecutionException, InterruptedException;

    void synchronizeNotes(List<ClientDto> clients);

    List<CommentResponseDto> fetchClientComments(UUID clientGuid, String agency);

    void filterInvalidCommentData(List<CommentResponseDto> commentData);
}
