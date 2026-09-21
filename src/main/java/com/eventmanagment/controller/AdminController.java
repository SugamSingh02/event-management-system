package com.eventmanagment.controller;

import com.eventmanagment.entity.Event;
import com.eventmanagment.service.EventService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/admin")
public class AdminController {

    private final EventService eventService;

    public AdminController(EventService eventService) {
        this.eventService = eventService;
    }

    // =========================
    // GET PENDING EVENTS
    // =========================

    @GetMapping("/pending")
    public List<Event> getPendingEvents() {
        return eventService.getPendingEvents();
    }

    // =========================
    // APPROVE EVENT
    // =========================

    @PutMapping("/{id}/approve")
    public Event approveEvent(@PathVariable Long id) {
        return eventService.approveEvent(id);
    }

    // =========================
    // REJECT EVENT
    // =========================

    @PutMapping("/{id}/reject")
    public Event rejectEvent(@PathVariable Long id) {
        return eventService.rejectEvent(id);
    }
}