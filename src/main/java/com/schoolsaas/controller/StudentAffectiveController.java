package com.schoolsaas.controller;

import com.schoolsaas.model.StudentAffectiveRating;
import com.schoolsaas.service.StudentAffectiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/affective-ratings")
@RequiredArgsConstructor
public class StudentAffectiveController {

    private final StudentAffectiveService affectiveService;

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<StudentAffectiveRating>> getStudentRatings(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            @RequestParam UUID termId,
            @RequestParam(required = false) Integer weekNumber) {
        return ResponseEntity.ok(affectiveService.getRatings(schoolId, studentId, termId, weekNumber));
    }

    @GetMapping
    public ResponseEntity<List<StudentAffectiveRating>> getAllRatingsForTerm(
            @PathVariable UUID schoolId,
            @RequestParam UUID termId,
            @RequestParam(required = false) Integer weekNumber) {
        return ResponseEntity.ok(affectiveService.getRatingsForTerm(schoolId, termId, weekNumber));
    }

    @PostMapping("/student/{studentId}")
    public ResponseEntity<List<StudentAffectiveRating>> saveStudentRatings(
            @PathVariable UUID schoolId,
            @PathVariable UUID studentId,
            @RequestParam UUID termId,
            @RequestParam(required = false) Integer weekNumber,
            @RequestBody List<Map<String, Object>> ratings) {
        return ResponseEntity.ok(affectiveService.saveRatings(schoolId, studentId, termId, weekNumber, ratings));
    }

    @DeleteMapping("/{ratingId}")
    public ResponseEntity<Void> deleteRating(@PathVariable UUID ratingId) {
        affectiveService.deleteRating(ratingId);
        return ResponseEntity.noContent().build();
    }
}
