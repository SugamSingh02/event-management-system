package com.eventmanagment.service;

import com.eventmanagment.dto.UserDTO;
import com.eventmanagment.entity.User;
import com.eventmanagment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Test
    void loginShouldMatchUserNameOrEmailPasswordAndRole() {

        UserRepository userRepository = mock(UserRepository.class);

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        User user = new User(
                "Admin User",
                "admin@event.com",
                "+91 90000 00001",
                "ADMIN"
        );

        user.setPassword(passwordEncoder.encode("admin123"));

        when(userRepository.findByNameOrEmail("admin@event.com"))
                .thenReturn(Optional.of(user));

        UserService userService =
                new UserService(userRepository, passwordEncoder);

        UserDTO result =
                userService.login(
                        "admin@event.com",
                        "admin123",
                        "ADMIN"
                );

        assertEquals("Admin User", result.getName());
        assertEquals("admin@event.com", result.getEmail());
        assertEquals("ADMIN", result.getRole());
    }
}