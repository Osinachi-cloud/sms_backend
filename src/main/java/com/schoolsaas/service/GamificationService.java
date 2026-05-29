package com.schoolsaas.service;

import com.schoolsaas.dto.gamification.BadgeDto;
import com.schoolsaas.dto.gamification.LeaderboardEntryDto;
import com.schoolsaas.model.*;
import com.schoolsaas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GamificationService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final PointsTransactionRepository pointsRepository;
    private final UserRepository userRepository;

    @Transactional
    public Badge createBadge(UUID schoolId, BadgeDto dto) {
        Badge badge = Badge.builder()
                .schoolId(schoolId)
                .name(dto.getName())
                .description(dto.getDescription())
                .iconUrl(dto.getIconUrl())
                .color(dto.getColor())
                .criteriaType(dto.getCriteriaType())
                .criteriaValue(dto.getCriteriaValue())
                .pointsValue(dto.getPointsValue())
                .build();
        return badgeRepository.save(badge);
    }

    public List<BadgeDto> listBadges(UUID schoolId) {
        return badgeRepository.findBySchoolIdAndIsActiveTrue(schoolId).stream().map(this::mapBadgeDto).collect(Collectors.toList());
    }

    @Transactional
    public void awardBadge(UUID userId, UUID badgeId, UUID schoolId) {
        if (userBadgeRepository.existsByUserIdAndBadgeIdAndSchoolId(userId, badgeId, schoolId)) return;

        UserBadge ub = UserBadge.builder()
                .userId(userId)
                .badgeId(badgeId)
                .schoolId(schoolId)
                .build();
        userBadgeRepository.save(ub);

        Badge badge = badgeRepository.findById(badgeId).orElse(null);
        if (badge != null && badge.getPointsValue() > 0) {
            awardPoints(userId, schoolId, badge.getPointsValue(), "EARNED", "Earned badge: " + badge.getName(), "BADGE", badgeId);
        }
    }

    @Transactional
    public void awardPoints(UUID userId, UUID schoolId, Integer points, String type, String reason, String referenceType, UUID referenceId) {
        PointsTransaction tx = PointsTransaction.builder()
                .userId(userId)
                .schoolId(schoolId)
                .points(points)
                .transactionType(type)
                .reason(reason)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();
        pointsRepository.save(tx);
    }

    public List<LeaderboardEntryDto> getLeaderboard(UUID schoolId, int limit) {
        // This is a simplified implementation. In production, use a materialized view or cache.
        Map<UUID, Integer> pointsMap = new HashMap<>();
        Map<UUID, Integer> badgesMap = new HashMap<>();

        pointsRepository.findAll().stream()
                .filter(p -> p.getSchoolId().equals(schoolId))
                .forEach(p -> pointsMap.merge(p.getUserId(), p.getPoints(), Integer::sum));

        userBadgeRepository.findAll().stream()
                .filter(ub -> ub.getSchoolId().equals(schoolId))
                .forEach(ub -> badgesMap.merge(ub.getUserId(), 1, Integer::sum));

        Set<UUID> userIds = new HashSet<>();
        userIds.addAll(pointsMap.keySet());
        userIds.addAll(badgesMap.keySet());

        List<LeaderboardEntryDto> entries = new ArrayList<>();
        for (UUID userId : userIds) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) continue;
            LeaderboardEntryDto e = new LeaderboardEntryDto();
            e.setUserId(userId.toString());
            e.setFullName(user.getFullName());
            e.setAvatarUrl(user.getAvatarUrl());
            e.setTotalPoints(pointsMap.getOrDefault(userId, 0));
            e.setBadgesCount(badgesMap.getOrDefault(userId, 0));
            entries.add(e);
        }

        entries.sort((a, b) -> b.getTotalPoints().compareTo(a.getTotalPoints()));
        int rank = 1;
        for (LeaderboardEntryDto e : entries) {
            e.setRank(rank++);
        }
        return entries.stream().limit(limit).collect(Collectors.toList());
    }

    public Integer getUserPoints(UUID userId, UUID schoolId) {
        return pointsRepository.getTotalPointsByUserAndSchool(userId, schoolId);
    }

    public List<BadgeDto> getUserBadges(UUID userId, UUID schoolId) {
        return userBadgeRepository.findByUserIdAndSchoolId(userId, schoolId).stream().map(ub -> {
            Badge b = badgeRepository.findById(ub.getBadgeId()).orElse(null);
            if (b == null) return null;
            BadgeDto dto = mapBadgeDto(b);
            dto.setEarnedAt(ub.getEarnedAt());
            return dto;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private BadgeDto mapBadgeDto(Badge badge) {
        BadgeDto dto = new BadgeDto();
        dto.setId(badge.getId());
        dto.setName(badge.getName());
        dto.setDescription(badge.getDescription());
        dto.setIconUrl(badge.getIconUrl());
        dto.setColor(badge.getColor());
        dto.setCriteriaType(badge.getCriteriaType());
        dto.setCriteriaValue(badge.getCriteriaValue());
        dto.setPointsValue(badge.getPointsValue());
        return dto;
    }
}
