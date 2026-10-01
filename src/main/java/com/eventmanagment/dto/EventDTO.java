package com.eventmanagment.dto;

import com.eventmanagment.entity.Event;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class EventDTO {

    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotBlank(message = "Location is required")
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    private LocalDateTime eventDate;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer availableSeats;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long organizerId;

    public EventDTO() {
    }

    public EventDTO(
            Long id,
            String title,
            String description,
            String location,
            LocalDateTime eventDate,
            Integer capacity,
            Integer availableSeats,
            Long organizerId) {

        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.eventDate = eventDate;
        this.capacity = capacity;
        this.availableSeats = availableSeats;
        this.organizerId = organizerId;
    }

    public static EventDTO fromEntity(Event event) {
        return new EventDTO(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getEventDate(),
                event.getCapacity(),
                event.getAvailableSeats(),
                event.getOrganizer() != null
                        ? event.getOrganizer().getId()
                        : null
        );
    }

    public Event toEntity() {
        Event event = new Event();

        event.setId(this.id);
        event.setTitle(this.title);
        event.setDescription(this.description);
        event.setLocation(this.location);
        event.setEventDate(this.eventDate);

        if (this.capacity != null) {
            event.setCapacity(this.capacity);
        }

        return event;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
    }

    public Long getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(Long organizerId) {
        this.organizerId = organizerId;
    }
}