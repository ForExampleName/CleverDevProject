package com.example.oldsystem.repository.old;

import com.example.oldsystem.entity.old.Comment;
import org.springframework.data.repository.ListCrudRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CommentRepository extends ListCrudRepository<Comment, UUID> {
    List<Comment> findByClient_IdAndClient_Agency_NameAndCreatedAtBetween(UUID clientId, String agency, LocalDateTime from, LocalDateTime to);
}
