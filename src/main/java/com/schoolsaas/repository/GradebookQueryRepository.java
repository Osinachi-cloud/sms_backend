package com.schoolsaas.repository;

import com.schoolsaas.dto.gradebook.GradebookEntryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class GradebookQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String BASE_SQL = """
        SELECT * FROM (
            SELECT
                g.id::varchar AS entry_id,
                g.student_id::varchar AS student_id,
                s.full_name AS student_name,
                s.admission_number,
                s.class_id::varchar AS class_id,
                COALESCE(cl.name, '') AS class_name,
                g.subject_id::varchar AS subject_id,
                COALESCE(subj.name, '') AS subject_name,
                g.assessment_type AS source_title,
                'GRADE' AS source_type,
                g.assessment_type,
                g.score,
                g.max_score,
                g.grade_letter,
                g.term_id::varchar AS term_id,
                t.name AS term_name,
                g.session_id::varchar AS session_id,
                ses.name AS session_name,
                g.created_at
            FROM grades g
            JOIN students s ON s.id = g.student_id
            LEFT JOIN classes cl ON cl.id = s.class_id
            LEFT JOIN subjects subj ON subj.id = g.subject_id
            LEFT JOIN terms t ON t.id = g.term_id
            LEFT JOIN academic_sessions ses ON ses.id = g.session_id
            WHERE g.school_id = :schoolId

            UNION ALL

            SELECT
                ascore.id::varchar AS entry_id,
                ascore.student_id::varchar AS student_id,
                s.full_name AS student_name,
                s.admission_number,
                s.class_id::varchar AS class_id,
                COALESCE(cl.name, '') AS class_name,
                a.subject_id::varchar AS subject_id,
                COALESCE(subj.name, '') AS subject_name,
                a.title AS source_title,
                'ASSESSMENT' AS source_type,
                a.assessment_type,
                ascore.score,
                a.max_score,
                NULL AS grade_letter,
                a.term_id::varchar AS term_id,
                t.name AS term_name,
                a.session_id::varchar AS session_id,
                ses.name AS session_name,
                ascore.created_at
            FROM assessment_scores ascore
            JOIN teacher_assessments a ON a.id = ascore.assessment_id
            JOIN students s ON s.id = ascore.student_id
            LEFT JOIN classes cl ON cl.id = s.class_id
            LEFT JOIN subjects subj ON subj.id = a.subject_id
            LEFT JOIN terms t ON t.id = a.term_id
            LEFT JOIN academic_sessions ses ON ses.id = a.session_id
            WHERE a.school_id = :schoolId
        ) combined
        WHERE 1=1
        """;

    private static final RowMapper<GradebookEntryDto> ROW_MAPPER = (rs, rowNum) -> {
        BigDecimal score = rs.getBigDecimal("score");
        BigDecimal maxScore = rs.getBigDecimal("max_score");
        BigDecimal pct = null;
        if (score != null && maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
            pct = score.multiply(new BigDecimal("100")).divide(maxScore, 2, java.math.RoundingMode.HALF_UP);
        }
        String gradeLetter = rs.getString("grade_letter");
        if (gradeLetter == null && pct != null) {
            double p = pct.doubleValue();
            gradeLetter = p >= 70 ? "A" : p >= 60 ? "B" : p >= 50 ? "C" : p >= 45 ? "D" : "F";
        }
        Timestamp createdAt = rs.getTimestamp("created_at");
        return GradebookEntryDto.builder()
                .id(UUID.fromString(rs.getString("entry_id")))
                .studentId(UUID.fromString(rs.getString("student_id")))
                .studentName(rs.getString("student_name"))
                .admissionNumber(rs.getString("admission_number"))
                .classId(parseUuid(rs.getString("class_id")))
                .className(rs.getString("class_name"))
                .subjectId(parseUuid(rs.getString("subject_id")))
                .subjectName(rs.getString("subject_name"))
                .sourceTitle(rs.getString("source_title"))
                .sourceType(rs.getString("source_type"))
                .assessmentType(rs.getString("assessment_type"))
                .score(score)
                .maxScore(maxScore)
                .percentage(pct)
                .gradeLetter(gradeLetter)
                .termId(parseUuid(rs.getString("term_id")))
                .termName(rs.getString("term_name"))
                .sessionId(parseUuid(rs.getString("session_id")))
                .sessionName(rs.getString("session_name"))
                .createdAt(createdAt != null ? createdAt.toLocalDateTime() : null)
                .build();
    };

    private static UUID parseUuid(String s) {
        return s == null || s.isBlank() ? null : UUID.fromString(s);
    }

    public List<GradebookEntryDto> findEntries(
            UUID schoolId,
            UUID classId,
            UUID subjectId,
            UUID studentId,
            UUID termId,
            UUID sessionId,
            String search,
            List<UUID> allowedClassIds,
            Pageable pageable) {

        StringBuilder sql = new StringBuilder(BASE_SQL);
        MapSqlParameterSource params = new MapSqlParameterSource("schoolId", schoolId);

        applyFilters(sql, params, classId, subjectId, studentId, termId, sessionId, search, allowedClassIds);

        sql.append(" ORDER BY created_at DESC LIMIT :limit OFFSET :offset");
        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        return jdbcTemplate.query(sql.toString(), params, ROW_MAPPER);
    }

    public long countEntries(
            UUID schoolId,
            UUID classId,
            UUID subjectId,
            UUID studentId,
            UUID termId,
            UUID sessionId,
            String search,
            List<UUID> allowedClassIds) {

        String sql = "SELECT COUNT(*) FROM (" + BASE_SQL + ") c WHERE 1=1";
        StringBuilder filterSql = new StringBuilder(sql);
        MapSqlParameterSource params = new MapSqlParameterSource("schoolId", schoolId);

        applyFilters(filterSql, params, classId, subjectId, studentId, termId, sessionId, search, allowedClassIds);

        Long count = jdbcTemplate.queryForObject(filterSql.toString(), params, Long.class);
        return count != null ? count : 0L;
    }

    private void applyFilters(StringBuilder sql, MapSqlParameterSource params,
                              UUID classId, UUID subjectId, UUID studentId,
                              UUID termId, UUID sessionId, String search,
                              List<UUID> allowedClassIds) {
        if (classId != null) {
            sql.append(" AND class_id = :classId");
            params.addValue("classId", classId.toString());
        }
        if (allowedClassIds != null && !allowedClassIds.isEmpty()) {
            sql.append(" AND class_id IN (:allowedClassIds)");
            params.addValue("allowedClassIds", allowedClassIds.stream().map(UUID::toString).toList());
        }
        if (subjectId != null) {
            sql.append(" AND subject_id = :subjectId");
            params.addValue("subjectId", subjectId.toString());
        }
        if (studentId != null) {
            sql.append(" AND student_id = :studentId");
            params.addValue("studentId", studentId.toString());
        }
        if (termId != null) {
            sql.append(" AND term_id = :termId");
            params.addValue("termId", termId.toString());
        }
        if (sessionId != null) {
            sql.append(" AND session_id = :sessionId");
            params.addValue("sessionId", sessionId.toString());
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (" +
                "LOWER(student_name) LIKE LOWER(:search) OR " +
                "LOWER(admission_number) LIKE LOWER(:search) OR " +
                "LOWER(source_title) LIKE LOWER(:search) OR " +
                "LOWER(subject_name) LIKE LOWER(:search) OR " +
                "LOWER(class_name) LIKE LOWER(:search))");
            params.addValue("search", "%" + search + "%");
        }
    }
}
