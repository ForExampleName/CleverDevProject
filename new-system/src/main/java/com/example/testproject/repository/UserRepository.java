package com.example.testproject.repository;

import com.example.testproject.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.ListCrudRepository;

public interface UserRepository extends ListCrudRepository<User, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    boolean existsByLogin(String login);

    User findByLogin(String login);
}
