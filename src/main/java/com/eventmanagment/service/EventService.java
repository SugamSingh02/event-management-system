package com.eventmanagment.service;

import com.eventmanagment.entity.Event;
import com.eventmanagment.entity.EventStatus;
import com.eventmanagment.entity.User;
import com.eventmanagment.entity.UserRole;
import com.eventmanagment.repository.EventRepository;
import com.eventmanagment.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public EventService(
            EventRepository eventRepository,
            UserRepository userRepository,
            NotificationService notificationService) {

        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // =========================
    // VALIDATE EVENT DATE
    // =========================

    private void validateEventDate(String date) {

        try {
            LocalDate eventDate = LocalDate.parse(date);

            LocalDate today =
                    LocalDate.now(ZoneId.of("Asia/Kolkata"));

            if (eventDate.isBefore(today)) {
                throw new RuntimeException(
                        "Event date cannot be in the past");
            }

        } catch (DateTimeParseException e) {

            throw new RuntimeException(
                    "Invalid date format. Use yyyy-MM-dd");
        }
    }

    // =========================
    // CREATE EVENT
    // =========================

    public Event createEvent(
            Event event,
            Long organizerId) {

        User organizer = userRepository
                .findById(organizerId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Organizer not found"));

        if (organizer.getRole() != UserRole.ORGANIZER) {
            throw new RuntimeException(
                    "Only organizers can create events");
        }

        validateEventDate(event.getDate());

        event.setOrganizer(organizer);

        // Organizer-created event needs admin approval
        event.setStatus(EventStatus.PENDING);

        return eventRepository.save(event);
    }

    // =========================
    // GET ALL EVENTS
    // =========================

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    // =========================
    // GET EVENT BY ID
    // =========================

    public Event getEventById(Long id) {
        return eventRepository
                .findById(id)
                .orElse(null);
    }

    // =========================
    // SEARCH BY CATEGORY
    // =========================

    public List<Event> getEventsByCategory(
            String category) {

        return eventRepository
                .findByCategory(category);
    }

    // =========================
    // SEARCH BY TITLE
    // =========================

    public List<Event> searchEventsByTitle(
            String title) {

        return eventRepository
                .findByTitleContainingIgnoreCase(title);
    }

    // =========================
    // SEARCH BY LOCATION
    // =========================

    public List<Event> searchEventsByLocation(
            String location) {

        return eventRepository
                .findByLocationContainingIgnoreCase(location);
    }

    // =========================
    // SEARCH BY DATE
    // =========================

    public List<Event> searchEventsByDate(
            String date) {

        return eventRepository
                .findByDate(date);
    }

    // =========================
    // SEARCH BY STATUS
    // =========================

    public List<Event> searchEventsByStatus(
            EventStatus status) {

        return eventRepository
                .findByStatus(status);
    }

    // =========================
    // GET EVENTS BY ORGANIZER
    // =========================

    public List<Event> getEventsByOrganizer(
            Long organizerId) {

        return eventRepository
                .findByOrganizerId(organizerId);
    }

    // =========================
    // CHECK EVENT OWNER
    // =========================

    private boolean isEventOwner(
            Event event,
            Long organizerId) {

        return event.getOrganizer() != null
                && event.getOrganizer()
                        .getId()
                        .equals(organizerId);
    }

    // =========================
    // UPDATE EVENT
    // =========================

    public Event updateEvent(
            Long id,
            Event event,
            Long organizerId) {

        Event existingEvent =
                eventRepository
                        .findById(id)
                        .orElse(null);

        if (existingEvent == null) {
            return null;
        }

        if (!isEventOwner(
                existingEvent,
                organizerId)) {

            throw new RuntimeException(
                    "You can only update your own events");
        }

        validateEventDate(event.getDate());

        existingEvent.setTitle(event.getTitle());
        existingEvent.setDescription(event.getDescription());
        existingEvent.setDate(event.getDate());
        existingEvent.setTime(event.getTime());
        existingEvent.setLocation(event.getLocation());
        existingEvent.setCategory(event.getCategory());
        existingEvent.setCapacity(event.getCapacity());
        existingEvent.setTicketPrice(event.getTicketPrice());

        // Any organizer update requires re-approval
        existingEvent.setStatus(EventStatus.PENDING);

        return eventRepository.save(existingEvent);
    }

    // =========================
    // DELETE EVENT
    // =========================

    public void deleteEvent(
            Long id,
            Long organizerId) {

        Event existingEvent =
                eventRepository
                        .findById(id)
                        .orElse(null);

        if (existingEvent == null) {
            throw new RuntimeException(
                    "Event not found");
        }

        if (!isEventOwner(
                existingEvent,
                organizerId)) {

            throw new RuntimeException(
                    "You can only delete your own events");
        }

        eventRepository.deleteById(id);
    }

    // =========================
    // ADMIN - PENDING EVENTS
    // =========================

    public List<Event> getPendingEvents() {

        return eventRepository
                .findByStatus(EventStatus.PENDING);
    }

    // =========================
    // ADMIN - APPROVE EVENT
    // =========================

    public Event approveEvent(Long id) {

        Event event =
                eventRepository
                        .findById(id)
                        .orElse(null);

        if (event == null) {
            return null;
        }

        // Only pending events can be approved
        if (event.getStatus()
                != EventStatus.PENDING) {

            throw new RuntimeException(
                    "Only pending events can be approved");
        }

        event.setStatus(EventStatus.APPROVED);

        Event savedEvent =
                eventRepository.save(event);

        // Notify organizer
        if (savedEvent.getOrganizer() != null) {

            notificationService.createNotification(
                    savedEvent.getOrganizer().getId(),
                    "Your event \"" +
                            savedEvent.getTitle() +
                            "\" has been approved.",
                    "EVENT_APPROVED"
            );
        }

        return savedEvent;
    }

    // =========================
    // ADMIN - REJECT EVENT
    // =========================

    public Event rejectEvent(Long id) {

        Event event =
                eventRepository
                        .findById(id)
                        .orElse(null);

        if (event == null) {
            return null;
        }

        // Only pending events can be rejected
        if (event.getStatus()
                != EventStatus.PENDING) {

            throw new RuntimeException(
                    "Only pending events can be rejected");
        }

        event.setStatus(EventStatus.REJECTED);

        Event savedEvent =
                eventRepository.save(event);

        // Notify organizer
        if (savedEvent.getOrganizer() != null) {

            notificationService.createNotification(
                    savedEvent.getOrganizer().getId(),
                    "Your event \"" +
                            savedEvent.getTitle() +
                            "\" has been rejected.",
                    "EVENT_REJECTED"
            );
        }

        return savedEvent;
    }

    // =========================
    // ADMIN STATISTICS
    // =========================

    public long getApprovedEventCount() {

        return eventRepository
                .countByStatus(EventStatus.APPROVED);
    }

    public long getPendingEventCount() {

        return eventRepository
                .countByStatus(EventStatus.PENDING);
    }

    public long getRejectedEventCount() {

        return eventRepository
                .countByStatus(EventStatus.REJECTED);
    }

    public long getTotalEventCount() {

        return eventRepository.count();
    }

    // =========================
    // ORGANIZER STATISTICS
    // =========================

    public long getOrganizerApprovedEventCount(
            Long organizerId) {

        return eventRepository
                .countByOrganizerIdAndStatus(
                        organizerId,
                        EventStatus.APPROVED
                );
    }

    public long getOrganizerPendingEventCount(
            Long organizerId) {

        return eventRepository
                .countByOrganizerIdAndStatus(
                        organizerId,
                        EventStatus.PENDING
                );
    }

    public long getOrganizerRejectedEventCount(
            Long organizerId) {

        return eventRepository
                .countByOrganizerIdAndStatus(
                        organizerId,
                        EventStatus.REJECTED
                );
    }

    public long getOrganizerTotalEventCount(
            Long organizerId) {

        return eventRepository
                .countByOrganizerId(organizerId);
    }
}