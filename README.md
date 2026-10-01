# Event Management System

A simple web-based event management application built with Spring Boot, Java, and a lightweight static frontend.

## Features

- Create and view events
- Create and view users
- Register users for events
- Track available seats
- In-memory H2 database for local execution
- REST API endpoints for event management

## Tech Stack

- Java 26
- Spring Boot 4.1.1
- Spring Web
- Spring Data JPA
- H2 Database
- Vanilla HTML/CSS/JavaScript frontend

## Run locally

```bash
./mvnw spring-boot:run
```

Open the app in your browser:

- http://localhost:8080/
- http://localhost:8080/h2-console

## API endpoints

- GET /api/events
- POST /api/events
- GET /api/users
- POST /api/users
- GET /api/registrations
- POST /api/registrations

## Default H2 database

- JDBC URL: jdbc:h2:mem:eventdb
- Username: sa
- Password: empty

## Project structure

```text
src/
  main/
    java/
      com/eventmanagment/
        config/
        controller/
        dto/
        entity/
        exception/
        repository/
        service/
    resources/
      static/
      application.properties
```
