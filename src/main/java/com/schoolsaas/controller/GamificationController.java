package com.schoolsaas.controller;

import com.schoolsaas.dto.gamification.BadgeDto;
import com.schoolsaas.dto.gamification.LeaderboardEntryDto;
import com.schoolsaas.model.Badge;
import com.schoolsaas.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    @PostMapping("/badges")
    public ResponseEntity<Badge> createBadge(@PathVariable UUID schoolId, @RequestBody BadgeDto dto) {
        return ResponseEntity.ok(gamificationService.createBadge(schoolId, dto));
    }

    @GetMapping("/badges")
    public ResponseEntity<Page<BadgeDto>> listBadges(@PathVariable UUID schoolId, Pageable pageable) {
        List<BadgeDto> list = gamificationService.listBadges(schoolId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }

    @PostMapping("/badges/{badgeId}/award/{userId}")
    public ResponseEntity<Void> awardBadge(@PathVariable UUID schoolId, @PathVariable UUID badgeId, @PathVariable UUID userId) {
        gamificationService.awardBadge(userId, badgeId, schoolId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(@PathVariable UUID schoolId, @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(gamificationService.getLeaderboard(schoolId, limit));
    }

    @GetMapping("/users/{userId}/points")
    public ResponseEntity<Integer> getUserPoints(@PathVariable UUID schoolId, @PathVariable UUID userId) {
        return ResponseEntity.ok(gamificationService.getUserPoints(userId, schoolId));
    }

    @GetMapping("/users/{userId}/badges")
    public ResponseEntity<Page<BadgeDto>> getUserBadges(@PathVariable UUID schoolId, @PathVariable UUID userId, Pageable pageable) {
        List<BadgeDto> list = gamificationService.getUserBadges(userId, schoolId);
        return ResponseEntity.ok(new PageImpl<>(list, pageable, list.size()));
    }
}
