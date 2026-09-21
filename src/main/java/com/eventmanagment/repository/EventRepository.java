package com.eventmanagment.repository;

import com.eventmanagment.entity.Event;
import com.eventmanagment.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    // Search events by category
    List<Event> findByCategory(String category);

    // Search events by title
    List<Event> findByTitleContainingIgnoreCase(String title);

    // Search events by location
    List<Event> findByLocationContainingIgnoreCase(String location);

    // Search events by date
    List<Event> findByDate(String date);

    // Search events by status
    List<Event> findByStatus(EventStatus status);

    // Get events created by a particular organizer
    List<Event> findByOrganizerId(Long organizerId);

    // Count events by status
    long countByStatus(EventStatus status);

    // Count events by organizer
    long countByOrganizerId(Long organizerId);

    long count();

    long countByOrganizerIdAndStatus(
        Long organizerId,
        EventStatus status);

}