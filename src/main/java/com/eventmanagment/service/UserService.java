package com.eventmanagment.service;

import com.eventmanagment.dto.UserDTO;
import com.eventmanagment.entity.User;
import com.eventmanagment.exception.ResourceNotFoundException;
import com.eventmanagment.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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

    // --------------------------------------------------
    // Public registration
    // --------------------------------------------------

    public UserDTO registerUser(UserDTO dto) {

        String normalizedEmail =
                dto.getEmail() == null
                        ? ""
                        : dto.getEmail().trim().toLowerCase(Locale.ROOT);

        String normalizedName =
                dto.getName() == null
                        ? ""
                        : dto.getName().trim();

        String normalizedPassword =
                dto.getPassword() == null
                        ? ""
                        : dto.getPassword();

        if (normalizedEmail.isBlank()
                || normalizedName.isBlank()
                || normalizedPassword.isBlank()) {

            throw new IllegalArgumentException(
                    "Name, email and password are required"
            );
        }

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException(
                    "User with this email already exists"
            );
        }

        User user = new User();

        user.setName(normalizedName);
        user.setEmail(normalizedEmail);
        user.setPhone(dto.getPhone());

        // Public registration always creates an ATTENDEE.
        // The client cannot choose ADMIN or ORGANIZER.
        user.setRole("ATTENDEE");

        user.setPassword(
                passwordEncoder.encode(normalizedPassword)
        );

        User saved = userRepository.save(user);

        return UserDTO.fromEntity(saved);
    }

    // --------------------------------------------------
    // Admin user creation
    // --------------------------------------------------

    public UserDTO createUser(UserDTO dto) {

        User currentUser = getAuthenticatedUser();

        if (!currentUser.getRole().equals("ADMIN")) {
            throw new AccessDeniedException(
                    "Only administrators can create users"
            );
        }

        String normalizedEmail =
                dto.getEmail() == null
                        ? ""
                        : dto.getEmail().trim().toLowerCase(Locale.ROOT);

        String normalizedName =
                dto.getName() == null
                        ? ""
                        : dto.getName().trim();

        String normalizedRole =
                dto.getRole() == null
                        ? ""
                        : dto.getRole().trim().toUpperCase(Locale.ROOT);

        String normalizedPassword =
                dto.getPassword() == null
                        ? ""
                        : dto.getPassword();

        if (normalizedEmail.isBlank()
                || normalizedName.isBlank()
                || normalizedRole.isBlank()
                || normalizedPassword.isBlank()) {

            throw new IllegalArgumentException(
                    "Name, email, password and role are required"
            );
        }

        validateRole(normalizedRole);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException(
                    "User with this email already exists"
            );
        }

        User user = new User();

        user.setName(normalizedName);
        user.setEmail(normalizedEmail);
        user.setPhone(dto.getPhone());
        user.setPassword(
                passwordEncoder.encode(normalizedPassword)
        );
        user.setRole(normalizedRole);

        User saved = userRepository.save(user);

        return UserDTO.fromEntity(saved);
    }

    // --------------------------------------------------
    // Login
    // --------------------------------------------------

    public UserDTO login(
            String identifier,
            String password,
            String role) {

        String normalizedIdentifier =
                identifier == null
                        ? ""
                        : identifier.trim();

        String normalizedPassword =
                password == null
                        ? ""
                        : password;

        String normalizedRole =
                role == null
                        ? ""
                        : role.trim().toUpperCase(Locale.ROOT);

        if (normalizedIdentifier.isBlank()
                || normalizedPassword.isBlank()
                || normalizedRole.isBlank()) {

            throw new IllegalArgumentException(
                    "Username or email, password and role are required"
            );
        }

        User user =
                userRepository.findByNameOrEmail(
                        normalizedIdentifier
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invalid username/email or password"
                        )
                );

        if (!passwordEncoder.matches(
                normalizedPassword,
                user.getPassword())) {

            throw new ResourceNotFoundException(
                    "Invalid username/email or password"
            );
        }

        if (!user.getRole().equals(normalizedRole)) {
            throw new ResourceNotFoundException(
                    "Selected role does not match the account role"
            );
        }

        return UserDTO.fromEntity(user);
    }

    // --------------------------------------------------
    // Get all users
    // --------------------------------------------------

    public List<UserDTO> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // --------------------------------------------------
    // Get user by ID
    // --------------------------------------------------

    public UserDTO getUserById(Long id) {

        User currentUser = getAuthenticatedUser();

        boolean isAdmin =
                currentUser.getRole().equals("ADMIN");

        boolean isOwner =
                currentUser.getId().equals(id);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException(
                    "You can only view your own account"
            );
        }

        User user =
                userRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found with id: " + id
                                ));

        return UserDTO.fromEntity(user);
    }

    // --------------------------------------------------
    // Update user
    // --------------------------------------------------

    public UserDTO updateUser(Long id, UserDTO dto) {

        User currentUser = getAuthenticatedUser();

        boolean isAdmin =
                currentUser.getRole().equals("ADMIN");

        boolean isOwner =
                currentUser.getId().equals(id);

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException(
                    "You can only update your own account"
            );
        }

        User existing =
                userRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found with id: " + id
                                ));

        String normalizedEmail =
                dto.getEmail() == null
                        ? ""
                        : dto.getEmail().trim().toLowerCase(Locale.ROOT);

        String normalizedName =
                dto.getName() == null
                        ? ""
                        : dto.getName().trim();

        if (normalizedName.isBlank()
                || normalizedEmail.isBlank()) {

            throw new IllegalArgumentException(
                    "Name and email are required"
            );
        }

        // Prevent duplicate email on update.
        userRepository.findByEmail(normalizedEmail)
                .ifPresent(otherUser -> {

                    if (!otherUser.getId().equals(id)) {
                        throw new IllegalArgumentException(
                                "User with this email already exists"
                        );
                    }
                });

        existing.setName(normalizedName);
        existing.setEmail(normalizedEmail);
        existing.setPhone(dto.getPhone());

        // Only admins can change roles.
        if (isAdmin) {

            String normalizedRole =
                    dto.getRole() == null
                            ? ""
                            : dto.getRole()
                                    .trim()
                                    .toUpperCase(Locale.ROOT);

            if (!normalizedRole.isBlank()) {
                validateRole(normalizedRole);
                existing.setRole(normalizedRole);
            }
        }

        // Hash only when a new password is supplied.
        if (dto.getPassword() != null
                && !dto.getPassword().isBlank()) {

            existing.setPassword(
                    passwordEncoder.encode(dto.getPassword())
            );
        }

        User updated =
                userRepository.save(existing);

        return UserDTO.fromEntity(updated);
    }

    // --------------------------------------------------
    // Delete user
    // --------------------------------------------------

    public void deleteUser(Long id) {

        User currentUser = getAuthenticatedUser();

        if (!currentUser.getRole().equals("ADMIN")) {
            throw new AccessDeniedException(
                    "Only administrators can delete users"
            );
        }

        User user =
                userRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found with id: " + id
                                ));

        userRepository.delete(user);
    }

    // --------------------------------------------------
    // Find ID by login identifier
    // --------------------------------------------------

    public Long findUserIdByIdentifier(
            String identifier) {

        return userRepository
                .findIdByNameOrEmail(identifier.trim())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));
    }

    // --------------------------------------------------
    // Get currently authenticated user
    // --------------------------------------------------

    public User getAuthenticatedUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new AccessDeniedException(
                    "User is not authenticated"
            );
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"
                        ));
    }

    // --------------------------------------------------
    // Validate role
    // --------------------------------------------------

    private void validateRole(String role) {

        if (!role.equals("ADMIN")
                && !role.equals("ORGANIZER")
                && !role.equals("ATTENDEE")) {

            throw new IllegalArgumentException(
                    "Invalid user role"
            );
        }
    }
}