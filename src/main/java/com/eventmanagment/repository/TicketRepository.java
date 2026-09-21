package com.eventmanagment.repository;

import com.eventmanagment.entity.Ticket;
import com.eventmanagment.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Find ticket by registration
    Optional<Ticket> findByRegistrationId(Long registrationId);

    // Find tickets by status
    List<Ticket> findByStatus(TicketStatus status);

    // Check if ticket already exists for registration
    boolean existsByRegistrationId(Long registrationId);
}