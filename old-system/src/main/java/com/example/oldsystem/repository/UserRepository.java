package com.example.oldsystem.repository;

import com.example.oldsystem.entity.User;
import org.springframework.data.repository.ListCrudRepository;

public interface UserRepository extends ListCrudRepository<User, Long> {
}
