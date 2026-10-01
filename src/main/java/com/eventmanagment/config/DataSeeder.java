package com.eventmanagment.config;

import com.eventmanagment.entity.Event;
import com.eventmanagment.entity.Registration;
import com.eventmanagment.entity.User;
import com.eventmanagment.repository.EventRepository;
import com.eventmanagment.repository.RegistrationRepository;
import com.eventmanagment.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            EventRepository eventRepository,
            UserRepository userRepository,
            RegistrationRepository registrationRepository,
            PasswordEncoder passwordEncoder) {

        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if (eventRepository.count() > 0 || userRepository.count() > 0) {
            return;
        }

        // -----------------------------
        // Create users first
        // -----------------------------

        User admin = new User(
                "Admin User",
                "admin@event.com",
                "+91 90000 00001",
                passwordEncoder.encode("admin123"),
                "ADMIN"
        );

        User organizer = new User(
                "Event Organizer",
                "organizer@event.com",
                "+91 90000 00002",
                passwordEncoder.encode("organizer123"),
                "ORGANIZER"
        );

        User attendee = new User(
                "Demo Attendee",
                "attendee@event.com",
                "+91 90000 00003",
                passwordEncoder.encode("attendee123"),
                "ATTENDEE"
        );

        User speaker = new User(
                "Aisha Khan",
                "aisha@example.com",
                "+91 90000 12345",
                passwordEncoder.encode("aisha123"),
                "ATTENDEE"
        );

        userRepository.save(admin);
        userRepository.save(organizer);
        userRepository.save(attendee);
        userRepository.save(speaker);

        // -----------------------------
        // Create events
        // -----------------------------

        Event event1 = new Event(
                "Spring Boot Meetup",
                "Meet developers and discuss modern Java application design.",
                "Hyderabad",
                LocalDateTime.now().plusDays(10),
                50
        );

        Event event2 = new Event(
                "AI & Automation Workshop",
                "Hands-on workshop focused on automation and AI workflows.",
                "Bengaluru",
                LocalDateTime.now().plusDays(20),
                30
        );

        // Assign organizer ownership
        event1.setOrganizer(organizer);
        event2.setOrganizer(organizer);

        eventRepository.save(event1);
        eventRepository.save(event2);

        // -----------------------------
        // Create demo registration
        // -----------------------------

        Registration registration =
                new Registration(event1, attendee, "CONFIRMED");

        registrationRepository.save(registration);

        event1.setAvailableSeats(
                event1.getAvailableSeats() - 1
        );

        eventRepository.save(event1);
    }
}