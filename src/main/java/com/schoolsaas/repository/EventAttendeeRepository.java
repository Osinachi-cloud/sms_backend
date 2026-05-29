package com.schoolsaas.repository;

import com.schoolsaas.model.EventAttendee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventAttendeeRepository extends JpaRepository<EventAttendee, UUID> {
    List<EventAttendee> findByEventId(UUID eventId);
    List<EventAttendee> findByUserId(UUID userId);
}
