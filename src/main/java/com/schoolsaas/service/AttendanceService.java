package com.schoolsaas.service;

import com.schoolsaas.dto.attendance.AttendanceResponse;
import com.schoolsaas.dto.attendance.AttendanceSummary;
import com.schoolsaas.model.Attendance;
import com.schoolsaas.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    public List<AttendanceResponse> getStudentAttendance(UUID schoolId, UUID studentId) {
        List<Attendance> records = attendanceRepository.findBySchoolIdAndStudentId(schoolId, studentId);

        return records.stream().map(a -> AttendanceResponse.builder()
                .id(a.getId())
                .studentId(a.getStudentId())
                .studentName(a.getStudent() != null ? a.getStudent().getFullName() : null)
                .classId(a.getClassId())
                .date(a.getDate())
                .status(a.getStatus())
                .remarks(a.getRemarks())
                .build()
        ).collect(Collectors.toList());
    }

    public AttendanceSummary getStudentAttendanceSummary(UUID studentId) {
        long total = attendanceRepository.countByStudentId(studentId);
        long present = attendanceRepository.countByStudentIdAndStatus(studentId, "PRESENT");
        long absent = attendanceRepository.countByStudentIdAndStatus(studentId, "ABSENT");
        long late = attendanceRepository.countByStudentIdAndStatus(studentId, "LATE");
        long excused = attendanceRepository.countByStudentIdAndStatus(studentId, "EXCUSED");

        double percentage = total > 0 ? ((present + late) * 100.0) / total : 0;

        return AttendanceSummary.builder()
                .totalDays(total)
                .presentDays(present)
                .absentDays(absent)
                .lateDays(late)
                .excusedDays(excused)
                .attendancePercentage(Math.round(percentage * 100.0) / 100.0)
                .build();
    }
}
