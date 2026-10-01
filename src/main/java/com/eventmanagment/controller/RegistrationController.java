package com.eventmanagment.controller;

import com.eventmanagment.dto.RegistrationDTO;
import com.eventmanagment.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping
    public List<RegistrationDTO> getAllRegistrations() {
        return registrationService.getAllRegistrations();
    }

    @GetMapping("/event/{eventId}")
    public List<RegistrationDTO> getRegistrationsByEvent(
            @PathVariable Long eventId) {

        return registrationService.getRegistrationsByEventId(eventId);
    }

    @GetMapping("/user/{userId}")
    public List<RegistrationDTO> getRegistrationsByUser(
            @PathVariable Long userId) {

        return registrationService.getRegistrationsByUserId(userId);
    }

    @PostMapping
    public ResponseEntity<RegistrationDTO> createRegistration(
            @Valid @RequestBody RegistrationDTO dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(registrationService.createRegistration(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelRegistration(
            @PathVariable Long id) {

        registrationService.cancelRegistration(id);

        return ResponseEntity.noContent().build();
    }
}