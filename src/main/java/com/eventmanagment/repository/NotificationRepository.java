package com.eventmanagment.repository;

import com.eventmanagment.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    // Get all notifications of a user
    List<Notification> findByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    // Get unread notifications of a user
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(
            Long userId
    );

    // Count unread notifications
    long countByUserIdAndReadFalse(Long userId);
}