package com.eventmanagment.service;

import com.eventmanagment.entity.User;
import com.eventmanagment.entity.UserRole;
import com.eventmanagment.repository.UserRepository;
import com.eventmanagment.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // =========================
    // PUBLIC REGISTRATION
    // =========================

    public User register(User user) {

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException(
                    "Email already registered");
        }

        // IMPORTANT:
        // Public users can only register as ATTENDEE
        user.setRole(UserRole.ATTENDEE);

        user.setPassword(
                passwordEncoder.encode(
                        user.getPassword()
                )
        );

        return userRepository.save(user);
    }

    // =========================
    // LOGIN
    // =========================

    public String login(
            String email,
            String password) {

        User user = userRepository
                .findByEmail(email)
                .orElse(null);

        if (user == null) {
            throw new RuntimeException(
                    "Invalid email or password");
        }

        if (!passwordEncoder.matches(
                password,
                user.getPassword())) {

            throw new RuntimeException(
                    "Invalid email or password");
        }

        return jwtService.generateToken(
                user.getEmail(),
                user.getRole().name()
        );
    }
}