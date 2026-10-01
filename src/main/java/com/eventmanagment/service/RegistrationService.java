package com.eventmanagment.service;

import com.eventmanagment.dto.RegistrationDTO;
import com.eventmanagment.entity.Event;
import com.eventmanagment.entity.Registration;
import com.eventmanagment.entity.User;
import com.eventmanagment.exception.ResourceNotFoundException;
import com.eventmanagment.repository.EventRepository;
import com.eventmanagment.repository.RegistrationRepository;
import com.eventmanagment.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public RegistrationService(
            RegistrationRepository registrationRepository,
            EventRepository eventRepository,
            UserRepository userRepository) {

        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    // --------------------------------------------------
    // Create / Re-activate registration
    // --------------------------------------------------

    @Transactional
    public RegistrationDTO createRegistration(RegistrationDTO dto) {

        User currentUser = getAuthenticatedUser();

        if (!currentUser.getRole().equals("ATTENDEE")) {
            throw new AccessDeniedException(
                    "Only attendees can register for events"
            );
        }

        Event event =
                eventRepository.findByIdForUpdate(dto.getEventId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Event not found with id: "
                                                + dto.getEventId()
                                ));

        Long userId = currentUser.getId();

        Registration existing =
                registrationRepository
                        .findByEventIdAndUserId(
                                event.getId(),
                                userId
                        )
                        .orElse(null);

        // Already confirmed means duplicate registration.
        if (existing != null
                && "CONFIRMED".equals(existing.getStatus())) {

            throw new IllegalArgumentException(
                    "User is already registered for this event"
            );
        }

        // No seat is available for a new or re-activated registration.
        if (event.getAvailableSeats() <= 0) {
            throw new IllegalStateException(
                    "No available seats left for this event"
            );
        }

        Registration registration;

        if (existing != null
                && "CANCELLED".equals(existing.getStatus())) {

            // Re-activate the cancelled registration.
            registration = existing;
            registration.setStatus("CONFIRMED");

        } else {

            // Create a new registration.
            registration =
                    new Registration(
                            event,
                            currentUser,
                            "CONFIRMED"
                    );
        }

        Registration saved =
                registrationRepository.save(registration);

        event.setAvailableSeats(
                event.getAvailableSeats() - 1
        );

        eventRepository.save(event);

        return RegistrationDTO.fromEntity(saved);
    }

    // --------------------------------------------------
    // Get all registrations
    // --------------------------------------------------

    @Transactional(readOnly = true)
    public List<RegistrationDTO> getAllRegistrations() {

        User currentUser = getAuthenticatedUser();

        List<Registration> registrations;

        if (currentUser.getRole().equals("ADMIN")) {

            registrations = registrationRepository.findAll();

        } else if (currentUser.getRole().equals("ORGANIZER")) {

            registrations =
                    registrationRepository.findByEventOrganizerId(
                            currentUser.getId()
                    );

        } else {

            registrations =
                    registrationRepository.findByUserId(
                            currentUser.getId()
                    );
        }

        return registrations.stream()
                .map(RegistrationDTO::fromEntity)
                .toList();
    }

    // --------------------------------------------------
    // Get registrations by event
    // --------------------------------------------------

    @Transactional(readOnly = true)
    public List<RegistrationDTO> getRegistrationsByEventId(
            Long eventId) {

        User currentUser = getAuthenticatedUser();

        Event event =
                eventRepository.findById(eventId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Event not found with id: " + eventId
                                ));

        if (currentUser.getRole().equals("ADMIN")) {

            // Admin can view any event registrations.

        } else if (currentUser.getRole().equals("ORGANIZER")) {

            if (event.getOrganizer() == null
                    || !event.getOrganizer()
                            .getId()
                            .equals(currentUser.getId())) {

                throw new AccessDeniedException(
                        "You can only view registrations for your own events"
                );
            }

        } else {

            throw new AccessDeniedException(
                    "Attendees cannot view all registrations for an event"
            );
        }

        return registrationRepository
                .findByEventId(eventId)
                .stream()
                .map(RegistrationDTO::fromEntity)
                .toList();
    }

    // --------------------------------------------------
    // Get registrations by user
    // --------------------------------------------------

    @Transactional(readOnly = true)
    public List<RegistrationDTO> getRegistrationsByUserId(
            Long userId) {

        User currentUser = getAuthenticatedUser();

        if (currentUser.getRole().equals("ADMIN")) {

            // Admin can view any user's registrations.

        } else if (!currentUser.getId().equals(userId)) {

            throw new AccessDeniedException(
                    "You can only view your own registrations"
            );
        }

        return registrationRepository
                .findByUserId(userId)
                .stream()
                .map(RegistrationDTO::fromEntity)
                .toList();
    }

    // --------------------------------------------------
    // Cancel registration
    // --------------------------------------------------

    @Transactional
    public void cancelRegistration(Long id) {

        User currentUser = getAuthenticatedUser();

        Registration registration =
                registrationRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Registration not found with id: " + id
                                ));

        Event registrationEvent = registration.getEvent();

        Event event = null;

        if (registrationEvent != null) {

            Long eventId = registrationEvent.getId();

            event =
                    eventRepository.findByIdForUpdate(eventId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Event not found with id: "
                                                    + eventId
                                    ));
        }

        boolean isAdmin =
                currentUser.getRole().equals("ADMIN");

        boolean isOrganizer =
                currentUser.getRole().equals("ORGANIZER")
                        && event != null
                        && event.getOrganizer() != null
                        && event.getOrganizer()
                                .getId()
                                .equals(currentUser.getId());

        boolean isOwner =
                registration.getUser() != null
                        && registration.getUser()
                                .getId()
                                .equals(currentUser.getId());

        if (!isAdmin && !isOrganizer && !isOwner) {
            throw new AccessDeniedException(
                    "You do not have permission to cancel this registration"
            );
        }

        if ("CANCELLED".equals(registration.getStatus())) {
            throw new IllegalStateException(
                    "Registration is already cancelled"
            );
        }

        // Restore the seat only when cancelling a confirmed registration.
        if (event != null
                && "CONFIRMED".equals(registration.getStatus())) {

            event.setAvailableSeats(
                    event.getAvailableSeats() + 1
            );

            eventRepository.save(event);
        }

        // Keep the registration record for history.
        registration.setStatus("CANCELLED");

        registrationRepository.save(registration);
    }

    // --------------------------------------------------
    // Get authenticated user
    // --------------------------------------------------

    private User getAuthenticatedUser() {

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
}