package com.eventmanagment.service;

import com.eventmanagment.dto.EventDTO;
import com.eventmanagment.entity.Event;
import com.eventmanagment.entity.User;
import com.eventmanagment.exception.ResourceNotFoundException;
import com.eventmanagment.repository.EventRepository;
import com.eventmanagment.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventService(
            EventRepository eventRepository,
            UserRepository userRepository) {

        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    public EventDTO createEvent(EventDTO dto) {

        User currentUser = getAuthenticatedUser();

        if (!currentUser.getRole().equals("ADMIN")
                && !currentUser.getRole().equals("ORGANIZER")) {

            throw new AccessDeniedException(
                    "Only administrators and organizers can create events"
            );
        }

        Event event = new Event();

        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setLocation(dto.getLocation());
        event.setEventDate(dto.getEventDate());
        event.setCapacity(dto.getCapacity());

        // Available seats are calculated by the server.
        event.setAvailableSeats(dto.getCapacity());

        // Organizer is always taken from the authenticated user.
        event.setOrganizer(currentUser);

        Event saved = eventRepository.save(event);

        return EventDTO.fromEntity(saved);
    }

    public List<EventDTO> getAllEvents() {

        return eventRepository.findAll()
                .stream()
                .map(EventDTO::fromEntity)
                .toList();
    }

    public EventDTO getEventById(Long id) {

        Event event = eventRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Event not found with id: " + id
                        ));

        return EventDTO.fromEntity(event);
    }

    @Transactional
    public EventDTO updateEvent(Long id, EventDTO dto) {

        Event existing = eventRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Event not found with id: " + id
                        ));

        User currentUser = getAuthenticatedUser();

        checkEventOwnership(existing, currentUser);

        existing.setTitle(dto.getTitle());
        existing.setDescription(dto.getDescription());
        existing.setLocation(dto.getLocation());
        existing.setEventDate(dto.getEventDate());

        int newCapacity = dto.getCapacity();

        int alreadyBooked =
                existing.getCapacity()
                        - existing.getAvailableSeats();

        if (newCapacity < alreadyBooked) {
            throw new IllegalArgumentException(
                    "Capacity cannot be less than the number of existing registrations"
            );
        }

        existing.setCapacity(newCapacity);

        existing.setAvailableSeats(
                newCapacity - alreadyBooked
        );

        Event updated = eventRepository.save(existing);

        return EventDTO.fromEntity(updated);
    }

    @Transactional
    public void deleteEvent(Long id) {

        Event event = eventRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Event not found with id: " + id
                        ));

        User currentUser = getAuthenticatedUser();

        checkEventOwnership(event, currentUser);

        eventRepository.delete(event);
    }

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

    private void checkEventOwnership(
            Event event,
            User currentUser) {

        // Admin can manage every event.
        if (currentUser.getRole().equals("ADMIN")) {
            return;
        }

        // Only organizers can manage events besides admins.
        if (!currentUser.getRole().equals("ORGANIZER")) {
            throw new AccessDeniedException(
                    "You do not have permission to manage events"
            );
        }

        // Organizer can manage only their own events.
        if (event.getOrganizer() == null
                || !event.getOrganizer()
                        .getId()
                        .equals(currentUser.getId())) {

            throw new AccessDeniedException(
                    "You can only manage your own events"
            );
        }
    }
}