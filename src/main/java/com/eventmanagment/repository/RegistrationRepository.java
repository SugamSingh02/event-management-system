package com.eventmanagment.repository;

import com.eventmanagment.entity.Registration;
import com.eventmanagment.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistrationRepository
        extends JpaRepository<Registration, Long> {

    // Find registrations by user
    List<Registration> findByUserId(Long userId);

    // Find registrations by event
    List<Registration> findByEventId(Long eventId);

    // Find registrations by status
    List<Registration> findByStatus(RegistrationStatus status);

    // Check if user already registered for event
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
}