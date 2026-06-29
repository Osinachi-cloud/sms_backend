package com.schoolsaas.repository;

import com.schoolsaas.model.StudentAffectiveRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentAffectiveRatingRepository extends JpaRepository<StudentAffectiveRating, UUID> {

    List<StudentAffectiveRating> findBySchoolIdAndStudentIdAndTermId(UUID schoolId, UUID studentId, UUID termId);

    List<StudentAffectiveRating> findBySchoolIdAndTermId(UUID schoolId, UUID termId);

    Optional<StudentAffectiveRating> findBySchoolIdAndStudentIdAndTermIdAndTrait(UUID schoolId, UUID studentId, UUID termId, String trait);
}
