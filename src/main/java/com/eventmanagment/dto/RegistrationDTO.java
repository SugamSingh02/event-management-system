package com.eventmanagment.dto;

import com.eventmanagment.entity.Registration;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class RegistrationDTO {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long userId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String attendeeName;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String attendeeEmail;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String status;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime registeredAt;

    public RegistrationDTO() {
    }

    public RegistrationDTO(
            Long id,
            Long eventId,
            Long userId,
            String attendeeName,
            String attendeeEmail,
            String status,
            LocalDateTime registeredAt) {

        this.id = id;
        this.eventId = eventId;
        this.userId = userId;
        this.attendeeName = attendeeName;
        this.attendeeEmail = attendeeEmail;
        this.status = status;
        this.registeredAt = registeredAt;
    }

    public static RegistrationDTO fromEntity(Registration registration) {
        return new RegistrationDTO(
                registration.getId(),
                registration.getEvent().getId(),
                registration.getUser().getId(),
                registration.getUser().getName(),
                registration.getUser().getEmail(),
                registration.getStatus(),
                registration.getRegisteredAt()
        );
    }

    public Registration toEntity() {
        Registration registration = new Registration();

        registration.setId(this.id);
        registration.setStatus(this.status);

        return registration;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAttendeeName() {
        return attendeeName;
    }

    public void setAttendeeName(String attendeeName) {
        this.attendeeName = attendeeName;
    }

    public String getAttendeeEmail() {
        return attendeeEmail;
    }

    public void setAttendeeEmail(String attendeeEmail) {
        this.attendeeEmail = attendeeEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }
}