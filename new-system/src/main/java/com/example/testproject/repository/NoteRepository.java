package com.example.testproject.repository;

import com.example.testproject.entity.Note;
import org.springframework.data.repository.ListCrudRepository;

import java.util.UUID;

public interface NoteRepository extends ListCrudRepository<Note, Long> {
    Note findByCommentId(UUID commentId);

    boolean existsByCommentId(UUID commentId);
}
