package com.schoolsaas.service;

import com.schoolsaas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final JdbcTemplate jdbcTemplate;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PaymentRepository paymentRepository;
    private final ContentItemRepository contentItemRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats(UUID schoolId) {
        Map<String, Object> stats = new HashMap<>();

        long totalStudents = studentRepository.countBySchoolIdAndStatus(schoolId, "ACTIVE");
        long totalTeachers = teacherRepository.countBySchoolId(schoolId);

        String revenueQuery = """
            SELECT COALESCE(SUM(amount), 0) as total
            FROM payments
            WHERE school_id = ? AND status = 'SUCCESS'
            AND created_at >= DATE_TRUNC('month', CURRENT_DATE)
            """;
        BigDecimal monthlyRevenue = jdbcTemplate.queryForObject(revenueQuery, BigDecimal.class, schoolId);

        long contentCount = contentItemRepository.countBySchoolId(schoolId);

        String prevMonthRevenueQuery = """
            SELECT COALESCE(SUM(amount), 0) as total
            FROM payments
            WHERE school_id = ? AND status = 'SUCCESS'
            AND created_at >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month')
            AND created_at < DATE_TRUNC('month', CURRENT_DATE)
            """;
        BigDecimal prevMonthRevenue = jdbcTemplate.queryForObject(prevMonthRevenueQuery, BigDecimal.class, schoolId);

        double revenueGrowth = prevMonthRevenue.compareTo(BigDecimal.ZERO) > 0
            ? ((monthlyRevenue.doubleValue() - prevMonthRevenue.doubleValue()) / prevMonthRevenue.doubleValue()) * 100
            : 0;

        stats.put("totalStudents", totalStudents);
        stats.put("totalTeachers", totalTeachers);
        stats.put("monthlyRevenue", monthlyRevenue);
        stats.put("contentCount", contentCount);
        stats.put("revenueGrowth", Math.round(revenueGrowth * 10) / 10.0);

        return stats;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRevenueChart(UUID schoolId, int months) {
        String query = """
            SELECT
                TO_CHAR(DATE_TRUNC('month', created_at), 'Mon YYYY') as month,
                COALESCE(SUM(amount), 0) as revenue
            FROM payments
            WHERE school_id = ? AND status = 'SUCCESS'
            AND created_at >= CURRENT_DATE - INTERVAL '1 month' * ?
            GROUP BY DATE_TRUNC('month', created_at)
            ORDER BY DATE_TRUNC('month', created_at)
            """;

        return jdbcTemplate.query(query, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("month", rs.getString("month"));
            row.put("revenue", rs.getBigDecimal("revenue"));
            return row;
        }, schoolId, months);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEnrollmentTrend(UUID schoolId, int months) {
        String query = """
            SELECT
                TO_CHAR(DATE_TRUNC('month', admission_date), 'Mon YYYY') as month,
                COUNT(*) as enrollments
            FROM students
            WHERE school_id = ?
            AND admission_date >= CURRENT_DATE - INTERVAL '1 month' * ?
            GROUP BY DATE_TRUNC('month', admission_date)
            ORDER BY DATE_TRUNC('month', admission_date)
            """;

        return jdbcTemplate.query(query, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("month", rs.getString("month"));
            row.put("enrollments", rs.getInt("enrollments"));
            return row;
        }, schoolId, months);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getGenderDistribution(UUID schoolId) {
        String query = """
            SELECT
                COALESCE(gender, 'Not Specified') as gender,
                COUNT(*) as count
            FROM students
            WHERE school_id = ? AND status = 'ACTIVE'
            GROUP BY gender
            """;

        List<Map<String, Object>> results = jdbcTemplate.query(query, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("name", rs.getString("gender"));
            row.put("value", rs.getInt("count"));
            return row;
        }, schoolId);

        return Map.of("data", results);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getClassDistribution(UUID schoolId) {
        String query = """
            SELECT
                c.name as class_name,
                COUNT(s.id) as student_count
            FROM classes c
            LEFT JOIN students s ON s.class_id = c.id AND s.status = 'ACTIVE'
            WHERE c.school_id = ?
            GROUP BY c.id, c.name
            ORDER BY c.name
            """;

        return jdbcTemplate.query(query, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("className", rs.getString("class_name"));
            row.put("students", rs.getInt("student_count"));
            return row;
        }, schoolId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentStats(UUID schoolId) {
        String query = """
            SELECT
                status,
                COUNT(*) as count,
                COALESCE(SUM(amount), 0) as total
            FROM payments
            WHERE school_id = ?
            AND created_at >= DATE_TRUNC('month', CURRENT_DATE)
            GROUP BY status
            """;

        List<Map<String, Object>> results = jdbcTemplate.query(query, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("status", rs.getString("status"));
            row.put("count", rs.getInt("count"));
            row.put("total", rs.getBigDecimal("total"));
            return row;
        }, schoolId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("breakdown", results);

        BigDecimal totalCollected = results.stream()
            .filter(r -> "SUCCESS".equals(r.get("status")))
            .map(r -> (BigDecimal) r.get("total"))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalTransactions = results.stream()
            .mapToInt(r -> (Integer) r.get("count"))
            .sum();

        stats.put("totalCollected", totalCollected);
        stats.put("totalTransactions", totalTransactions);

        return stats;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getContentStats(UUID schoolId) {
        String query = """
            SELECT
                status,
                COUNT(*) as count
            FROM content_items
            WHERE school_id = ?
            GROUP BY status
            """;

        List<Map<String, Object>> results = jdbcTemplate.query(query, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("status", rs.getString("status"));
            row.put("count", rs.getInt("count"));
            return row;
        }, schoolId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("breakdown", results);

        long totalContent = results.stream()
            .mapToInt(r -> (Integer) r.get("count"))
            .sum();

        stats.put("total", totalContent);

        return stats;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentActivity(UUID schoolId, int limit) {
        List<Map<String, Object>> activities = new ArrayList<>();

        String studentsQuery = """
            SELECT 'enrollment' as type, full_name as detail, created_at
            FROM students WHERE school_id = ?
            ORDER BY created_at DESC LIMIT ?
            """;
        activities.addAll(jdbcTemplate.query(studentsQuery, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("type", "enrollment");
            row.put("detail", "New student: " + rs.getString("detail"));
            row.put("time", rs.getTimestamp("created_at").toLocalDateTime());
            return row;
        }, schoolId, limit / 3));

        String paymentsQuery = """
            SELECT 'payment' as type, amount as detail, created_at
            FROM payments WHERE school_id = ? AND status = 'SUCCESS'
            ORDER BY created_at DESC LIMIT ?
            """;
        activities.addAll(jdbcTemplate.query(paymentsQuery, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("type", "payment");
            row.put("detail", "Payment received: ₦" + rs.getBigDecimal("detail"));
            row.put("time", rs.getTimestamp("created_at").toLocalDateTime());
            return row;
        }, schoolId, limit / 3));

        String contentQuery = """
            SELECT 'content' as type, title as detail, created_at
            FROM content_items WHERE school_id = ?
            ORDER BY created_at DESC LIMIT ?
            """;
        activities.addAll(jdbcTemplate.query(contentQuery, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("type", "content");
            row.put("detail", "Content created: " + rs.getString("detail"));
            row.put("time", rs.getTimestamp("created_at").toLocalDateTime());
            return row;
        }, schoolId, limit / 3));

        activities.sort((a, b) -> ((LocalDateTime) b.get("time")).compareTo((LocalDateTime) a.get("time")));

        return activities.subList(0, Math.min(limit, activities.size()));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPlatformStats() {
        Map<String, Object> stats = new HashMap<>();

        String schoolsQuery = "SELECT COUNT(*) FROM schools WHERE status = 'ACTIVE'";
        stats.put("totalSchools", jdbcTemplate.queryForObject(schoolsQuery, Long.class));

        String studentsQuery = "SELECT COUNT(*) FROM students WHERE status = 'ACTIVE'";
        stats.put("totalStudents", jdbcTemplate.queryForObject(studentsQuery, Long.class));

        String teachersQuery = "SELECT COUNT(*) FROM teachers";
        stats.put("totalTeachers", jdbcTemplate.queryForObject(teachersQuery, Long.class));

        String revenueQuery = """
            SELECT COALESCE(SUM(amount), 0)
            FROM payments WHERE status = 'SUCCESS'
            AND created_at >= DATE_TRUNC('month', CURRENT_DATE)
            """;
        stats.put("monthlyRevenue", jdbcTemplate.queryForObject(revenueQuery, BigDecimal.class));

        return stats;
    }
}
