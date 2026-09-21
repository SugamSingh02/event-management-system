package com.eventmanagment.controller;

import com.eventmanagment.service.EventService;
import com.eventmanagment.repository.UserRepository;
import com.eventmanagment.entity.User;

import java.security.Principal;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizer/stats")
public class OrganizerEventController {

    private final EventService eventService;
    private final UserRepository userRepository;

    public OrganizerEventController(
            EventService eventService,
            UserRepository userRepository) {

        this.eventService = eventService;
        this.userRepository = userRepository;
    }

    // =========================
    // GET CURRENT ORGANIZER ID
    // =========================

    private Long getCurrentOrganizerId(Principal principal) {

        User user = userRepository
                .findByEmail(principal.getName())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return user.getId();
    }

    // =========================
    // APPROVED EVENTS
    // =========================

    @GetMapping("/approved")
    public long getApprovedEventCount(Principal principal) {

        Long organizerId = getCurrentOrganizerId(principal);

        return eventService.getOrganizerApprovedEventCount(
                organizerId
        );
    }

    // =========================
    // PENDING EVENTS
    // =========================

    @GetMapping("/pending")
    public long getPendingEventCount(Principal principal) {

        Long organizerId = getCurrentOrganizerId(principal);

        return eventService.getOrganizerPendingEventCount(
                organizerId
        );
    }
    @GetMapping("/total")
public long getTotalEventCount(Principal principal) {

    Long organizerId = getCurrentOrganizerId(principal);

    return eventService.getOrganizerTotalEventCount(
            organizerId
    );
}
@GetMapping("/rejected")
public long getRejectedEventCount(Principal principal) {

    Long organizerId = getCurrentOrganizerId(principal);

    return eventService.getOrganizerRejectedEventCount(
            organizerId
    );
}
}