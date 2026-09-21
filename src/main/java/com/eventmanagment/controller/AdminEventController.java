package com.eventmanagment.controller;

import com.eventmanagment.entity.Event;
import com.eventmanagment.service.EventService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/events")
public class AdminEventController {

    private final EventService eventService;

    public AdminEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/pending")
    public List<Event> getPendingEvents() {
        return eventService.getPendingEvents();
    }

    @PutMapping("/{id}/approve")
    public Event approveEvent(@PathVariable Long id) {
        return eventService.approveEvent(id);
    }

    @PutMapping("/{id}/reject")
    public Event rejectEvent(@PathVariable Long id) {
        return eventService.rejectEvent(id);
    }
    @GetMapping("/stats/approved")
public long getApprovedEventCount() {
    return eventService.getApprovedEventCount();
}

@GetMapping("/stats/pending")
public long getPendingEventCount() {
    return eventService.getPendingEventCount();
}
@GetMapping("/stats/rejected")
public long getRejectedEventCount() {
    return eventService.getRejectedEventCount();
}
@GetMapping("/stats/total")
public long getTotalEventCount() {
    return eventService.getTotalEventCount();
}
}
