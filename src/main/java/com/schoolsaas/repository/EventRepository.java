package com.schoolsaas.repository;

import com.schoolsaas.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    Page<Event> findBySchoolId(UUID schoolId, Pageable pageable);
    List<Event> findBySchoolIdAndStartDateBetween(UUID schoolId, LocalDateTime start, LocalDateTime end);
    List<Event> findBySchoolIdAndStartDateAfterOrderByStartDateAsc(UUID schoolId, LocalDateTime date);
}
