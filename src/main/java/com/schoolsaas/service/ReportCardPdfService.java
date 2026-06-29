package com.schoolsaas.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportCardPdfService {

    public byte[] generateReportCardPdf(Map<String, Object> report) throws Exception {
        String html = buildHtml(report);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        }
    }

    private String buildHtml(Map<String, Object> report) {
        Map<String, Object> school = (Map<String, Object>) report.getOrDefault("school", Map.of());
        Map<String, Object> student = (Map<String, Object>) report.getOrDefault("student", Map.of());
        Map<String, Object> term = (Map<String, Object>) report.getOrDefault("term", Map.of());
        Map<String, Object> attendance = (Map<String, Object>) report.getOrDefault("attendance", Map.of());
        List<Map<String, Object>> subjects = (List<Map<String, Object>>) report.getOrDefault("subjects", List.of());
        List<Map<String, Object>> affective = (List<Map<String, Object>>) report.getOrDefault("affective_domain", List.of());
        List<Map<String, Object>> gradingScale = (List<Map<String, Object>>) report.getOrDefault("grading_scale", List.of());
        Object overallAverage = report.get("overall_average");
        Object overallGrade = report.get("overall_grade");
        String teacherComment = String.valueOf(report.getOrDefault("teacher_comment", ""));
        String principalComment = String.valueOf(report.getOrDefault("principal_comment", ""));

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'/><style>");
        sb.append("body{font-family:Arial,sans-serif;margin:0;padding:20px;color:#333;}");
        sb.append(".header{background:linear-gradient(135deg,#3b82f6,#8b5cf6);color:#fff;padding:24px;border-radius:12px 12px 0 0;}");
        sb.append(".header h1{margin:0;font-size:28px;}");
        sb.append(".header p{margin:4px 0 0;font-size:12px;opacity:.9;}");
        sb.append(".info-section{padding:16px 0;border-bottom:1px solid #e5e7eb;}");
        sb.append(".grid-2{display:flex;justify-content:space-between;}");
        sb.append(".grid-2 div{flex:1;}");
        sb.append("table{width:100%;border-collapse:collapse;font-size:12px;margin-top:8px;}");
        sb.append("th,td{padding:8px 10px;text-align:left;border-bottom:1px solid #e5e7eb;}");
        sb.append("th{background:#f8fafc;font-weight:700;font-size:10px;text-transform:uppercase;letter-spacing:.5px;color:#64748b;}");
        sb.append("td{text-align:center;}");
        sb.append("td:first-child{text-align:left;font-weight:600;}");
        sb.append(".grade-badge{display:inline-block;padding:2px 8px;border-radius:6px;font-weight:bold;font-size:11px;color:#fff;}");
        sb.append(".grade-a{background:#10b981;}");
        sb.append(".grade-b{background:#3b82f6;}");
        sb.append(".grade-c{background:#f59e0b;}");
        sb.append(".grade-d{background:#f97316;}");
        sb.append(".grade-f{background:#ef4444;}");
        sb.append(".scale-strip{background:#f8fafc;padding:8px 16px;font-size:10px;border-bottom:1px solid #e5e7eb;}");
        sb.append(".scale-badge{display:inline-block;margin-right:8px;padding:2px 6px;border-radius:4px;font-size:9px;font-weight:bold;border:1px solid #cbd5e1;}");
        sb.append(".affective-grid{display:flex;flex-wrap:wrap;gap:6px;}");
        sb.append(".affective-item{flex:0 0 48%;display:flex;justify-content:space-between;padding:6px 10px;border:1px solid #e5e7eb;border-radius:6px;font-size:11px;}");
        sb.append(".comment-box{background:#f8fafc;padding:10px;border-radius:8px;font-size:12px;border:1px solid #e5e7eb;min-height:50px;}");
        sb.append(".signature{margin-top:40px;border-top:2px solid #94a3b8;padding-top:8px;text-align:center;font-size:12px;}");
        sb.append(".stats-grid{display:flex;gap:10px;}");
        sb.append(".stat-box{flex:1;text-align:center;padding:10px;border-radius:8px;font-size:12px;}");
        sb.append("</style></head><body>");

        // Header
        sb.append("<div class='header'>");
        sb.append("<h1>").append(esc(school.get("name"))).append("</h1>");
        sb.append("<p>").append(esc(school.get("address"))).append(" &bull; ").append(esc(school.get("phone"))).append(" &bull; ").append(esc(school.get("email"))).append("</p>");
        sb.append("<p style='text-align:right;margin-top:-20px;font-weight:bold;'>").append(esc(term.get("name"))).append("</p>");
        sb.append("</div>");

        // Grading scale strip
        sb.append("<div class='scale-strip'>");
        sb.append("<strong>GRADING SCALE:</strong> ");
        for (Map<String, Object> s : gradingScale) {
            sb.append("<span class='scale-badge'>").append(s.get("grade")).append(": ").append(s.get("minScore")).append("-").append(s.get("maxScore")).append("% (").append(s.get("label")).append(")</span>");
        }
        sb.append("</div>");

        // Student info
        sb.append("<div class='info-section'><div class='grid-2'>");
        sb.append("<div><strong>Student:</strong> ").append(esc(student.get("name"))).append("<br/>");
        sb.append("<strong>Admission No:</strong> ").append(esc(student.get("admission_number"))).append("<br/>");
        sb.append("<strong>Class:</strong> ").append(esc(student.get("class_name"))).append("</div>");
        sb.append("<div style='text-align:right;'><strong>Overall Average:</strong> ").append(overallAverage != null ? overallAverage : "-").append("%<br/>");
        sb.append("<strong>Overall Grade:</strong> ").append(overallGrade != null ? overallGrade : "-").append("</div>");
        sb.append("</div></div>");

        // Subjects table
        sb.append("<div class='info-section'><h3 style='font-size:14px;margin:0 0 8px;'>Academic Performance</h3>");
        if (!subjects.isEmpty()) {
            List<String> colNames = subjects.get(0).get("components") != null
                    ? ((List<Map<String, Object>>) subjects.get(0).get("components")).stream().map(c -> String.valueOf(c.get("component_name"))).toList()
                    : List.of();
            sb.append("<table><thead><tr><th>Subject</th>");
            for (String cn : colNames) sb.append("<th>").append(cn).append("</th>");
            sb.append("<th>Total</th><th>Grade</th></tr></thead><tbody>");
            for (Map<String, Object> sub : subjects) {
                Object total = sub.get("total");
                String grade = total != null ? computeGradeLetter(((Number) total).doubleValue(), gradingScale) : "-";
                String gradeClass = grade.equals("A") ? "grade-a" : grade.equals("B") ? "grade-b" : grade.equals("C") ? "grade-c" : grade.equals("D") ? "grade-d" : "grade-f";
                sb.append("<tr><td>").append(esc(sub.get("subject_name"))).append("</td>");
                List<Map<String, Object>> comps = sub.get("components") != null ? (List<Map<String, Object>>) sub.get("components") : List.of();
                for (String cn : colNames) {
                    Map<String, Object> comp = comps.stream().filter(c -> cn.equals(c.get("component_name"))).findFirst().orElse(null);
                    if (comp != null && comp.get("score") != null) {
                        sb.append("<td>").append(comp.get("score")).append(" / ").append(comp.get("weight")).append("</td>");
                    } else {
                        sb.append("<td style='color:#94a3b8'>...</td>");
                    }
                }
                sb.append("<td><strong>").append(total != null ? total : "...").append("</strong></td>");
                sb.append("<td><span class='grade-badge ").append(gradeClass).append("'>").append(grade).append("</span></td></tr>");
            }
            sb.append("</tbody></table>");
        } else {
            sb.append("<p style='text-align:center;color:#94a3b8;'>No subject results available.</p>");
        }
        sb.append("</div>");

        // Affective + attendance
        sb.append("<div class='info-section'><div class='grid-2'>");
        sb.append("<div style='flex:2;margin-right:16px;'><h3 style='font-size:14px;margin:0 0 8px;'>Affective Domain</h3><div class='affective-grid'>");
        for (Map<String, Object> af : affective) {
            sb.append("<div class='affective-item'><span>").append(af.get("trait")).append("</span><span>");
            Object r = af.get("rating");
            sb.append(r != null ? r + " / 5" : "-");
            sb.append("</span></div>");
        }
        sb.append("</div></div>");
        sb.append("<div style='flex:1;'><h3 style='font-size:14px;margin:0 0 8px;'>Attendance</h3><div class='stats-grid'>");
        sb.append("<div class='stat-box' style='background:#ecfdf5;'>").append(attendance.getOrDefault("present", 0)).append("<br/><span style='font-size:9px;color:#059669;'>Present</span></div>");
        sb.append("<div class='stat-box' style='background:#fef2f2;'>").append(attendance.getOrDefault("absent", 0)).append("<br/><span style='font-size:9px;color:#dc2626;'>Absent</span></div>");
        sb.append("<div class='stat-box' style='background:#fffbeb;'>").append(attendance.getOrDefault("late", 0)).append("<br/><span style='font-size:9px;color:#d97706;'>Late</span></div>");
        sb.append("</div></div>");
        sb.append("</div></div>");

        // Comments
        sb.append("<div class='info-section' style='display:flex;gap:16px;'>");
        sb.append("<div style='flex:1;'><h3 style='font-size:12px;margin:0 0 6px;text-transform:uppercase;letter-spacing:.5px;color:#64748b;'>Teacher's Comment</h3><div class='comment-box'>").append(esc(teacherComment.isEmpty() ? "No comment" : teacherComment)).append("</div></div>");
        sb.append("<div style='flex:1;'><h3 style='font-size:12px;margin:0 0 6px;text-transform:uppercase;letter-spacing:.5px;color:#64748b;'>Principal's Comment</h3><div class='comment-box'>").append(esc(principalComment.isEmpty() ? "No comment" : principalComment)).append("</div></div>");
        sb.append("</div>");

        // Signatures
        sb.append("<div style='display:flex;gap:40px;margin-top:24px;'>");
        sb.append("<div class='signature' style='flex:1;'>Class Teacher's Signature</div>");
        sb.append("<div class='signature' style='flex:1;'>Principal's Signature</div>");
        sb.append("<div class='signature' style='flex:1;'>Parent/Guardian's Signature</div>");
        sb.append("</div>");

        sb.append("<p style='text-align:center;margin-top:16px;font-size:9px;color:#94a3b8;text-transform:uppercase;letter-spacing:1px;'>Powered by School SaaS &bull; Official Academic Report</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String esc(Object o) {
        if (o == null) return "";
        return o.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String computeGradeLetter(double score, List<Map<String, Object>> scale) {
        List<Map<String, Object>> sorted = scale.stream()
                .sorted((a, b) -> {
                    int minA = a.get("minScore") instanceof Number ? ((Number) a.get("minScore")).intValue() : 0;
                    int minB = b.get("minScore") instanceof Number ? ((Number) b.get("minScore")).intValue() : 0;
                    return Integer.compare(minB, minA);
                }).toList();
        for (Map<String, Object> entry : sorted) {
            int min = entry.get("minScore") instanceof Number ? ((Number) entry.get("minScore")).intValue() : 0;
            int max = entry.get("maxScore") instanceof Number ? ((Number) entry.get("maxScore")).intValue() : 100;
            if (score >= min && score <= max) return String.valueOf(entry.get("grade"));
        }
        return "F";
    }
}
