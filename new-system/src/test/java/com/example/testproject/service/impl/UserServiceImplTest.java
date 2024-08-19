package com.example.testproject.service.impl;

import com.example.testproject.entity.User;
import com.example.testproject.repository.UserRepository;
import com.example.testproject.statistics.Statistics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private Statistics statistics;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Trying create user with existing user login")
    public void testCreateUserIfNotExistsWithExistingLogin() {
        User existingUser = new User();
        existingUser.setLogin("login");

        when(userRepository.createByLoginIfNotExists(existingUser.getLogin())).thenReturn(0);
        when(userRepository.findByLogin(existingUser.getLogin())).thenReturn(existingUser);

        User returnedUser = userService.createUserIfNotExists(existingUser.getLogin());

        verify(statistics, times(0)).addNewUsers(1);
        Assertions.assertEquals(returnedUser.getLogin(), existingUser.getLogin());
    }

    @Test
    @DisplayName("Trying create user with new user login")
    public void testCreateUserIfNotExistsWithNewLogin() {
        final String newLogin = "login";

        User newUser = new User();
        newUser.setLogin(newLogin);

        when(userRepository.createByLoginIfNotExists(newLogin)).thenReturn(1);
        when(userRepository.findByLogin(any())).thenReturn(newUser);

        User returnedUser = userService.createUserIfNotExists(newLogin);

        verify(statistics, times(1)).addNewUsers(1);
        Assertions.assertEquals(returnedUser.getLogin(), newLogin);
    }
}
