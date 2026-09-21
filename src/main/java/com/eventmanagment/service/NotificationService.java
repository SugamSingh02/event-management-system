package com.eventmanagment.service;

import com.eventmanagment.entity.Notification;
import com.eventmanagment.entity.User;
import com.eventmanagment.repository.NotificationRepository;
import com.eventmanagment.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository) {

        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // =========================
    // CREATE NOTIFICATION
    // =========================

    public Notification createNotification(
            Long userId,
            String message,
            String type) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Notification notification = new Notification();

        notification.setUser(user);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    // =========================
    // GET MY NOTIFICATIONS
    // =========================

    public List<Notification> getUserNotifications(
            Long userId) {

        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
    }

    // =========================
    // GET MY UNREAD NOTIFICATIONS
    // =========================

    public List<Notification> getUnreadNotifications(
            Long userId) {

        return notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(
                        userId);
    }

    // =========================
    // UNREAD COUNT
    // =========================

    public long getUnreadCount(Long userId) {

        return notificationRepository
                .countByUserIdAndReadFalse(userId);
    }

    // =========================
    // MARK AS READ
    // =========================

    public Notification markAsRead(
            Long notificationId,
            Long userId) {

        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElse(null);

        if (notification == null) {
            throw new RuntimeException(
                    "Notification not found");
        }

        // Ownership check
        if (notification.getUser() == null
                || !notification.getUser()
                        .getId()
                        .equals(userId)) {

            throw new RuntimeException(
                    "You can only update your own notification");
        }

        notification.setRead(true);

        return notificationRepository.save(notification);
    }

    // =========================
    // DELETE NOTIFICATION
    // =========================

    public void deleteNotification(
            Long notificationId,
            Long userId) {

        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElse(null);

        if (notification == null) {
            throw new RuntimeException(
                    "Notification not found");
        }

        // Ownership check
        if (notification.getUser() == null
                || !notification.getUser()
                        .getId()
                        .equals(userId)) {

            throw new RuntimeException(
                    "You can only delete your own notification");
        }

        notificationRepository.delete(notification);
    }
}