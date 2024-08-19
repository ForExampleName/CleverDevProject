package com.example.testproject.repository;

import com.example.testproject.entity.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends ListCrudRepository<User, Long> {
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO company_user (login)
        VALUES (:login)
        ON CONFLICT (login) DO NOTHING
    """, nativeQuery = true)
    int createByLoginIfNotExists(@Param("login") String login);

    User findByLogin(String login);
}
