package com.example.testproject.service;

import com.example.testproject.entity.User;

public interface UserService {
    User createUserIfNotExists(String login);
}
