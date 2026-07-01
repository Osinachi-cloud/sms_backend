package com.schoolsaas.service;

import com.schoolsaas.model.StudentAffectiveRating;
import com.schoolsaas.repository.StudentAffectiveRatingRepository;
import com.schoolsaas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class StudentAffectiveService {

    private final StudentAffectiveRatingRepository ratingRepository;

    @Transactional(readOnly = true)
    public List<StudentAffectiveRating> getRatings(UUID schoolId, UUID studentId, UUID termId, Integer weekNumber) {
        if (weekNumber != null) {
            return ratingRepository.findBySchoolIdAndTermIdAndWeekNumber(schoolId, termId, weekNumber)
                    .stream()
                    .filter(r -> r.getStudentId().equals(studentId))
                    .toList();
        }
        return ratingRepository.findBySchoolIdAndStudentIdAndTermId(schoolId, studentId, termId);
    }

    @Transactional(readOnly = true)
    public List<StudentAffectiveRating> getRatingsForTerm(UUID schoolId, UUID termId, Integer weekNumber) {
        if (weekNumber != null) {
            return ratingRepository.findBySchoolIdAndTermIdAndWeekNumber(schoolId, termId, weekNumber);
        }
        return ratingRepository.findBySchoolIdAndTermId(schoolId, termId);
    }

    @Transactional
    public List<StudentAffectiveRating> saveRatings(UUID schoolId, UUID studentId, UUID termId, Integer weekNumber, List<Map<String, Object>> ratings) {
        UUID raterId = SecurityUtils.getCurrentUserId();
        List<StudentAffectiveRating> result = new ArrayList<>();
        int week = weekNumber != null ? weekNumber : 1;

        for (Map<String, Object> entry : ratings) {
            String trait = (String) entry.get("trait");
            Integer rating = entry.get("rating") instanceof Number ? ((Number) entry.get("rating")).intValue() : null;
            String remarks = entry.get("remarks") instanceof String ? (String) entry.get("remarks") : null;

            Optional<StudentAffectiveRating> existing = ratingRepository
                    .findBySchoolIdAndStudentIdAndTermIdAndWeekNumberAndTrait(schoolId, studentId, termId, week, trait);

            StudentAffectiveRating record;
            if (existing.isPresent()) {
                record = existing.get();
                record.setRating(rating);
                record.setRemarks(remarks);
                record.setRatedBy(raterId);
            } else {
                record = StudentAffectiveRating.builder()
                        .schoolId(schoolId)
                        .studentId(studentId)
                        .termId(termId)
                        .weekNumber(week)
                        .trait(trait)
                        .rating(rating)
                        .remarks(remarks)
                        .ratedBy(raterId)
                        .build();
            }
            result.add(ratingRepository.save(record));
        }
        return result;
    }

    @Transactional
    public void deleteRating(UUID ratingId) {
        ratingRepository.deleteById(ratingId);
    }
}
