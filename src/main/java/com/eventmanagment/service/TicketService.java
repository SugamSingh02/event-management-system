package com.eventmanagment.service;

import com.eventmanagment.entity.Event;
import com.eventmanagment.entity.EventStatus;
import com.eventmanagment.entity.Registration;
import com.eventmanagment.entity.RegistrationStatus;
import com.eventmanagment.entity.Ticket;
import com.eventmanagment.entity.TicketStatus;
import com.eventmanagment.repository.RegistrationRepository;
import com.eventmanagment.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final RegistrationRepository registrationRepository;

    public TicketService(
            TicketRepository ticketRepository,
            RegistrationRepository registrationRepository) {

        this.ticketRepository = ticketRepository;
        this.registrationRepository = registrationRepository;
    }

    // =========================
    // CHECK TICKET OWNER
    // =========================

    private boolean isTicketOwner(
            Ticket ticket,
            Long userId) {

        return ticket.getRegistration() != null
                && ticket.getRegistration().getUser() != null
                && ticket.getRegistration()
                        .getUser()
                        .getId()
                        .equals(userId);
    }

    // =========================
    // CREATE TICKET
    // =========================

    public Ticket createTicket(
            Long registrationId,
            Long userId) {

        Registration registration =
                registrationRepository
                        .findById(registrationId)
                        .orElse(null);

        if (registration == null) {
            throw new RuntimeException(
                    "Registration not found");
        }

        // Only registration owner can create ticket
        if (registration.getUser() == null
                || !registration.getUser()
                        .getId()
                        .equals(userId)) {

            throw new RuntimeException(
                    "You can only create a ticket for your own registration");
        }

        // Registration must be active
        if (registration.getStatus()
                != RegistrationStatus.REGISTERED) {

            throw new RuntimeException(
                    "Ticket can only be created for an active registration");
        }

        Event event = registration.getEvent();

        if (event == null) {
            throw new RuntimeException(
                    "Event not found");
        }

        // Event must be approved
        if (event.getStatus()
                != EventStatus.APPROVED) {

            throw new RuntimeException(
                    "Ticket can only be created for an approved event");
        }

        // Check duplicate ticket
        if (ticketRepository
                .existsByRegistrationId(registrationId)) {

            throw new RuntimeException(
                    "Ticket already exists for this registration");
        }

        Ticket ticket = new Ticket();

        ticket.setRegistration(registration);
        ticket.setPrice(event.getTicketPrice());
        ticket.setStatus(TicketStatus.ACTIVE);

        return ticketRepository.save(ticket);
    }

    // =========================
    // GET ALL TICKETS
    // =========================

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    // =========================
    // GET TICKET BY ID
    // =========================

    public Ticket getTicketById(Long id) {
        return ticketRepository
                .findById(id)
                .orElse(null);
    }

    // =========================
    // GET TICKET BY REGISTRATION
    // =========================

    public Ticket getTicketByRegistration(
            Long registrationId) {

        return ticketRepository
                .findByRegistrationId(registrationId)
                .orElse(null);
    }

    // =========================
    // GET TICKETS BY STATUS
    // =========================

    public List<Ticket> getTicketsByStatus(
            TicketStatus status) {

        return ticketRepository.findByStatus(status);
    }

    // =========================
    // GET MY TICKETS
    // =========================

    public List<Ticket> getTicketsByUserId(Long userId) {

        List<Registration> registrations =
                registrationRepository.findByUserId(userId);

        List<Ticket> tickets = new ArrayList<>();

        for (Registration registration : registrations) {

            Ticket ticket = ticketRepository
                    .findByRegistrationId(
                            registration.getId())
                    .orElse(null);

            if (ticket != null) {
                tickets.add(ticket);
            }
        }

        return tickets;
    }

    // =========================
    // CANCEL TICKET
    // =========================

    public Ticket cancelTicket(
            Long id,
            Long userId) {

        Ticket ticket =
                ticketRepository
                        .findById(id)
                        .orElse(null);

        if (ticket == null) {
            return null;
        }

        // Ownership check
        if (!isTicketOwner(ticket, userId)) {

            throw new RuntimeException(
                    "You can only cancel your own ticket");
        }

        ticket.setStatus(
                TicketStatus.CANCELLED);

        return ticketRepository.save(ticket);
    }

    // =========================
    // DELETE TICKET
    // =========================

    public void deleteTicket(
            Long id,
            Long userId) {

        Ticket ticket =
                ticketRepository
                        .findById(id)
                        .orElse(null);

        if (ticket == null) {
            throw new RuntimeException(
                    "Ticket not found");
        }

        // Ownership check
        if (!isTicketOwner(ticket, userId)) {

            throw new RuntimeException(
                    "You can only delete your own ticket");
        }

        ticketRepository.delete(ticket);
    }
}