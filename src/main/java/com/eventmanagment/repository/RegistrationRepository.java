package com.eventmanagment.repository;

import com.eventmanagment.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    List<Registration> findByEventId(Long eventId);

    List<Registration> findByUserId(Long userId);

    Optional<Registration> findByEventIdAndUserId(Long eventId, Long userId);

    @Query("SELECT r FROM Registration r WHERE r.event.organizer.id = :organizerId")
    List<Registration> findByEventOrganizerId(@Param("organizerId") Long organizerId);
}
