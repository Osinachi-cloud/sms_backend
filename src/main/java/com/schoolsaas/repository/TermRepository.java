package com.schoolsaas.repository;

import com.schoolsaas.model.Term;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TermRepository extends JpaRepository<Term, UUID> {

    List<Term> findBySchoolIdOrderByStartDateDesc(UUID schoolId);

    Page<Term> findBySchoolIdOrderByStartDateDesc(UUID schoolId, Pageable pageable);

    Optional<Term> findBySchoolIdAndIsCurrentTrue(UUID schoolId);

    List<Term> findBySessionId(UUID sessionId);

    Page<Term> findBySessionId(UUID sessionId, Pageable pageable);
}
