package com.eventmanagment.controller;

import com.eventmanagment.entity.User;
import com.eventmanagment.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // =========================
    // ADMIN - CREATE USER
    // =========================

    @PostMapping
    public User createUser(
            @Valid @RequestBody User user) {

        return userService.createUser(user);
    }

    // =========================
    // ADMIN - GET ALL USERS
    // =========================

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // =========================
    // ADMIN - GET USER BY ID
    // =========================

    @GetMapping("/{id}")
    public User getUserById(
            @PathVariable Long id) {

        return userService.getUserById(id);
    }

    // =========================
    // ADMIN - GET USER BY EMAIL
    // =========================

    @GetMapping("/email")
    public User getUserByEmail(
            @RequestParam String email) {

        return userService.getUserByEmail(email);
    }

    // =========================
    // MY PROFILE
    // =========================

    @GetMapping("/me")
    public User getMyProfile(
            Principal principal) {

        return userService.getMyProfile(
                principal.getName());
    }

    // =========================
    // UPDATE MY PROFILE
    // =========================

    @PutMapping("/me")
    public User updateMyProfile(
            @RequestBody Map<String, String> request,
            Principal principal) {

        String name = request.get("name");
        String email = request.get("email");

        return userService.updateMyProfile(
                principal.getName(),
                name,
                email
        );
    }

    // =========================
    // CHANGE MY PASSWORD
    // =========================

    @PutMapping("/me/password")
    public String changePassword(
            @RequestBody Map<String, String> request,
            Principal principal) {

        String currentPassword =
                request.get("currentPassword");

        String newPassword =
                request.get("newPassword");

        userService.changePassword(
                principal.getName(),
                currentPassword,
                newPassword
        );

        return "Password changed successfully";
    }

    // =========================
    // ADMIN - DELETE USER
    // =========================

    @DeleteMapping("/{id}")
    public String deleteUser(
            @PathVariable Long id) {

        userService.deleteUser(id);

        return "User deleted successfully";
    }
}