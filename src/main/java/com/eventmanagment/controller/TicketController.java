package com.eventmanagment.controller;

import com.eventmanagment.entity.Ticket;
import com.eventmanagment.entity.TicketStatus;
import com.eventmanagment.entity.User;
import com.eventmanagment.entity.UserRole;
import com.eventmanagment.repository.UserRepository;
import com.eventmanagment.service.TicketService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final UserRepository userRepository;

    public TicketController(
            TicketService ticketService,
            UserRepository userRepository) {

        this.ticketService = ticketService;
        this.userRepository = userRepository;
    }

    // =========================
    // GET CURRENT USER
    // =========================

    private User getCurrentUser(Principal principal) {

        return userRepository
                .findByEmail(principal.getName())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }

    // =========================
    // GET CURRENT USER ID
    // =========================

    private Long getCurrentUserId(
            Principal principal) {

        return getCurrentUser(principal).getId();
    }

    // =========================
    // CREATE TICKET
    // =========================

    @PostMapping
    public Ticket createTicket(
            @RequestParam Long registrationId,
            Principal principal) {

        Long userId =
                getCurrentUserId(principal);

        return ticketService.createTicket(
                registrationId,
                userId
        );
    }

    // =========================
    // GET ALL TICKETS
    // ADMIN ONLY
    // =========================

    @GetMapping
    public List<Ticket> getAllTickets(
            Principal principal) {

        User user = getCurrentUser(principal);

        if (user.getRole() != UserRole.ADMIN) {

            throw new RuntimeException(
                    "Only admin can view all tickets");
        }

        return ticketService.getAllTickets();
    }

    // =========================
    // GET MY TICKETS
    // =========================

    @GetMapping("/my")
    public List<Ticket> getMyTickets(
            Principal principal) {

        Long userId =
                getCurrentUserId(principal);

        return ticketService.getTicketsByUserId(
                userId
        );
    }

    // =========================
    // GET TICKET BY ID
    // ADMIN / OWNER
    // =========================

    @GetMapping("/{id}")
    public Ticket getTicketById(
            @PathVariable Long id,
            Principal principal) {

        User currentUser =
                getCurrentUser(principal);

        Ticket ticket =
                ticketService.getTicketById(id);

        if (ticket == null) {
            return null;
        }

        // Admin can view any ticket
        if (currentUser.getRole()
                == UserRole.ADMIN) {

            return ticket;
        }

        // Owner can view own ticket
        if (ticket.getRegistration() != null
                && ticket.getRegistration().getUser() != null
                && ticket.getRegistration()
                        .getUser()
                        .getId()
                        .equals(currentUser.getId())) {

            return ticket;
        }

        throw new RuntimeException(
                "You can only view your own ticket");
    }

    // =========================
    // GET TICKET BY REGISTRATION
    // ADMIN / OWNER
    // =========================

    @GetMapping("/registration/{registrationId}")
    public Ticket getTicketByRegistration(
            @PathVariable Long registrationId,
            Principal principal) {

        User currentUser =
                getCurrentUser(principal);

        Ticket ticket =
                ticketService.getTicketByRegistration(
                        registrationId);

        if (ticket == null) {
            return null;
        }

        // Admin can view any ticket
        if (currentUser.getRole()
                == UserRole.ADMIN) {

            return ticket;
        }

        // Owner can view own ticket
        if (ticket.getRegistration() != null
                && ticket.getRegistration().getUser() != null
                && ticket.getRegistration()
                        .getUser()
                        .getId()
                        .equals(currentUser.getId())) {

            return ticket;
        }

        throw new RuntimeException(
                "You can only view your own ticket");
    }

    // =========================
    // GET TICKETS BY STATUS
    // ADMIN ONLY
    // =========================

    @GetMapping("/status/{status}")
    public List<Ticket> getTicketsByStatus(
            @PathVariable TicketStatus status,
            Principal principal) {

        User user = getCurrentUser(principal);

        if (user.getRole() != UserRole.ADMIN) {

            throw new RuntimeException(
                    "Only admin can view tickets by status");
        }

        return ticketService.getTicketsByStatus(
                status);
    }

    // =========================
    // CANCEL TICKET
    // =========================

    @PutMapping("/{id}/cancel")
    public Ticket cancelTicket(
            @PathVariable Long id,
            Principal principal) {

        Long userId =
                getCurrentUserId(principal);

        return ticketService.cancelTicket(
                id,
                userId
        );
    }

    // =========================
    // DELETE TICKET
    // =========================

    @DeleteMapping("/{id}")
    public String deleteTicket(
            @PathVariable Long id,
            Principal principal) {

        Long userId =
                getCurrentUserId(principal);

        ticketService.deleteTicket(
                id,
                userId
        );

        return "Ticket deleted successfully";
    }
}