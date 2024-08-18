package com.example.oldsystem.repository;

import com.example.oldsystem.entity.Note;
import org.springframework.data.repository.ListCrudRepository;

public interface NoteRepository extends ListCrudRepository<Note, Long> {
}
