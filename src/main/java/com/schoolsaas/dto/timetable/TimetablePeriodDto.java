package com.schoolsaas.dto.timetable;

import lombok.Data;

import java.time.LocalTime;
import java.util.UUID;

@Data
public class TimetablePeriodDto {
    private UUID id;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer periodOrder;
    private Boolean isBreak;
}
