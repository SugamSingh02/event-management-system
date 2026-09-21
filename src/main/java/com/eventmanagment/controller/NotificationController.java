package com.eventmanagment.controller;

import com.eventmanagment.entity.Notification;
import com.eventmanagment.entity.User;
import com.eventmanagment.repository.UserRepository;
import com.eventmanagment.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(
            NotificationService notificationService,
            UserRepository userRepository) {

        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    // =========================
    // GET CURRENT USER
    // =========================

    private User getCurrentUser(Principal principal) {

        return userRepository
                .findByEmail(principal.getName())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }

    // =========================
    // GET MY NOTIFICATIONS
    // =========================

    @GetMapping("/my")
    public List<Notification> getMyNotifications(
            Principal principal) {

        Long userId = getCurrentUser(principal).getId();

        return notificationService
                .getUserNotifications(userId);
    }

    // =========================
    // GET MY UNREAD NOTIFICATIONS
    // =========================

    @GetMapping("/unread")
    public List<Notification> getUnreadNotifications(
            Principal principal) {

        Long userId = getCurrentUser(principal).getId();

        return notificationService
                .getUnreadNotifications(userId);
    }

    // =========================
    // GET UNREAD COUNT
    // =========================

    @GetMapping("/unread/count")
    public long getUnreadCount(
            Principal principal) {

        Long userId = getCurrentUser(principal).getId();

        return notificationService
                .getUnreadCount(userId);
    }

    // =========================
    // MARK NOTIFICATION AS READ
    // =========================

    @PutMapping("/{id}/read")
    public Notification markAsRead(
            @PathVariable Long id,
            Principal principal) {

        Long userId = getCurrentUser(principal).getId();

        return notificationService
                .markAsRead(id, userId);
    }

    // =========================
    // DELETE MY NOTIFICATION
    // =========================

    @DeleteMapping("/{id}")
    public String deleteNotification(
            @PathVariable Long id,
            Principal principal) {

        Long userId = getCurrentUser(principal).getId();

        notificationService
                .deleteNotification(id, userId);

        return "Notification deleted successfully";
    }
}