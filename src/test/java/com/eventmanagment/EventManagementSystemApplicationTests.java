package com.eventmanagment;

import com.eventmanagment.dto.EventDTO;
import com.eventmanagment.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class EventManagementSystemApplicationTests {

    @Test
    void contextLoads() {

        try (ConfigurableApplicationContext context =
                     SpringApplication.run(
                             EventManagementSystemApplication.class,
                             "--server.port=0")) {

            assertThat(context).isNotNull();

            String port =
                    context.getEnvironment()
                            .getProperty("local.server.port");

            assertThat(port).isNotBlank();
        }
    }

    @Test
    void createEventAsOrganizerShouldSucceed() throws Exception {

        try (ConfigurableApplicationContext context =
                     SpringApplication.run(
                             EventManagementSystemApplication.class,
                             "--server.port=0")) {

            String port =
                    context.getEnvironment()
                            .getProperty("local.server.port");

            assertThat(port).isNotBlank();

            // -------------------------------------------------
            // 1. Login as organizer
            // -------------------------------------------------

            String organizerCookie =
                    login(
                            port,
                            "organizer@event.com",
                            "organizer123",
                            "ORGANIZER"
                    );

            assertThat(organizerCookie).isNotBlank();

            // -------------------------------------------------
            // 2. Create event
            // -------------------------------------------------

            HttpURLConnection eventConnection =
                    (HttpURLConnection) new URL(
                            "http://localhost:" + port +
                                    "/api/events"
                    ).openConnection();

            eventConnection.setRequestMethod("POST");

            TestHttpHelper.setJsonRequest(
                    eventConnection,
                    organizerCookie
            );

            String eventPayload = """
                    {
                        "title": "Test Event",
                        "description": "desc",
                        "location": "NYC",
                        "eventDate": "2027-09-25T20:00",
                        "capacity": 100
                    }
                    """;

            try (OutputStream os =
                         eventConnection.getOutputStream()) {

                os.write(
                        eventPayload.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
            }

            int statusCode =
                    eventConnection.getResponseCode();

            String responseBody =
                    readResponseBody(eventConnection);

            assertThat(statusCode)
                    .withFailMessage(
                            "Event creation failed. HTTP %s, response: %s",
                            statusCode,
                            responseBody
                    )
                    .isEqualTo(201);

            eventConnection.disconnect();
        }
    }

    @Test
    void registrationFlowShouldSucceedPreventDuplicateAndRejectOrganizer()
            throws Exception {

        try (ConfigurableApplicationContext context =
                     SpringApplication.run(
                             EventManagementSystemApplication.class,
                             "--server.port=0")) {

            String port =
                    context.getEnvironment()
                            .getProperty("local.server.port");

            assertThat(port).isNotBlank();

            JsonMapper objectMapper =
                    context.getBean(JsonMapper.class);

            // -------------------------------------------------
            // 1. Login as organizer
            // -------------------------------------------------

            String organizerCookie =
                    login(
                            port,
                            "organizer@event.com",
                            "organizer123",
                            "ORGANIZER"
                    );

            assertThat(organizerCookie).isNotBlank();

            // -------------------------------------------------
            // 2. Organizer creates a fresh event
            // -------------------------------------------------

            HttpURLConnection eventConnection =
                    (HttpURLConnection) new URL(
                            "http://localhost:" + port +
                                    "/api/events"
                    ).openConnection();

            eventConnection.setRequestMethod("POST");

            TestHttpHelper.setJsonRequest(
                    eventConnection,
                    organizerCookie
            );

            String eventPayload = """
                    {
                        "title": "Registration Test Event",
                        "description": "Testing registration flow",
                        "location": "Test Location",
                        "eventDate": "2027-10-10T18:00",
                        "capacity": 2
                    }
                    """;

            try (OutputStream os =
                         eventConnection.getOutputStream()) {

                os.write(
                        eventPayload.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
            }

            int eventStatus =
                    eventConnection.getResponseCode();

            String eventResponse =
                    readResponseBody(eventConnection);

            assertThat(eventStatus)
                    .withFailMessage(
                            "Test event creation failed. HTTP %s, response: %s",
                            eventStatus,
                            eventResponse
                    )
                    .isEqualTo(201);

            EventDTO createdEvent =
                    objectMapper.readValue(
                            eventResponse,
                            EventDTO.class
                    );

            assertThat(createdEvent.getId()).isNotNull();

            Long eventId =
                    createdEvent.getId();

            eventConnection.disconnect();

            // -------------------------------------------------
            // 3. Login as attendee
            // -------------------------------------------------

            String attendeeCookie =
                    login(
                            port,
                            "attendee@event.com",
                            "attendee123",
                            "ATTENDEE"
                    );

            assertThat(attendeeCookie).isNotBlank();

            // -------------------------------------------------
            // 4. Attendee registers successfully
            // -------------------------------------------------

            HttpURLConnection registrationConnection =
                    (HttpURLConnection) new URL(
                            "http://localhost:" + port +
                                    "/api/registrations"
                    ).openConnection();

            registrationConnection.setRequestMethod("POST");

            TestHttpHelper.setJsonRequest(
                    registrationConnection,
                    attendeeCookie
            );

            String registrationPayload = """
                    {
                        "eventId": %d
                    }
                    """.formatted(eventId);

            try (OutputStream os =
                         registrationConnection.getOutputStream()) {

                os.write(
                        registrationPayload.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
            }

            int registrationStatus =
                    registrationConnection.getResponseCode();

            String registrationResponse =
                    readResponseBody(registrationConnection);

            assertThat(registrationStatus)
                    .withFailMessage(
                            "Attendee registration failed. HTTP %s, response: %s",
                            registrationStatus,
                            registrationResponse
                    )
                    .isEqualTo(201);

            assertThat(registrationResponse)
                    .contains("\"id\"");

            assertThat(registrationResponse)
                    .contains("\"eventId\"");

            assertThat(registrationResponse)
                    .contains("\"userId\"");

            assertThat(registrationResponse)
                    .contains("\"status\"");

            assertThat(registrationResponse)
                    .contains("CONFIRMED");

            assertThat(registrationResponse)
                    .contains("\"registeredAt\"");

            registrationConnection.disconnect();

            // -------------------------------------------------
            // 5. Same attendee tries to register again
            // -------------------------------------------------

            HttpURLConnection duplicateConnection =
                    (HttpURLConnection) new URL(
                            "http://localhost:" + port +
                                    "/api/registrations"
                    ).openConnection();

            duplicateConnection.setRequestMethod("POST");

            TestHttpHelper.setJsonRequest(
                    duplicateConnection,
                    attendeeCookie
            );

            try (OutputStream os =
                         duplicateConnection.getOutputStream()) {

                os.write(
                        registrationPayload.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
            }

            int duplicateStatus =
                    duplicateConnection.getResponseCode();

            String duplicateResponse =
                    readResponseBody(duplicateConnection);

            assertThat(duplicateStatus)
                    .withFailMessage(
                            "Duplicate registration should have been rejected. "
                                    + "HTTP %s, response: %s",
                            duplicateStatus,
                            duplicateResponse
                    )
                    .isEqualTo(400);

            duplicateConnection.disconnect();

            // -------------------------------------------------
            // 6. Organizer tries to register
            // -------------------------------------------------

            HttpURLConnection organizerRegistrationConnection =
                    (HttpURLConnection) new URL(
                            "http://localhost:" + port +
                                    "/api/registrations"
                    ).openConnection();

            organizerRegistrationConnection.setRequestMethod("POST");

            TestHttpHelper.setJsonRequest(
                    organizerRegistrationConnection,
                    organizerCookie
            );

            try (OutputStream os =
                         organizerRegistrationConnection
                                 .getOutputStream()) {

                os.write(
                        registrationPayload.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
            }

            int organizerRegistrationStatus =
                    organizerRegistrationConnection
                            .getResponseCode();

            String organizerRegistrationResponse =
                    readResponseBody(
                            organizerRegistrationConnection
                    );

            assertThat(organizerRegistrationStatus)
                    .withFailMessage(
                            "Organizer should not be allowed to register "
                                    + "as an attendee. HTTP %s, response: %s",
                            organizerRegistrationStatus,
                            organizerRegistrationResponse
                    )
                    .isEqualTo(403);

            organizerRegistrationConnection.disconnect();
        }
    }

    @Test
    void organizerOwnershipShouldBeEnforced() throws Exception {

        try (ConfigurableApplicationContext context =
                     SpringApplication.run(
                             EventManagementSystemApplication.class,
                             "--server.port=0")) {

            String port =
                    context.getEnvironment()
                            .getProperty("local.server.port");

            assertThat(port).isNotBlank();

            JsonMapper objectMapper =
                    context.getBean(JsonMapper.class);

            // -------------------------------------------------
            // 1. Login as first organizer
            // -------------------------------------------------

            String organizerCookie =
                    login(
                            port,
                            "organizer@event.com",
                            "organizer123",
                            "ORGANIZER"
                    );

            assertThat(organizerCookie).isNotBlank();

            // -------------------------------------------------
            // 2. First organizer creates an event
            // -------------------------------------------------

            HttpURLConnection createConnection =
                    (HttpURLConnection) new URL(
                            "http://localhost:" + port +
                                    "/api/events"
                    ).openConnection();

            createConnection.setRequestMethod("POST");

            TestHttpHelper.setJsonRequest(
                    createConnection,
                    organizerCookie
            );

            String eventPayload = """
                    {
                        "title": "Ownership Test Event",
                        "description": "Ownership testing",
                        "location": "Test Location",
                        "eventDate": "2027-12-01T18:00",
                        "capacity": 10
                    }
                    """;

            try (OutputStream os =
                         createConnection.getOutputStream()) {

                os.write(
                        eventPayload.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
            }

            int createStatus =
                    createConnection.getResponseCode();

            String createResponse =
                    readResponseBody(createConnection);

            assertThat(createStatus)
                    .withFailMessage(
                            "Event creation failed. HTTP %s, response: %s",
                            createStatus,
                            createResponse
                    )
                    .isEqualTo(201);

            EventDTO createdEvent =
                    objectMapper.readValue(
                            createResponse,
                            EventDTO.class
                    );

            Long eventId =
                    createdEvent.getId();

            assertThat(eventId).isNotNull();

            createConnection.disconnect();

            // -------------------------------------------------
            // 3. Login as admin
            // -------------------------------------------------

            String adminCookie =
                    login(
                            port,
                            "admin@event.com",
                            "admin123",
                            "ADMIN"
                    );

            assertThat(adminCookie).isNotBlank();

            // -------------------------------------------------
            // 4. Admin creates a second organizer
            // -------------------------------------------------

            String secondOrganizerEmail =
                    "organizer."
                            + System.nanoTime()
                            + "@example.com";

            HttpURLConnection userConnection =
                    (HttpURLConnection) new URL(
                            "http://localhost:" + port +
                                    "/api/users"
                    ).openConnection();

            userConnection.setRequestMethod("POST");

            TestHttpHelper.setJsonRequest(
                    userConnection,
                    adminCookie
            );

            String secondOrganizerPayload = """
                    {
                        "name": "Second Organizer",
                        "email": "%s",
                        "phone": "+91 90000 00100",
                        "password": "organizer456",
                        "role": "ORGANIZER"
                    }
                    """.formatted(secondOrganizerEmail);

            try (OutputStream os =
                         userConnection.getOutputStream()) {

                os.write(
                        secondOrganizerPayload.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
            }

            int userStatus =
                    userConnection.getResponseCode();

            String userResponse =
                    readResponseBody(userConnection);

            assertThat(userStatus)
                    .withFailMessage(
                            "Second organizer creation failed. HTTP %s, response: %s",
                            userStatus,
                            userResponse
                    )
                    .isEqualTo(201);

            UserDTO secondOrganizer =
                    objectMapper.readValue(
                            userResponse,
                            UserDTO.class
                    );

            assertThat(secondOrganizer.getRole())
                    .isEqualTo("ORGANIZER");

            userConnection.disconnect();

            // -------------------------------------------------
            // 5. Second organizer logs in
            // -------------------------------------------------

            String secondOrganizerCookie =
                    login(
                            port,
                            secondOrganizerEmail,
                            "organizer456",
                            "ORGANIZER"
                    );

            assertThat(secondOrganizerCookie)
                    .isNotBlank();

            // -------------------------------------------------
            // 6. Second organizer tries to modify
            //    first organizer's event
            // -------------------------------------------------

            HttpURLConnection unauthorizedUpdate =
                    (HttpURLConnection) new URL(
                            "http://localhost:" + port +
                                    "/api/events/" + eventId
                    ).openConnection();

            unauthorizedUpdate.setRequestMethod("PUT");

            TestHttpHelper.setJsonRequest(
                    unauthorizedUpdate,
                    secondOrganizerCookie
            );

            String updatePayload = """
                    {
                        "title": "Unauthorized Update",
                        "description": "Should fail",
                        "location": "Test Location",
                        "eventDate": "2027-12-01T18:00",
                        "capacity": 10
                    }
                    """;

            try (OutputStream os =
                         unauthorizedUpdate.getOutputStream()) {

                os.write(
                        updatePayload.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
            }

            int unauthorizedStatus =
                    unauthorizedUpdate.getResponseCode();

            String unauthorizedResponse =
                    readResponseBody(unauthorizedUpdate);

            assertThat(unauthorizedStatus)
                    .withFailMessage(
                            "Another organizer was allowed to modify "
                                    + "an event they do not own. "
                                    + "HTTP %s, response: %s",
                            unauthorizedStatus,
                            unauthorizedResponse
                    )
                    .isEqualTo(403);

            unauthorizedUpdate.disconnect();

            // -------------------------------------------------
            // 7. Original organizer modifies own event
            // -------------------------------------------------

            HttpURLConnection authorizedUpdate =
                    (HttpURLConnection) new URL(
                            "http://localhost:" + port +
                                    "/api/events/" + eventId
                    ).openConnection();

            authorizedUpdate.setRequestMethod("PUT");

            TestHttpHelper.setJsonRequest(
                    authorizedUpdate,
                    organizerCookie
            );

            String authorizedPayload = """
                    {
                        "title": "Authorized Update",
                        "description": "Owner can update",
                        "location": "Updated Location",
                        "eventDate": "2027-12-01T18:00",
                        "capacity": 10
                    }
                    """;

            try (OutputStream os =
                         authorizedUpdate.getOutputStream()) {

                os.write(
                        authorizedPayload.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
            }

            int authorizedStatus =
                    authorizedUpdate.getResponseCode();

            String authorizedResponse =
                    readResponseBody(authorizedUpdate);

            assertThat(authorizedStatus)
                    .withFailMessage(
                            "Event owner could not update their own event. "
                                    + "HTTP %s, response: %s",
                            authorizedStatus,
                            authorizedResponse
                    )
                    .isEqualTo(200);

            authorizedUpdate.disconnect();
        }
    }

    @Test
    void seatAndCapacityRulesShouldBeEnforced() throws Exception {

        try (ConfigurableApplicationContext context =
                     SpringApplication.run(
                             EventManagementSystemApplication.class,
                             "--server.port=0")) {

            String port =
                    context.getEnvironment()
                            .getProperty("local.server.port");

            assertThat(port).isNotBlank();

            JsonMapper objectMapper =
                    context.getBean(JsonMapper.class);

            // -------------------------------------------------
            // 1. Create three unique attendees
            // -------------------------------------------------

            UserDTO attendeeOne =
                    registerUniqueAttendee(
                            port,
                            objectMapper
                    );

            UserDTO attendeeTwo =
                    registerUniqueAttendee(
                            port,
                            objectMapper
                    );

            UserDTO attendeeThree =
                    registerUniqueAttendee(
                            port,
                            objectMapper
                    );

            // -------------------------------------------------
            // 2. Login as organizer
            // -------------------------------------------------

            String organizerCookie =
                    login(
                            port,
                            "organizer@event.com",
                            "organizer123",
                            "ORGANIZER"
                    );

            assertThat(organizerCookie)
                    .isNotBlank();

            // -------------------------------------------------
            // 3. Create event with capacity = 2
            // -------------------------------------------------

            HttpURLConnection createConnection =
                    (HttpURLConnection) new URL(
                            "http://localhost:" + port +
                                    "/api/events"
                    ).openConnection();

            createConnection.setRequestMethod("POST");

            TestHttpHelper.setJsonRequest(
                    createConnection,
                    organizerCookie
            );

            String eventPayload = """
                    {
                        "title": "Seat Capacity Test Event",
                        "description": "Testing seats and capacity",
                        "location": "Test Location",
                        "eventDate": "2028-01-15T18:00",
                        "capacity": 2
                    }
                    """;

            try (OutputStream os =
                         createConnection.getOutputStream()) {

                os.write(
                        eventPayload.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
            }

            int createStatus =
                    createConnection.getResponseCode();

            String createResponse =
                    readResponseBody(createConnection);

            assertThat(createStatus)
                    .withFailMessage(
                            "Seat test event creation failed. "
                                    + "HTTP %s, response: %s",
                            createStatus,
                            createResponse
                    )
                    .isEqualTo(201);

            // -------------------------------------------------
            // Extract id and read-only availableSeats
            // -------------------------------------------------

            Long eventId =
                    extractLongField(
                            createResponse,
                            "id"
                    );

            assertThat(eventId)
                    .isNotNull();

            EventDTO event =
                    objectMapper.readValue(
                            createResponse,
                            EventDTO.class
                    );

            assertThat(event.getCapacity())
                    .isEqualTo(2);

            assertThat(
                    extractLongField(
                            createResponse,
                            "availableSeats"
                    )
            ).isEqualTo(2);

            createConnection.disconnect();

            // -------------------------------------------------
            // 4. First attendee registers
            // -------------------------------------------------

            String attendeeOneCookie =
                    login(
                            port,
                            attendeeOne.getEmail(),
                            "test123",
                            "ATTENDEE"
                    );

            String registrationOneResponse =
                    createRegistration(
                            port,
                            eventId,
                            attendeeOneCookie
                    );

            Long registrationOneId =
                    extractLongField(
                            registrationOneResponse,
                            "id"
                    );

            assertThat(registrationOneId)
                    .isNotNull();

            String afterFirstRegistration =
                    getEventResponse(
                            port,
                            eventId
                    );

            assertThat(
                    extractLongField(
                            afterFirstRegistration,
                            "availableSeats"
                    )
            ).isEqualTo(1);

            // -------------------------------------------------
            // 5. Second attendee registers
            // -------------------------------------------------

            String attendeeTwoCookie =
                    login(
                            port,
                            attendeeTwo.getEmail(),
                            "test123",
                            "ATTENDEE"
                    );

            String registrationTwoResponse =
                    createRegistration(
                            port,
                            eventId,
                            attendeeTwoCookie
                    );

            Long registrationTwoId =
                    extractLongField(
                            registrationTwoResponse,
                            "id"
                    );

            assertThat(registrationTwoId)
                    .isNotNull();

            String afterSecondRegistration =
                    getEventResponse(
                            port,
                            eventId
                    );

            assertThat(
                    extractLongField(
                            afterSecondRegistration,
                            "availableSeats"
                    )
            ).isEqualTo(0);

            // -------------------------------------------------
            // 6. Third attendee tries to register when full
            // -------------------------------------------------

            String attendeeThreeCookie =
                    login(
                            port,
                            attendeeThree.getEmail(),
                            "test123",
                            "ATTENDEE"
                    );

            HttpURLConnection fullEventConnection =
                    (HttpURLConnection) new URL(
                            "http://localhost:" + port +
                                    "/api/registrations"
                    ).openConnection();

            fullEventConnection.setRequestMethod("POST");

            TestHttpHelper.setJsonRequest(
                    fullEventConnection,
                    attendeeThreeCookie
            );

            String fullEventPayload = """
                    {
                        "eventId": %d
                    }
                    """.formatted(eventId);

            try (OutputStream os =
                         fullEventConnection.getOutputStream()) {

                os.write(
                        fullEventPayload.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
            }

            int fullEventStatus =
                    fullEventConnection.getResponseCode();

            String fullEventResponse =
                    readResponseBody(fullEventConnection);

            assertThat(fullEventStatus)
                    .withFailMessage(
                            "Registration should fail when event is full. "
                                    + "HTTP %s, response: %s",
                            fullEventStatus,
                            fullEventResponse
                    )
                    .isEqualTo(400);

            fullEventConnection.disconnect();

            // -------------------------------------------------
            // 7. Capacity cannot be reduced below
            //    existing registrations
            // -------------------------------------------------

            HttpURLConnection capacityConnection =
                    (HttpURLConnection) new URL(
                            "http://localhost:" + port +
                                    "/api/events/" + eventId
                    ).openConnection();

            capacityConnection.setRequestMethod("PUT");

            TestHttpHelper.setJsonRequest(
                    capacityConnection,
                    organizerCookie
            );

            String invalidCapacityPayload = """
                    {
                        "title": "Seat Capacity Test Event",
                        "description": "Testing seats and capacity",
                        "location": "Test Location",
                        "eventDate": "2028-01-15T18:00",
                        "capacity": 1
                    }
                    """;

            try (OutputStream os =
                         capacityConnection.getOutputStream()) {

                os.write(
                        invalidCapacityPayload.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
            }

            int capacityStatus =
                    capacityConnection.getResponseCode();

            String capacityResponse =
                    readResponseBody(capacityConnection);

            assertThat(capacityStatus)
                    .withFailMessage(
                            "Capacity should not be reduced below "
                                    + "existing registrations. "
                                    + "HTTP %s, response: %s",
                            capacityStatus,
                            capacityResponse
                    )
                    .isEqualTo(400);

            capacityConnection.disconnect();

            // -------------------------------------------------
            // 8. First attendee cancels registration
            // -------------------------------------------------

            HttpURLConnection cancelConnection =
                    (HttpURLConnection) new URL(
                            "http://localhost:" + port +
                                    "/api/registrations/" +
                                    registrationOneId
                    ).openConnection();

            cancelConnection.setRequestMethod("DELETE");

            TestHttpHelper.setJsonRequest(
                    cancelConnection,
                    attendeeOneCookie
            );

            int cancelStatus =
                    cancelConnection.getResponseCode();

            String cancelResponse =
                    readResponseBody(cancelConnection);

            assertThat(cancelStatus)
                    .withFailMessage(
                            "Registration cancellation failed. "
                                    + "HTTP %s, response: %s",
                            cancelStatus,
                            cancelResponse
                    )
                    .isEqualTo(204);

            cancelConnection.disconnect();

            // -------------------------------------------------
            // 9. Seat should be restored
            // -------------------------------------------------

            String afterCancellation =
                    getEventResponse(
                            port,
                            eventId
                    );

            assertThat(
                    extractLongField(
                            afterCancellation,
                            "availableSeats"
                    )
            ).isEqualTo(1);
        }
    }

    @Test
    void authorizationRulesShouldBeEnforced() throws Exception {

        try (ConfigurableApplicationContext context =
                     SpringApplication.run(
                             EventManagementSystemApplication.class,
                             "--server.port=0")) {

            String port =
                    context.getEnvironment()
                            .getProperty("local.server.port");

            assertThat(port).isNotBlank();

            // -------------------------------------------------
            // 1. Public GET /api/events should work without login
            // -------------------------------------------------

            HttpURLConnection publicEvents =
                    (HttpURLConnection) new URL(
                            "http://localhost:" + port +
                                    "/api/events"
                    ).openConnection();

            publicEvents.setRequestMethod("GET");

            int publicEventsStatus =
                    publicEvents.getResponseCode();

            String publicEventsResponse =
                    readResponseBody(publicEvents);

            assertThat(publicEventsStatus)
                    .withFailMessage(
                            "Public event list should be accessible. "
                                    + "HTTP %s, response: %s",
                            publicEventsStatus,
                            publicEventsResponse
                    )
                    .isEqualTo(200);

            publicEvents.disconnect();

            // -------------------------------------------------
            // 2. Unauthenticated POST /api/events must fail
            // -------------------------------------------------

            HttpURLConnection guestCreateEvent =
                    (HttpURLConnection) new URL(
                            "http://localhost:" + port +
                                    "/api/events"
                    ).openConnection();

            guestCreateEvent.setRequestMethod("POST");

            TestHttpHelper.setJsonRequest(
                    guestCreateEvent,
                    null
            );

            String eventPayload = """
                    {
                        "title": "Authorization Test Event",
                        "description": "Testing authorization",
                        "location": "Test Location",
                        "eventDate": "2028-03-15T18:00",
                        "capacity": 10
                    }
                    """;

            try (OutputStream os =
                         guestCreateEvent.getOutputStream()) {

                os.write(
                        eventPayload.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
            }

            int guestCreateStatus =
                    guestCreateEvent.getResponseCode();

            String guestCreateResponse =
                    readResponseBody(guestCreateEvent);

            assertThat(guestCreateStatus)
                    .withFailMessage(
                            "Unauthenticated user should not create events. "
                                    + "HTTP %s, response: %s",
                            guestCreateStatus,
                            guestCreateResponse
                    )
                    .isEqualTo(403);

            guestCreateEvent.disconnect();

            // -------------------------------------------------
            // 3. Login as attendee
            // -------------------------------------------------

            String attendeeCookie =
                    login(
                            port,
                            "attendee@event.com",
                            "attendee123",
                            "ATTENDEE"
                    );

            assertThat(attendeeCookie)
                    .isNotBlank();

            // -------------------------------------------------
            // 4. Attendee cannot create an event
            // -------------------------------------------------

            HttpURLConnection attendeeCreateEvent =
                    (HttpURLConnection) new URL(
                            "http://localhost:" + port +
                                    "/api/events"
                    ).openConnection();

            attendeeCreateEvent.setRequestMethod("POST");

            TestHttpHelper.setJsonRequest(
                    attendeeCreateEvent,
                    attendeeCookie
            );

            try (OutputStream os =
                         attendeeCreateEvent.getOutputStream()) {

                os.write(
                        eventPayload.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
            }

            int attendeeCreateStatus =
                    attendeeCreateEvent.getResponseCode();

            String attendeeCreateResponse =
                    readResponseBody(attendeeCreateEvent);

            assertThat(attendeeCreateStatus)
                    .withFailMessage(
                            "Attendee should not create events. "
                                    + "HTTP %s, response: %s",
                            attendeeCreateStatus,
                            attendeeCreateResponse
                    )
                    .isEqualTo(403);

            attendeeCreateEvent.disconnect();

            // -------------------------------------------------
            // 5. Attendee cannot create users
            // -------------------------------------------------

            HttpURLConnection attendeeCreateUser =
                    (HttpURLConnection) new URL(
                            "http://localhost:" + port +
                                    "/api/users"
                    ).openConnection();

            attendeeCreateUser.setRequestMethod("POST");

            TestHttpHelper.setJsonRequest(
                    attendeeCreateUser,
                    attendeeCookie
            );

            String userPayload = """
                    {
                        "name": "Unauthorized User",
                        "email": "unauthorized@example.com",
                        "phone": "+91 90000 00300",
                        "password": "test123",
                        "role": "ATTENDEE"
                    }
                    """;

            try (OutputStream os =
                         attendeeCreateUser.getOutputStream()) {

                os.write(
                        userPayload.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
            }

            int attendeeCreateUserStatus =
                    attendeeCreateUser.getResponseCode();

            String attendeeCreateUserResponse =
                    readResponseBody(attendeeCreateUser);

            assertThat(attendeeCreateUserStatus)
                    .withFailMessage(
                            "Attendee should not create users. "
                                    + "HTTP %s, response: %s",
                            attendeeCreateUserStatus,
                            attendeeCreateUserResponse
                    )
                    .isEqualTo(403);

            attendeeCreateUser.disconnect();

            // -------------------------------------------------
            // 6. Attendee cannot delete users
            // -------------------------------------------------

            HttpURLConnection attendeeDeleteUser =
                    (HttpURLConnection) new URL(
                            "http://localhost:" + port +
                                    "/api/users/1"
                    ).openConnection();

            attendeeDeleteUser.setRequestMethod("DELETE");

            TestHttpHelper.setJsonRequest(
                    attendeeDeleteUser,
                    attendeeCookie
            );

            int attendeeDeleteStatus =
                    attendeeDeleteUser.getResponseCode();

            String attendeeDeleteResponse =
                    readResponseBody(attendeeDeleteUser);

            assertThat(attendeeDeleteStatus)
                    .withFailMessage(
                            "Attendee should not delete users. "
                                    + "HTTP %s, response: %s",
                            attendeeDeleteStatus,
                            attendeeDeleteResponse
                    )
                    .isEqualTo(403);

            attendeeDeleteUser.disconnect();

            // -------------------------------------------------
            // 7. Unauthenticated GET /api/registrations
            //    must fail
            // -------------------------------------------------

            HttpURLConnection guestRegistrations =
                    (HttpURLConnection) new URL(
                            "http://localhost:" + port +
                                    "/api/registrations"
                    ).openConnection();

            guestRegistrations.setRequestMethod("GET");

            int guestRegistrationsStatus =
                    guestRegistrations.getResponseCode();

            String guestRegistrationsResponse =
                    readResponseBody(guestRegistrations);

            assertThat(guestRegistrationsStatus)
                    .withFailMessage(
                            "Unauthenticated user should not view registrations. "
                                    + "HTTP %s, response: %s",
                            guestRegistrationsStatus,
                            guestRegistrationsResponse
                    )
                    .isEqualTo(403);

            guestRegistrations.disconnect();
        }
    }

    // ---------------------------------------------------------
    // Helper: register a unique attendee
    // ---------------------------------------------------------

    private static UserDTO registerUniqueAttendee(
            String port,
            JsonMapper objectMapper) throws Exception {

        String email =
                "seat.test."
                        + System.nanoTime()
                        + "@example.com";

        HttpURLConnection connection =
                (HttpURLConnection) new URL(
                        "http://localhost:" + port +
                                "/api/users/register"
                ).openConnection();

        connection.setRequestMethod("POST");

        TestHttpHelper.setJsonRequest(
                connection,
                null
        );

        String payload = """
                {
                    "name": "Seat Test User",
                    "email": "%s",
                    "phone": "+91 90000 00200",
                    "password": "test123",
                    "role": "ATTENDEE"
                }
                """.formatted(email);

        try (OutputStream os =
                     connection.getOutputStream()) {

            os.write(
                    payload.getBytes(
                            StandardCharsets.UTF_8
                    )
            );
        }

        int status =
                connection.getResponseCode();

        String response =
                readResponseBody(connection);

        assertThat(status)
                .withFailMessage(
                        "Test attendee creation failed. "
                                + "HTTP %s, response: %s",
                        status,
                        response
                )
                .isEqualTo(201);

        UserDTO user =
                objectMapper.readValue(
                        response,
                        UserDTO.class
                );

        connection.disconnect();

        return user;
    }

    // ---------------------------------------------------------
    // Helper: create registration
    // ---------------------------------------------------------

    private static String createRegistration(
            String port,
            Long eventId,
            String cookie) throws Exception {

        HttpURLConnection connection =
                (HttpURLConnection) new URL(
                        "http://localhost:" + port +
                                "/api/registrations"
                ).openConnection();

        connection.setRequestMethod("POST");

        TestHttpHelper.setJsonRequest(
                connection,
                cookie
        );

        String payload = """
                {
                    "eventId": %d
                }
                """.formatted(eventId);

        try (OutputStream os =
                     connection.getOutputStream()) {

            os.write(
                    payload.getBytes(
                            StandardCharsets.UTF_8
                    )
            );
        }

        int status =
                connection.getResponseCode();

        String response =
                readResponseBody(connection);

        assertThat(status)
                .withFailMessage(
                        "Registration failed. HTTP %s, response: %s",
                        status,
                        response
                )
                .isEqualTo(201);

        connection.disconnect();

        return response;
    }

    // ---------------------------------------------------------
    // Helper: get event response JSON
    // ---------------------------------------------------------

    private static String getEventResponse(
            String port,
            Long eventId) throws Exception {

        HttpURLConnection connection =
                (HttpURLConnection) new URL(
                        "http://localhost:" + port +
                                "/api/events/" + eventId
                ).openConnection();

        connection.setRequestMethod("GET");

        int status =
                connection.getResponseCode();

        String response =
                readResponseBody(connection);

        assertThat(status)
                .withFailMessage(
                        "Could not retrieve event. "
                                + "HTTP %s, response: %s",
                        status,
                        response
                )
                .isEqualTo(200);

        connection.disconnect();

        return response;
    }

    // ---------------------------------------------------------
    // Helper: login and return session cookie
    // ---------------------------------------------------------

    private static String login(
            String port,
            String identifier,
            String password,
            String role) throws Exception {

        HttpURLConnection connection =
                (HttpURLConnection) new URL(
                        "http://localhost:" + port +
                                "/api/users/login"
                ).openConnection();

        connection.setRequestMethod("POST");

        TestHttpHelper.setJsonRequest(
                connection,
                null
        );

        String payload = """
                {
                    "identifier": "%s",
                    "password": "%s",
                    "role": "%s"
                }
                """.formatted(
                identifier,
                password,
                role
        );

        try (OutputStream os =
                     connection.getOutputStream()) {

            os.write(
                    payload.getBytes(
                            StandardCharsets.UTF_8
                    )
            );
        }

        int statusCode =
                connection.getResponseCode();

        String responseBody =
                readResponseBody(connection);

        assertThat(statusCode)
                .withFailMessage(
                        "Login failed for %s. HTTP %s, response: %s",
                        identifier,
                        statusCode,
                        responseBody
                )
                .isEqualTo(200);

        String sessionCookie =
                TestHttpHelper.extractSessionCookie(
                        connection
                );

        assertThat(sessionCookie)
                .withFailMessage(
                        "Login succeeded but no session cookie "
                                + "was returned for %s",
                        identifier
                )
                .isNotBlank();

        connection.disconnect();

        return sessionCookie;
    }

    // ---------------------------------------------------------
    // Helper: read HTTP response body
    // ---------------------------------------------------------

    private static String readResponseBody(
            HttpURLConnection connection) throws Exception {

        InputStream inputStream;

        if (connection.getResponseCode() >= 400) {

            inputStream =
                    connection.getErrorStream();

            if (inputStream == null) {
                return "";
            }

        } else {

            inputStream =
                    connection.getInputStream();
        }

        try (InputStream stream = inputStream) {

            return new String(
                    stream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }

    // ---------------------------------------------------------
    // Helper: extract numeric JSON field
    // ---------------------------------------------------------

    private static Long extractLongField(
            String json,
            String fieldName) {

        Pattern pattern =
                Pattern.compile(
                        "\"" + Pattern.quote(fieldName) +
                                "\"\\s*:\\s*(\\d+)"
                );

        Matcher matcher =
                pattern.matcher(json);

        if (!matcher.find()) {

            throw new AssertionError(
                    "Field '" + fieldName +
                            "' was not found in response: " +
                            json
            );
        }

        return Long.valueOf(
                matcher.group(1)
        );
    }
}