package com.eventmanagment;

import com.eventmanagment.dto.UserDTO;
import com.eventmanagment.entity.User;
import com.eventmanagment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.json.JsonMapper;

import java.net.HttpURLConnection;

import static org.assertj.core.api.Assertions.assertThat;

class UserApiTests {

    @Test
    void roleBasedLoginShouldFindUserByEmailAndRole() {

        try (ConfigurableApplicationContext context =
                     SpringApplication.run(
                             EventManagementSystemApplication.class,
                             "--server.port=0")) {

            UserRepository userRepository =
                    context.getBean(UserRepository.class);

            assertThat(
                    userRepository.findByEmailAndRole(
                            "admin@event.com",
                            "ADMIN"
                    )
            ).isPresent();

            assertThat(
                    userRepository.findByEmailAndRole(
                            "organizer@event.com",
                            "ORGANIZER"
                    )
            ).isPresent();

            assertThat(
                    userRepository.findByEmailAndRole(
                            "attendee@event.com",
                            "ATTENDEE"
                    )
            ).isPresent();
        }
    }

    @Test
    void userRegistrationShouldCreateAttendeeAndRejectInvalidData()
            throws Exception {

        try (ConfigurableApplicationContext context =
                     SpringApplication.run(
                             EventManagementSystemApplication.class,
                             "--server.port=0")) {

            String port =
                    context.getEnvironment()
                            .getProperty("local.server.port");

            assertThat(port).isNotBlank();

            UserRepository userRepository =
                    context.getBean(UserRepository.class);

            PasswordEncoder passwordEncoder =
                    context.getBean(PasswordEncoder.class);

            JsonMapper objectMapper =
                    context.getBean(JsonMapper.class);

            // -------------------------------------------------
            // 1. Register a new user
            // -------------------------------------------------

            String uniqueEmail =
                    "test.attendee."
                            + System.nanoTime()
                            + "@example.com";

            HttpURLConnection registrationConnection =
                    TestHttpHelper.openConnection(
                            port,
                            "/api/users/register",
                            "POST"
                    );

            TestHttpHelper.setJsonRequest(
                    registrationConnection,
                    null
            );

            String registrationPayload = """
                    {
                        "name": "Test Attendee",
                        "email": "%s",
                        "phone": "+91 90000 00099",
                        "password": "test123",
                        "role": "ADMIN"
                    }
                    """.formatted(uniqueEmail);

            TestHttpHelper.writeBody(
                    registrationConnection,
                    registrationPayload
            );

            int registrationStatus =
                    registrationConnection.getResponseCode();

            String registrationResponse =
                    TestHttpHelper.readResponseBody(
                            registrationConnection
                    );

            assertThat(registrationStatus)
                    .withFailMessage(
                            "User registration failed. HTTP %s, response: %s",
                            registrationStatus,
                            registrationResponse
                    )
                    .isEqualTo(201);

            UserDTO createdUser =
                    objectMapper.readValue(
                            registrationResponse,
                            UserDTO.class
                    );

            assertThat(createdUser.getEmail())
                    .isEqualTo(uniqueEmail);

            assertThat(createdUser.getRole())
                    .isEqualTo("ATTENDEE");

            registrationConnection.disconnect();

            // -------------------------------------------------
            // 2. Verify database record and password hashing
            // -------------------------------------------------

            User savedUser =
                    userRepository.findByEmail(uniqueEmail)
                            .orElseThrow();

            assertThat(savedUser.getRole())
                    .isEqualTo("ATTENDEE");

            assertThat(savedUser.getPassword())
                    .isNotEqualTo("test123");

            assertThat(
                    passwordEncoder.matches(
                            "test123",
                            savedUser.getPassword()
                    )
            ).isTrue();

            // -------------------------------------------------
            // 3. Invalid registration data must be rejected
            // -------------------------------------------------

            HttpURLConnection invalidConnection =
                    TestHttpHelper.openConnection(
                            port,
                            "/api/users/register",
                            "POST"
                    );

            TestHttpHelper.setJsonRequest(
                    invalidConnection,
                    null
            );

            String invalidPayload = """
                    {
                        "name": "",
                        "email": "not-an-email",
                        "phone": "",
                        "password": "123"
                    }
                    """;

            TestHttpHelper.writeBody(
                    invalidConnection,
                    invalidPayload
            );

            int invalidStatus =
                    invalidConnection.getResponseCode();

            String invalidResponse =
                    TestHttpHelper.readResponseBody(
                            invalidConnection
                    );

            assertThat(invalidStatus)
                    .withFailMessage(
                            "Invalid registration should return 400. "
                                    + "HTTP %s, response: %s",
                            invalidStatus,
                            invalidResponse
                    )
                    .isEqualTo(400);

            invalidConnection.disconnect();
        }
    }
}