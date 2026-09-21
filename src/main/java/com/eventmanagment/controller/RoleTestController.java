package com.eventmanagment.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RoleTestController {

    @GetMapping("/admin/test")
    public String adminTest() {
        return "Admin access granted";
    }

    @GetMapping("/organizer/test")
    public String organizerTest() {
        return "Organizer access granted";
    }

    @GetMapping("/attendee/test")
    public String attendeeTest() {
        return "Attendee access granted";
    }
}