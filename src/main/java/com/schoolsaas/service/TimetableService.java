package com.schoolsaas.service;

import com.schoolsaas.dto.timetable.TimetableEntryDto;
import com.schoolsaas.dto.timetable.TimetablePeriodDto;
import com.schoolsaas.exception.BadRequestException;
import com.schoolsaas.exception.ResourceNotFoundException;
import com.schoolsaas.model.TimetableEntry;
import com.schoolsaas.model.TimetablePeriod;
import com.schoolsaas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetablePeriodRepository periodRepository;
    private final TimetableEntryRepository entryRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    @Transactional
    public TimetablePeriodDto createPeriod(UUID schoolId, TimetablePeriodDto dto) {
        TimetablePeriod period = TimetablePeriod.builder()
                .schoolId(schoolId)
                .name(dto.getName())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .periodOrder(dto.getPeriodOrder())
                .isBreak(dto.getIsBreak())
                .build();
        period = periodRepository.save(period);
        return mapPeriodDto(period);
    }

    @Transactional
    public TimetablePeriodDto updatePeriod(UUID schoolId, UUID periodId, TimetablePeriodDto dto) {
        TimetablePeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable period", "id", periodId));
        if (!period.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Timetable period", "id", periodId);
        }
        period.setName(dto.getName());
        period.setStartTime(dto.getStartTime());
        period.setEndTime(dto.getEndTime());
        period.setPeriodOrder(dto.getPeriodOrder());
        period.setIsBreak(dto.getIsBreak());
        period = periodRepository.save(period);
        return mapPeriodDto(period);
    }

    @Transactional
    public void deletePeriod(UUID schoolId, UUID periodId) {
        TimetablePeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable period", "id", periodId));
        if (!period.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Timetable period", "id", periodId);
        }
        period.setIsActive(false);
        periodRepository.save(period);
    }

    public List<TimetablePeriodDto> getPeriods(UUID schoolId) {
        return periodRepository.findBySchoolIdAndIsActiveTrueOrderByPeriodOrderAsc(schoolId)
                .stream().map(this::mapPeriodDto).collect(Collectors.toList());
    }

    @Transactional
    public TimetableEntryDto createEntry(UUID schoolId, TimetableEntryDto dto) {
        String className = classRepository.findById(dto.getClassId()).map(c -> c.getName()).orElse("This class");

        // Class-level conflict: same class + period + day
        var classConflict = entryRepository.findBySchoolIdAndClassIdAndPeriodIdAndDayOfWeekAndIsActiveTrue(
                schoolId, dto.getClassId(), dto.getPeriodId(), dto.getDayOfWeek());
        if (classConflict.isPresent()) {
            TimetableEntry existing = classConflict.get();
            String subjectName = existing.getSubjectId() != null
                    ? subjectRepository.findById(existing.getSubjectId()).map(s -> s.getName()).orElse("Unknown Subject")
                    : "Unknown Subject";
            String periodName = periodRepository.findById(existing.getPeriodId()).map(p -> p.getName()).orElse("Unknown Period");
            throw new BadRequestException(
                    "Scheduling conflict: " + className + " already has " + subjectName +
                    " scheduled during " + periodName + ". Please choose a different period or day.");
        }

        // Teacher-level conflict: same teacher + period + day (any class)
        List<TimetableEntry> teacherEntries = entryRepository.findBySchoolIdAndTeacherIdAndIsActiveTrue(schoolId, dto.getTeacherId())
                .stream()
                .filter(e -> e.getPeriodId().equals(dto.getPeriodId()) && e.getDayOfWeek().equals(dto.getDayOfWeek()))
                .toList();
        if (!teacherEntries.isEmpty()) {
            TimetableEntry existing = teacherEntries.get(0);
            String existingTeacherName = teacherRepository.findById(existing.getTeacherId()).map(t -> t.getFullName()).orElse("Another teacher");
            String existingClassName = classRepository.findById(existing.getClassId()).map(c -> c.getName()).orElse("another class");
            String existingSubjectName = existing.getSubjectId() != null
                    ? subjectRepository.findById(existing.getSubjectId()).map(s -> s.getName()).orElse("a subject")
                    : "a subject";
            String periodName = periodRepository.findById(existing.getPeriodId()).map(p -> p.getName()).orElse("this period");
            throw new BadRequestException(
                    "Scheduling conflict: " + existingTeacherName + " is already teaching " + existingSubjectName +
                    " for " + existingClassName + " during " + periodName + ". Please choose a different teacher, period, or day.");
        }

        TimetableEntry entry = TimetableEntry.builder()
                .schoolId(schoolId)
                .classId(dto.getClassId())
                .subjectId(dto.getSubjectId())
                .teacherId(dto.getTeacherId())
                .periodId(dto.getPeriodId())
                .dayOfWeek(dto.getDayOfWeek())
                .room(dto.getRoom())
                .link(dto.getLink())
                .links(linksToJson(dto.getLinks()))
                .build();
        entry = entryRepository.save(entry);
        return mapEntryDto(entry);
    }

    @Transactional
    public TimetableEntryDto updateEntry(UUID schoolId, UUID entryId, TimetableEntryDto dto) {
        TimetableEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable entry", "id", entryId));
        if (!entry.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Timetable entry", "id", entryId);
        }

        // Only check conflicts if period, day, or teacher changed
        boolean periodChanged = !entry.getPeriodId().equals(dto.getPeriodId());
        boolean dayChanged = !entry.getDayOfWeek().equals(dto.getDayOfWeek());
        boolean teacherChanged = dto.getTeacherId() != null && !dto.getTeacherId().equals(entry.getTeacherId());

        if (periodChanged || dayChanged || teacherChanged) {
            // Class-level conflict: same class + period + day (excluding this entry)
            var classConflict = entryRepository.findBySchoolIdAndClassIdAndPeriodIdAndDayOfWeekAndIsActiveTrue(
                    schoolId, dto.getClassId(), dto.getPeriodId(), dto.getDayOfWeek());
            if (classConflict.isPresent() && !classConflict.get().getId().equals(entryId)) {
                TimetableEntry existing = classConflict.get();
                String className = classRepository.findById(dto.getClassId()).map(c -> c.getName()).orElse("This class");
                String subjectName = existing.getSubjectId() != null
                        ? subjectRepository.findById(existing.getSubjectId()).map(s -> s.getName()).orElse("Unknown Subject")
                        : "Unknown Subject";
                String periodName = periodRepository.findById(existing.getPeriodId()).map(p -> p.getName()).orElse("Unknown Period");
                throw new BadRequestException(
                        "Scheduling conflict: " + className + " already has " + subjectName +
                        " scheduled during " + periodName + ". Please choose a different period or day.");
            }

            // Teacher-level conflict: same teacher + period + day (any class, excluding this entry)
            List<TimetableEntry> teacherEntries = entryRepository.findBySchoolIdAndTeacherIdAndIsActiveTrue(schoolId, dto.getTeacherId())
                    .stream()
                    .filter(e -> e.getPeriodId().equals(dto.getPeriodId()) && e.getDayOfWeek().equals(dto.getDayOfWeek()) && !e.getId().equals(entryId))
                    .toList();
            if (!teacherEntries.isEmpty()) {
                TimetableEntry existing = teacherEntries.get(0);
                String existingTeacherName = teacherRepository.findById(existing.getTeacherId()).map(t -> t.getFullName()).orElse("Another teacher");
                String existingClassName = classRepository.findById(existing.getClassId()).map(c -> c.getName()).orElse("another class");
                String existingSubjectName = existing.getSubjectId() != null
                        ? subjectRepository.findById(existing.getSubjectId()).map(s -> s.getName()).orElse("a subject")
                        : "a subject";
                String periodName = periodRepository.findById(existing.getPeriodId()).map(p -> p.getName()).orElse("this period");
                throw new BadRequestException(
                        "Scheduling conflict: " + existingTeacherName + " is already teaching " + existingSubjectName +
                        " for " + existingClassName + " during " + periodName + ". Please choose a different teacher, period, or day.");
            }
        }

        entry.setClassId(dto.getClassId());
        entry.setSubjectId(dto.getSubjectId());
        entry.setTeacherId(dto.getTeacherId());
        entry.setPeriodId(dto.getPeriodId());
        entry.setDayOfWeek(dto.getDayOfWeek());
        entry.setRoom(dto.getRoom());
        entry.setLink(dto.getLink());
        entry.setLinks(linksToJson(dto.getLinks()));
        entry = entryRepository.save(entry);
        return mapEntryDto(entry);
    }

    @Transactional
    public void deleteEntry(UUID schoolId, UUID entryId) {
        TimetableEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable entry", "id", entryId));
        if (!entry.getSchoolId().equals(schoolId)) {
            throw new ResourceNotFoundException("Timetable entry", "id", entryId);
        }
        entry.setIsActive(false);
        entryRepository.save(entry);
    }

    public List<TimetableEntryDto> getClassTimetable(UUID schoolId, UUID classId) {
        return entryRepository.findBySchoolIdAndClassIdAndIsActiveTrue(schoolId, classId)
                .stream().map(this::mapEntryDto).collect(Collectors.toList());
    }

    public List<TimetableEntryDto> getTeacherTimetable(UUID schoolId, UUID teacherId) {
        return entryRepository.findBySchoolIdAndTeacherIdAndIsActiveTrue(schoolId, teacherId)
                .stream().map(this::mapEntryDto).collect(Collectors.toList());
    }

    private TimetablePeriodDto mapPeriodDto(TimetablePeriod p) {
        TimetablePeriodDto dto = new TimetablePeriodDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setStartTime(p.getStartTime());
        dto.setEndTime(p.getEndTime());
        dto.setPeriodOrder(p.getPeriodOrder());
        dto.setIsBreak(p.getIsBreak());
        return dto;
    }

    private TimetableEntryDto mapEntryDto(TimetableEntry e) {
        TimetableEntryDto dto = new TimetableEntryDto();
        dto.setId(e.getId());
        dto.setClassId(e.getClassId());
        classRepository.findById(e.getClassId()).ifPresent(c -> dto.setClassName(c.getName()));
        dto.setSubjectId(e.getSubjectId());
        if (e.getSubjectId() != null) subjectRepository.findById(e.getSubjectId()).ifPresent(s -> dto.setSubjectName(s.getName()));
        dto.setTeacherId(e.getTeacherId());
        if (e.getTeacherId() != null) teacherRepository.findById(e.getTeacherId()).ifPresent(t -> dto.setTeacherName(t.getFullName()));
        dto.setPeriodId(e.getPeriodId());
        periodRepository.findById(e.getPeriodId()).ifPresent(p -> {
            dto.setPeriodName(p.getName());
            dto.setStartTime(p.getStartTime());
            dto.setEndTime(p.getEndTime());
        });
        dto.setDayOfWeek(e.getDayOfWeek());
        dto.setRoom(e.getRoom());
        dto.setLink(e.getLink());
        dto.setLinks(linksFromJson(e.getLinks()));
        return dto;
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String linksToJson(List<Map<String, String>> links) {
        if (links == null || links.isEmpty()) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(links);
        } catch (JacksonException ex) {
            return null;
        }
    }

    private List<Map<String, String>> linksFromJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<Map<String, String>>>() {});
        } catch (JacksonException ex) {
            return List.of();
        }
    }
}
