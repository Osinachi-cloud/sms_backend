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

    @SuppressWarnings("unchecked")
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

        // Single dark-blue accent only
        String darkBlue = "#0b1d3a";

        List<String> allComponentNames = subjects.stream()
                .flatMap(sub -> ((List<Map<String, Object>>) sub.getOrDefault("components", List.of())).stream())
                .map(c -> String.valueOf(c.get("component_name")))
                .distinct()
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'/><style>");
        sb.append("@page { size: A4 portrait; margin: 10mm; }");
        sb.append("body { font-family: 'Segoe UI', Arial, sans-serif; font-size: 9px; line-height: 1.3; color: #000; background: #fff; }");
        sb.append("table { border-collapse: collapse; width: 100%; }");
        sb.append("td, th { padding: 0; vertical-align: middle; }");
        sb.append("p { margin: 0 0 6px 0; }");

        // Header banner
        sb.append(".banner { background: ").append(darkBlue).append("; color: #fff; }");
        sb.append(".banner td { padding: 12px 14px; }");
        sb.append(".banner .school { font-size: 17px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.5px; }");
        sb.append(".banner .meta  { font-size: 8px; opacity: 0.85; margin-top: 2px; }");
        sb.append(".banner .right { text-align: right; }");
        sb.append(".banner .term  { font-size: 12px; font-weight: 700; }");
        sb.append(".banner .badge { font-size: 7px; text-transform: uppercase; letter-spacing: 2px; opacity: 0.7; font-weight: 700; }");

        // Section heading (standalone paragraph, not inside tables)
        sb.append(".sh { font-size: 8px; font-weight: 700; color: ").append(darkBlue).append("; text-transform: uppercase; letter-spacing: 1px; border-bottom: 1.5px solid ").append(darkBlue).append("; padding-bottom: 3px; margin: 10px 0 5px 0; }");
        sb.append(".sh-small { font-size: 7px; font-weight: 700; color: ").append(darkBlue).append("; text-transform: uppercase; letter-spacing: 1px; border-bottom: 1px solid ").append(darkBlue).append("; padding-bottom: 2px; margin: 6px 0 4px 0; }");

        // Info grid
        sb.append(".info td { border: 1px solid #000; padding: 5px 8px; font-size: 9px; }");
        sb.append(".info .lb { background: #f5f7fa; font-weight: 700; font-size: 7px; text-transform: uppercase; letter-spacing: 0.5px; width: 18%; }");
        sb.append(".info .va { font-weight: 600; }");
        sb.append(".info .hl { background: #f5f7fa; }");
        sb.append(".bignum { font-size: 15px; font-weight: 900; color: ").append(darkBlue).append("; }");
        sb.append(".biggrade { display: inline-block; width: 22px; height: 22px; font-size: 10px; font-weight: 900; line-height: 22px; text-align: center; background: ").append(darkBlue).append("; color: #fff; }");

        // Subject table
        sb.append(".subj { border: 1px solid #000; }");
        sb.append(".subj th { background: ").append(darkBlue).append("; color: #fff; padding: 5px 7px; font-size: 7px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; text-align: center; border: 1px solid ").append(darkBlue).append("; }");
        sb.append(".subj td { border: 1px solid #000; padding: 4px 7px; font-size: 8.5px; text-align: center; }");
        sb.append(".subj td:first-child { text-align: left; font-weight: 600; }");
        sb.append(".subj tr:nth-child(even) { background: #f9fafb; }");

        // Affective table
        sb.append(".aff { border: 1px solid #000; }");
        sb.append(".aff td { border: 1px solid #000; padding: 4px 7px; font-size: 8.5px; }");
        sb.append(".aff th { background: #f5f7fa; border: 1px solid #000; padding: 4px 7px; font-size: 7px; font-weight: 700; text-transform: uppercase; }");

        // Comments
        sb.append(".cmt { border: 1px solid #000; padding: 8px 10px; font-size: 8.5px; color: #222; font-style: italic; min-height: 36px; background: #fff; }");

        // Signatures
        sb.append(".sig td { text-align: center; padding: 0 8px; }");
        sb.append(".sig-line { border-top: 1px solid #000; padding-top: 4px; margin-top: 28px; }");
        sb.append(".sig-role { font-size: 8px; font-weight: 700; text-transform: uppercase; }");
        sb.append(".sig-name { font-size: 7px; color: #444; margin-top: 2px; }");

        // Footer
        sb.append(".foot { text-align: center; font-size: 7px; color: #555; padding-top: 5px; border-top: 1px solid #000; margin-top: 6px; }");

        sb.append(".grey { color: #666; font-size: 8px; }");
        sb.append(".scale { font-size: 8px; font-weight: 600; margin-right: 10px; }");
        sb.append(".att-cell { border: 1px solid #000; text-align: center; padding: 6px 4px; }");
        sb.append(".att-num { font-size: 13px; font-weight: 900; color: ").append(darkBlue).append("; }");
        sb.append(".att-lbl { font-size: 7px; font-weight: 700; text-transform: uppercase; color: #333; }");

        sb.append("</style></head><body>");

        // ===== HEADER =====
        sb.append("<table class='banner'><tr>");
        sb.append("<td>");
        sb.append("<p class='school'>").append(esc(school.get("name"))).append("</p>");
        sb.append("<p class='meta'>").append(esc(school.get("address"))).append(" &#8226; Tel: ").append(esc(school.get("phone"))).append(" &#8226; Email: ").append(esc(school.get("email"))).append("</p>");
        sb.append("</td><td class='right'>");
        sb.append("<p class='badge'>Academic Report</p>");
        sb.append("<p class='term'>").append(esc(term.get("name"))).append("</p>");
        sb.append("<p class='meta'>").append(esc(term.get("session_name"))).append("</p>");
        sb.append("</td></tr></table>");

        // ===== STUDENT INFO =====
        sb.append("<table class='info' style='margin-top: 8px;'><tr>");
        sb.append("<td class='lb'>Student Name</td><td class='va'>").append(esc(student.get("name"))).append("</td>");
        sb.append("<td class='lb'>Admission No</td><td class='va'>").append(esc(student.get("admission_number"))).append("</td>");
        sb.append("</tr><tr>");
        sb.append("<td class='lb'>Class</td><td class='va'>").append(esc(student.get("class_name"))).append("</td>");
        sb.append("<td class='lb'>Gender</td><td class='va'>").append(esc(student.get("gender"))).append("</td>");
        sb.append("</tr><tr>");
        sb.append("<td class='lb'>Date of Birth</td><td class='va'>").append(esc(student.get("date_of_birth"))).append("</td>");
        sb.append("<td class='lb'>Class Teacher</td><td class='va'>").append(esc(student.get("class_teacher_name"))).append("</td>");
        sb.append("</tr><tr class='hl'>");
        sb.append("<td class='lb' style='color:").append(darkBlue).append("; font-size:8px;'>Overall Average</td>");
        sb.append("<td class='va'><span class='bignum'>").append(overallAverage != null ? overallAverage : "-").append("%</span></td>");
        sb.append("<td class='lb' style='color:").append(darkBlue).append("; font-size:8px;'>Overall Grade</td>");
        sb.append("<td class='va'>");
        if (overallGrade != null) {
            sb.append("<span class='biggrade'>").append(overallGrade).append("</span>");
        } else {
            sb.append("-");
        }
        sb.append("</td></tr></table>");

        // ===== GRADING SCALE =====
        sb.append("<p style='margin-top: 6px; padding: 3px 0; border-bottom: 1px solid #000; font-size: 8px;'>");
        sb.append("<span class='scale'>Grading Scale:</span>");
        for (Map<String, Object> s : gradingScale) {
            sb.append("<span class='scale'>");
            sb.append(esc(s.get("grade"))).append(": ").append(esc(s.get("minScore"))).append("-").append(esc(s.get("maxScore"))).append("% (").append(esc(s.get("label"))).append(")");
            sb.append("</span>");
        }
        sb.append("</p>");

        // ===== ACADEMIC PERFORMANCE =====
        sb.append("<p class='sh'>Academic Performance</p>");
        sb.append("<table class='subj'><thead><tr>");
        sb.append("<th style='text-align:left;'>Subject</th>");
        for (String cn : allComponentNames) {
            sb.append("<th>").append(esc(cn)).append("</th>");
        }
        sb.append("<th>Total</th><th>Grade</th></tr></thead><tbody>");
        for (Map<String, Object> sub : subjects) {
            Object total = sub.get("total");
            String grade = total != null ? computeGradeLetter(((Number) total).doubleValue(), gradingScale) : "-";
            sb.append("<tr><td>").append(esc(sub.get("subject_name"))).append("</td>");
            List<Map<String, Object>> comps = sub.get("components") != null ? (List<Map<String, Object>>) sub.get("components") : List.of();
            for (String cn : allComponentNames) {
                Map<String, Object> comp = comps.stream().filter(c -> cn.equals(c.get("component_name"))).findFirst().orElse(null);
                if (comp != null && comp.get("score") != null) {
                    sb.append("<td><strong>").append(esc(comp.get("score"))).append("</strong> <span class='grey'>/").append(esc(comp.get("weight"))).append("</span></td>");
                } else {
                    sb.append("<td class='grey'>-</td>");
                }
            }
            sb.append("<td><strong>").append(total != null ? total : "-").append("</strong></td>");
            sb.append("<td><strong>").append(grade).append("</strong></td></tr>");
        }
        sb.append("</tbody></table>");

        // ===== AFFECTIVE + ATTENDANCE (side by side) =====
        sb.append("<table style='margin-top: 10px; page-break-inside: avoid;'><tr>");

        // Affective
        sb.append("<td style='width: 55%; padding-right: 8px; vertical-align: top;'>");
        sb.append("<p class='sh-small'>Affective Domain</p>");
        if (!affective.isEmpty()) {
            sb.append("<table class='aff'><tr><th style='text-align:left;'>Trait</th><th style='text-align:center;'>Rating</th><th style='text-align:center;'>Remark</th></tr>");
            for (Map<String, Object> item : affective) {
                Object rating = item.get("rating");
                sb.append("<tr>");
                sb.append("<td>").append(esc(item.get("trait"))).append("</td>");
                sb.append("<td style='text-align:center;'>").append(rating != null ? rating : "-").append("</td>");
                sb.append("<td style='text-align:center;'>").append(getRatingLabel(rating)).append("</td>");
                sb.append("</tr>");
            }
            sb.append("</table>");
        } else {
            sb.append("<p style='font-size: 8px; padding: 6px; border: 1px solid #000; color: #555; margin:0;'>No affective ratings available.</p>");
        }
        sb.append("</td>");

        // Attendance
        sb.append("<td style='width: 45%; vertical-align: top;'>");
        sb.append("<p class='sh-small'>Attendance Summary</p>");
        sb.append("<table><tr>");
        sb.append("<td style='padding: 3px;'><p class='att-cell'><span class='att-num'>").append(attendance.getOrDefault("present", 0)).append("</span><br/><span class='att-lbl'>Present</span></p></td>");
        sb.append("<td style='padding: 3px;'><p class='att-cell'><span class='att-num'>").append(attendance.getOrDefault("absent", 0)).append("</span><br/><span class='att-lbl'>Absent</span></p></td>");
        sb.append("</tr><tr>");
        sb.append("<td style='padding: 3px;'><p class='att-cell'><span class='att-num'>").append(attendance.getOrDefault("late", 0)).append("</span><br/><span class='att-lbl'>Late</span></p></td>");
        sb.append("<td style='padding: 3px;'><p class='att-cell'><span class='att-num'>").append(attendance.getOrDefault("percentage", 0)).append("%</span><br/><span class='att-lbl'>Attendance</span></p></td>");
        sb.append("</tr></table>");
        sb.append("</td></tr></table>");

        // ===== COMMENTS =====
        sb.append("<table style='margin-top: 10px; page-break-inside: avoid;'><tr>");
        sb.append("<td style='width: 50%; padding-right: 6px; vertical-align: top;'>");
        sb.append("<p class='sh-small'>Teacher's Comment</p>");
        sb.append("<p class='cmt'>").append(esc(teacherComment.isEmpty() ? "No comment provided." : teacherComment)).append("</p>");
        sb.append("</td><td style='width: 50%; padding-left: 6px; vertical-align: top;'>");
        sb.append("<p class='sh-small'>Principal's Comment</p>");
        sb.append("<p class='cmt'>").append(esc(principalComment.isEmpty() ? "No comment provided." : principalComment)).append("</p>");
        sb.append("</td></tr></table>");

        // ===== SIGNATURES =====
        sb.append("<table class='sig' style='margin-top: 14px; page-break-inside: avoid;'><tr>");
        sb.append("<td><p class='sig-line'><span class='sig-role'>Class Teacher's Signature</span><br/><span class='sig-name'>").append(esc(student.get("class_teacher_name"))).append("</span></p></td>");
        sb.append("<td><p class='sig-line'><span class='sig-role'>Principal's Signature</span><br/><span class='sig-name'>Date: ").append(java.time.LocalDate.now()).append("</span></p></td>");
        sb.append("<td><p class='sig-line'><span class='sig-role'>Parent / Guardian's Signature</span><br/><span class='sig-name'>").append(esc(student.get("parent_name"))).append("</span></p></td>");
        sb.append("</tr></table>");

        sb.append("<p class='foot'>Powered by School SaaS &#8212; Official Academic Report</p>");
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

    private String getRatingLabel(Object rating) {
        if (rating == null) return "-";
        int n = ((Number) rating).intValue();
        if (n >= 80) return "Excellent";
        if (n >= 70) return "Very Good";
        if (n >= 60) return "Good";
        if (n >= 50) return "Fair";
        return "Poor";
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
