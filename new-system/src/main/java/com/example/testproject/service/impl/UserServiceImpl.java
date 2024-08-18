package com.example.testproject.service.impl;

import com.example.testproject.entity.User;
import com.example.testproject.repository.UserRepository;
import com.example.testproject.service.UserService;
import com.example.testproject.statistics.Statistics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public User createUserIfNotExists(String login) {
        if (!userRepository.existsByLogin(login)) {
            User newUser = new User();
            newUser.setLogin(login);
            statistics.addNewUsers(1);
            return userRepository.save(newUser);
        }

        return userRepository.findByLogin(login);
    }
}
