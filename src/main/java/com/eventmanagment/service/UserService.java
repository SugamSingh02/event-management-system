package com.eventmanagment.service;

import com.eventmanagment.entity.User;
import com.eventmanagment.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // =========================
    // CREATE USER - ADMIN
    // =========================

    public User createUser(User user) {

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException(
                    "Email already registered");
        }

        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        return userRepository.save(user);
    }

    // =========================
    // GET ALL USERS - ADMIN
    // =========================

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // =========================
    // GET USER BY ID - ADMIN
    // =========================

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    // =========================
    // GET USER BY EMAIL - ADMIN
    // =========================

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    // =========================
    // GET MY PROFILE
    // =========================

    public User getMyProfile(String email) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }

    // =========================
    // UPDATE MY PROFILE
    // =========================

    public User updateMyProfile(
            String currentEmail,
            String name,
            String email) {

        User user = userRepository
                .findByEmail(currentEmail)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (email == null || email.isBlank()) {
            throw new RuntimeException(
                    "Email is required");
        }

        // If email is changed, make sure another user
        // is not already using it.
        if (!email.equalsIgnoreCase(currentEmail)
                && userRepository.existsByEmail(email)) {

            throw new RuntimeException(
                    "Email already registered");
        }

        if (name != null && !name.isBlank()) {
            user.setName(name);
        }

        user.setEmail(email);

        return userRepository.save(user);
    }

    // =========================
    // CHANGE PASSWORD
    // =========================

    public void changePassword(
            String email,
            String currentPassword,
            String newPassword) {

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (!passwordEncoder.matches(
                currentPassword,
                user.getPassword())) {

            throw new RuntimeException(
                    "Current password is incorrect");
        }

        if (newPassword == null
                || newPassword.length() < 6) {

            throw new RuntimeException(
                    "New password must be at least 6 characters");
        }

        user.setPassword(
                passwordEncoder.encode(newPassword)
        );

        userRepository.save(user);
    }

    // =========================
    // DELETE USER - ADMIN
    // =========================

    public void deleteUser(Long id) {

        if (!userRepository.existsById(id)) {
            throw new RuntimeException(
                    "User not found");
        }

        userRepository.deleteById(id);
    }
}