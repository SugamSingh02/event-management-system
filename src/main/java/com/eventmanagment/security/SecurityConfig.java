package com.eventmanagment.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // =========================
                        // AUTH
                        // =========================

                        .requestMatchers(
                                "/api/auth/**"
                        ).permitAll()

                        // =========================
                        // ADMIN
                        // =========================

                        .requestMatchers(
                                "/api/admin/**"
                        ).hasAuthority("ROLE_ADMIN")

                        // =========================
                        // ADMIN EVENT APPROVAL
                        // =========================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/events/admin/**"
                        ).hasAuthority("ROLE_ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/events/admin/**"
                        ).hasAuthority("ROLE_ADMIN")

                        // =========================
                        // MY PROFILE
                        // =========================

                        .requestMatchers(
                                "/api/users/me",
                                "/api/users/me/**"
                        ).authenticated()

                        // =========================
                        // USER MANAGEMENT
                        // ADMIN ONLY
                        // =========================

                        .requestMatchers(
                                "/api/users/**"
                        ).hasAuthority("ROLE_ADMIN")

                        // =========================
                        // ORGANIZER
                        // =========================

                        .requestMatchers(
                                "/api/organizer/**"
                        ).hasAnyAuthority(
                                "ROLE_ADMIN",
                                "ROLE_ORGANIZER"
                        )

                        // =========================
                        // ATTENDEE
                        // =========================

                        .requestMatchers(
                                "/api/attendee/**"
                        ).hasAnyAuthority(
                                "ROLE_ADMIN",
                                "ROLE_ATTENDEE"
                        )

                        // =========================
                        // REGISTRATION - EVENT VIEW
                        // ADMIN + ORGANIZER
                        // =========================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/registrations/event/**"
                        ).hasAnyAuthority(
                                "ROLE_ADMIN",
                                "ROLE_ORGANIZER"
                        )

                        // =========================
                        // REGISTRATION
                        // =========================

                        .requestMatchers(
                                "/api/registrations/**"
                        ).hasAnyAuthority(
                                "ROLE_ADMIN",
                                "ROLE_ATTENDEE"
                        )

                        // =========================
                        // TICKET
                        // =========================

                        .requestMatchers(
                                "/api/tickets/**"
                        ).hasAnyAuthority(
                                "ROLE_ADMIN",
                                "ROLE_ATTENDEE"
                        )

                        // =========================
                        // NOTIFICATIONS
                        // =========================

                        .requestMatchers(
                                "/api/notifications/**"
                        ).authenticated()

                        // =========================
                        // CREATE EVENT
                        // =========================

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/events"
                        ).hasAnyAuthority(
                                "ROLE_ADMIN",
                                "ROLE_ORGANIZER"
                        )

                        // =========================
                        // UPDATE EVENT
                        // =========================

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/events/**"
                        ).hasAnyAuthority(
                                "ROLE_ADMIN",
                                "ROLE_ORGANIZER"
                        )

                        // =========================
                        // DELETE EVENT
                        // =========================

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/events/**"
                        ).hasAnyAuthority(
                                "ROLE_ADMIN",
                                "ROLE_ORGANIZER"
                        )

                        // =========================
                        // READ EVENTS
                        // =========================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/events/**"
                        ).authenticated()

                        // =========================
                        // EVERYTHING ELSE
                        // =========================

                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}