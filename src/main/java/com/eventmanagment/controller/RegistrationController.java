package com.eventmanagment.controller;

import com.eventmanagment.entity.Event;
import com.eventmanagment.entity.Registration;
import com.eventmanagment.entity.User;
import com.eventmanagment.entity.UserRole;
import com.eventmanagment.repository.EventRepository;
import com.eventmanagment.repository.UserRepository;
import com.eventmanagment.service.RegistrationService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public RegistrationController(
            RegistrationService registrationService,
            UserRepository userRepository,
            EventRepository eventRepository) {

        this.registrationService = registrationService;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
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
    // GET CURRENT USER ID
    // =========================

    private Long getCurrentUserId(Principal principal) {
        return getCurrentUser(principal).getId();
    }

    // =========================
    // REGISTER FOR EVENT
    // =========================

    @PostMapping
    public Registration registerUser(
            @RequestParam Long eventId,
            Principal principal) {

        Long userId = getCurrentUserId(principal);

        return registrationService.registerUser(
                eventId,
                userId
        );
    }

    // =========================
    // GET ALL REGISTRATIONS
    // ADMIN ONLY
    // =========================

    @GetMapping
    public List<Registration> getAllRegistrations(
            Principal principal) {

        User user = getCurrentUser(principal);

        if (user.getRole() != UserRole.ADMIN) {
            throw new RuntimeException(
                    "Only admin can view all registrations");
        }

        return registrationService.getAllRegistrations();
    }

    // =========================
    // GET REGISTRATION BY ID
    // ADMIN / OWNER / EVENT ORGANIZER
    // =========================

    @GetMapping("/{id}")
    public Registration getRegistrationById(
            @PathVariable Long id,
            Principal principal) {

        User currentUser = getCurrentUser(principal);

        Registration registration =
                registrationService.getRegistrationById(id);

        if (registration == null) {
            return null;
        }

        // Admin can view any registration
        if (currentUser.getRole() == UserRole.ADMIN) {
            return registration;
        }

        // Registration owner can view it
        if (registration.getUser() != null
                && registration.getUser().getId()
                .equals(currentUser.getId())) {

            return registration;
        }

        // Event organizer can view registrations
        Event event = registration.getEvent();

        if (currentUser.getRole() == UserRole.ORGANIZER
                && event != null
                && event.getOrganizer() != null
                && event.getOrganizer().getId()
                .equals(currentUser.getId())) {

            return registration;
        }

        throw new RuntimeException(
                "You can only view your own registration");
    }

    // =========================
    // GET MY REGISTRATIONS
    // =========================

    @GetMapping("/my")
    public List<Registration> getMyRegistrations(
            Principal principal) {

        Long userId = getCurrentUserId(principal);

        return registrationService.getRegistrationsByUser(
                userId
        );
    }

    // =========================
    // GET REGISTRATIONS BY EVENT
    // ADMIN / EVENT ORGANIZER
    // =========================

    @GetMapping("/event/{eventId}")
    public List<Registration> getRegistrationsByEvent(
            @PathVariable Long eventId,
            Principal principal) {

        User currentUser = getCurrentUser(principal);

        // Admin can view any event registrations
        if (currentUser.getRole() == UserRole.ADMIN) {

            return registrationService.getRegistrationsByEvent(
                    eventId
            );
        }

        // Find event
        Event event = eventRepository
                .findById(eventId)
                .orElseThrow(() ->
                        new RuntimeException("Event not found"));

        // Only event owner can view its registrations
        if (currentUser.getRole() == UserRole.ORGANIZER
                && event.getOrganizer() != null
                && event.getOrganizer().getId()
                .equals(currentUser.getId())) {

            return registrationService.getRegistrationsByEvent(
                    eventId
            );
        }

        throw new RuntimeException(
                "You can only view registrations for your own events");
    }

    // =========================
    // CANCEL REGISTRATION
    // =========================

    @PutMapping("/{id}/cancel")
    public Registration cancelRegistration(
            @PathVariable Long id,
            Principal principal) {

        Long userId = getCurrentUserId(principal);

        return registrationService.cancelRegistration(
                id,
                userId
        );
    }

    // =========================
    // DELETE REGISTRATION
    // =========================

    @DeleteMapping("/{id}")
    public String deleteRegistration(
            @PathVariable Long id,
            Principal principal) {

        Long userId = getCurrentUserId(principal);

        registrationService.deleteRegistration(
                id,
                userId
        );

        return "Registration deleted successfully";
    }
}