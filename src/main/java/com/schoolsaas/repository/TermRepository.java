package com.schoolsaas.repository;

import com.schoolsaas.model.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TermRepository extends JpaRepository<Term, UUID> {

    List<Term> findBySchoolIdOrderByStartDateDesc(UUID schoolId);

    Optional<Term> findBySchoolIdAndIsCurrentTrue(UUID schoolId);

    List<Term> findBySessionId(UUID sessionId);
}
