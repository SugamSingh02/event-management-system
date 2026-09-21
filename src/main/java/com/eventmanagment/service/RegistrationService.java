package com.eventmanagment.service;

import com.eventmanagment.entity.Event;
import com.eventmanagment.entity.EventStatus;
import com.eventmanagment.entity.Registration;
import com.eventmanagment.entity.RegistrationStatus;
import com.eventmanagment.entity.User;
import com.eventmanagment.repository.EventRepository;
import com.eventmanagment.repository.RegistrationRepository;
import com.eventmanagment.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public RegistrationService(
            RegistrationRepository registrationRepository,
            UserRepository userRepository,
            EventRepository eventRepository) {

        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    // =========================
    // REGISTER USER FOR EVENT
    // =========================

    public Registration registerUser(
            Long eventId,
            Long userId) {

        User user = userRepository
                .findById(userId)
                .orElse(null);

        if (user == null) {
            throw new RuntimeException(
                    "User not found");
        }

        Event event = eventRepository
                .findById(eventId)
                .orElse(null);

        if (event == null) {
            throw new RuntimeException(
                    "Event not found");
        }

        // Only approved events
        if (event.getStatus()
                != EventStatus.APPROVED) {

            throw new RuntimeException(
                    "Registration is allowed only for approved events");
        }

        // =========================
        // CHECK EVENT DATE
        // =========================

        try {

            LocalDate eventDate =
                    LocalDate.parse(event.getDate());

            LocalDate today =
                    LocalDate.now(
                            ZoneId.of("Asia/Kolkata"));

            if (eventDate.isBefore(today)) {

                throw new RuntimeException(
                        "Cannot register for a past event");
            }

        } catch (DateTimeParseException e) {

            throw new RuntimeException(
                    "Invalid event date format. Use yyyy-MM-dd");
        }

        // =========================
        // CHECK DUPLICATE ACTIVE REGISTRATION
        // =========================

        List<Registration> userRegistrations =
                registrationRepository
                        .findByUserId(userId);

        for (Registration registration
                : userRegistrations) {

            if (registration.getEvent() != null
                    && registration.getEvent()
                            .getId()
                            .equals(eventId)
                    && registration.getStatus()
                            == RegistrationStatus.REGISTERED) {

                throw new RuntimeException(
                        "User already registered for this event");
            }
        }

        // =========================
        // CHECK CAPACITY
        // =========================

        List<Registration> eventRegistrations =
                registrationRepository
                        .findByEventId(eventId);

        long activeRegistrations = 0;

        for (Registration registration
                : eventRegistrations) {

            if (registration.getStatus()
                    == RegistrationStatus.REGISTERED) {

                activeRegistrations++;
            }
        }

        if (activeRegistrations
                >= event.getCapacity()) {

            throw new RuntimeException(
                    "Event capacity is full");
        }

        // =========================
        // CREATE REGISTRATION
        // =========================

        Registration registration =
                new Registration();

        registration.setUser(user);
        registration.setEvent(event);
        registration.setStatus(
                RegistrationStatus.REGISTERED);

        return registrationRepository.save(
                registration);
    }

    // =========================
    // GET ALL REGISTRATIONS
    // =========================

    public List<Registration> getAllRegistrations() {

        return registrationRepository.findAll();
    }

    // =========================
    // GET REGISTRATION BY ID
    // =========================

    public Registration getRegistrationById(
            Long id) {

        return registrationRepository
                .findById(id)
                .orElse(null);
    }

    // =========================
    // GET REGISTRATIONS BY USER
    // =========================

    public List<Registration> getRegistrationsByUser(
            Long userId) {

        return registrationRepository
                .findByUserId(userId);
    }

    // =========================
    // GET REGISTRATIONS BY EVENT
    // =========================

    public List<Registration> getRegistrationsByEvent(
            Long eventId) {

        return registrationRepository
                .findByEventId(eventId);
    }

    // =========================
    // CANCEL REGISTRATION
    // =========================

    public Registration cancelRegistration(
            Long id,
            Long userId) {

        Registration registration =
                registrationRepository
                        .findById(id)
                        .orElse(null);

        if (registration == null) {
            return null;
        }

        // Ownership check
        if (registration.getUser() == null
                || !registration.getUser()
                        .getId()
                        .equals(userId)) {

            throw new RuntimeException(
                    "You can only cancel your own registration");
        }

        if (registration.getStatus()
                == RegistrationStatus.CANCELLED) {

            throw new RuntimeException(
                    "Registration is already cancelled");
        }

        registration.setStatus(
                RegistrationStatus.CANCELLED);

        return registrationRepository.save(
                registration);
    }

    // =========================
    // DELETE REGISTRATION
    // =========================

    public void deleteRegistration(
            Long id,
            Long userId) {

        Registration registration =
                registrationRepository
                        .findById(id)
                        .orElse(null);

        if (registration == null) {
            throw new RuntimeException(
                    "Registration not found");
        }

        // Ownership check
        if (registration.getUser() == null
                || !registration.getUser()
                        .getId()
                        .equals(userId)) {

            throw new RuntimeException(
                    "You can only delete your own registration");
        }

        registrationRepository.delete(
                registration);
    }
}