package com.schoolsaas.dto.timetable;

import lombok.Data;

import java.time.LocalTime;
import java.util.UUID;

@Data
public class TimetableEntryDto {
    private UUID id;
    private UUID classId;
    private String className;
    private UUID subjectId;
    private String subjectName;
    private UUID teacherId;
    private String teacherName;
    private UUID periodId;
    private String periodName;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer dayOfWeek;
    private String room;
    private String link;
    private java.util.List<java.util.Map<String, String>> links;
}
