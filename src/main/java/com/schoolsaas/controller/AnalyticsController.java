package com.schoolsaas.controller;

import com.schoolsaas.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/schools/{schoolId}/analytics/dashboard")
    @PreAuthorize("hasPermission(#schoolId, 'School', 'ANALYTICS_VIEW')")
    public ResponseEntity<Map<String, Object>> getDashboardStats(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(analyticsService.getDashboardStats(schoolId));
    }

    @GetMapping("/schools/{schoolId}/analytics/revenue-chart")
    @PreAuthorize("hasPermission(#schoolId, 'School', 'ANALYTICS_VIEW')")
    public ResponseEntity<List<Map<String, Object>>> getRevenueChart(
            @PathVariable UUID schoolId,
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(analyticsService.getRevenueChart(schoolId, months));
    }

    @GetMapping("/schools/{schoolId}/analytics/enrollment-trend")
    @PreAuthorize("hasPermission(#schoolId, 'School', 'ANALYTICS_VIEW')")
    public ResponseEntity<List<Map<String, Object>>> getEnrollmentTrend(
            @PathVariable UUID schoolId,
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(analyticsService.getEnrollmentTrend(schoolId, months));
    }

    @GetMapping("/schools/{schoolId}/analytics/gender-distribution")
    @PreAuthorize("hasPermission(#schoolId, 'School', 'ANALYTICS_VIEW')")
    public ResponseEntity<Map<String, Object>> getGenderDistribution(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(analyticsService.getGenderDistribution(schoolId));
    }

    @GetMapping("/schools/{schoolId}/analytics/class-distribution")
    @PreAuthorize("hasPermission(#schoolId, 'School', 'ANALYTICS_VIEW')")
    public ResponseEntity<List<Map<String, Object>>> getClassDistribution(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(analyticsService.getClassDistribution(schoolId));
    }

    @GetMapping("/schools/{schoolId}/analytics/payment-stats")
    @PreAuthorize("hasPermission(#schoolId, 'School', 'ANALYTICS_VIEW')")
    public ResponseEntity<Map<String, Object>> getPaymentStats(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(analyticsService.getPaymentStats(schoolId));
    }

    @GetMapping("/schools/{schoolId}/analytics/content-stats")
    @PreAuthorize("hasPermission(#schoolId, 'School', 'ANALYTICS_VIEW')")
    public ResponseEntity<Map<String, Object>> getContentStats(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(analyticsService.getContentStats(schoolId));
    }

    @GetMapping("/schools/{schoolId}/analytics/activity")
    @PreAuthorize("hasPermission(#schoolId, 'School', 'ANALYTICS_VIEW')")
    public ResponseEntity<List<Map<String, Object>>> getRecentActivity(
            @PathVariable UUID schoolId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getRecentActivity(schoolId, limit));
    }

    @GetMapping("/admin/analytics/platform")
    @PreAuthorize("hasRole('GENERAL_ADMIN') or hasRole('APP_ADMIN')")
    public ResponseEntity<Map<String, Object>> getPlatformStats() {
        return ResponseEntity.ok(analyticsService.getPlatformStats());
    }
}
