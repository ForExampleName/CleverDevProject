package com.example.testproject.service.impl;

import com.example.testproject.entity.User;
import com.example.testproject.repository.UserRepository;
import com.example.testproject.service.UserService;
import com.example.testproject.statistics.Statistics;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    private final Statistics statistics;

    public UserServiceImpl(UserRepository userRepository,
                           Statistics statistics) {
        this.userRepository = userRepository;
        this.statistics = statistics;
    }

    @Override
    public User createUserIfNotExists(String login) {
        int result = userRepository.createByLoginIfNotExists(login);
        if(result != 0) {
            statistics.addNewUsers(1);
        }
        return userRepository.findByLogin(login);
    }
}
