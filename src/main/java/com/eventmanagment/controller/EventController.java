package com.eventmanagment.controller;

import com.eventmanagment.entity.Event;
import com.eventmanagment.entity.EventStatus;
import com.eventmanagment.entity.User;
import com.eventmanagment.repository.UserRepository;
import com.eventmanagment.service.EventService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final UserRepository userRepository;

    public EventController(
            EventService eventService,
            UserRepository userRepository) {

        this.eventService = eventService;
        this.userRepository = userRepository;
    }

    // =========================
    // GET CURRENT USER ID
    // =========================

    private Long getCurrentUserId(Principal principal) {

        User user = userRepository
                .findByEmail(principal.getName())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return user.getId();
    }

    // =========================
    // CREATE EVENT
    // =========================

    @PostMapping
    public Event createEvent(
            @Valid @RequestBody Event event,
            Principal principal) {

        Long organizerId = getCurrentUserId(principal);

        return eventService.createEvent(event, organizerId);
    }

    // =========================
    // GET ALL EVENTS
    // =========================

    @GetMapping
    public List<Event> getAllEvents() {
        return eventService.getAllEvents();
    }

    // =========================
    // GET EVENTS BY ORGANIZER
    // =========================

    @GetMapping("/organizer")
public List<Event> getMyEvents(Principal principal) {

    Long organizerId = getCurrentUserId(principal);

    return eventService.getEventsByOrganizer(organizerId);
}

    // =========================
    // SEARCH BY TITLE
    // =========================

    @GetMapping("/search")
    public List<Event> searchEvents(
            @RequestParam String title) {

        return eventService.searchEventsByTitle(title);
    }

    // =========================
    // SEARCH BY LOCATION
    // =========================

    @GetMapping("/location")
    public List<Event> searchEventsByLocation(
            @RequestParam String location) {

        return eventService.searchEventsByLocation(location);
    }

    // =========================
    // SEARCH BY DATE
    // =========================

    @GetMapping("/date")
    public List<Event> searchEventsByDate(
            @RequestParam String date) {

        return eventService.searchEventsByDate(date);
    }

    // =========================
    // SEARCH BY STATUS
    // =========================

    @GetMapping("/status/{status}")
    public List<Event> searchEventsByStatus(
            @PathVariable EventStatus status) {

        return eventService.searchEventsByStatus(status);
    }

    // =========================
    // SEARCH BY CATEGORY
    // =========================

    @GetMapping("/category/{category}")
    public List<Event> getEventsByCategory(
            @PathVariable String category) {

        return eventService.getEventsByCategory(category);
    }

    // =========================
    // GET EVENT BY ID
    // =========================

    @GetMapping("/{id}")
    public Event getEventById(
            @PathVariable Long id) {

        return eventService.getEventById(id);
    }

    // =========================
    // UPDATE EVENT
    // =========================

    @PutMapping("/{id}")
    public Event updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody Event event,
            Principal principal) {

        Long organizerId = getCurrentUserId(principal);

        return eventService.updateEvent(
                id,
                event,
                organizerId
        );
    }

    // =========================
    // DELETE EVENT
    // =========================

    @DeleteMapping("/{id}")
    public String deleteEvent(
            @PathVariable Long id,
            Principal principal) {

        Long organizerId = getCurrentUserId(principal);

        eventService.deleteEvent(
                id,
                organizerId
        );

        return "Event deleted successfully";
    }
}