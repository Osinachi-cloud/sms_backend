package com.schoolsaas.service;

import com.schoolsaas.dto.parent.ParentDto;
import com.schoolsaas.dto.parent.ParentStudentInfo;
import com.schoolsaas.model.Parent;
import com.schoolsaas.model.ParentStudent;
import com.schoolsaas.model.Student;
import com.schoolsaas.repository.ParentRepository;
import com.schoolsaas.repository.ParentStudentRepository;
import com.schoolsaas.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParentService {

    private final ParentRepository parentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public ParentDto createParent(UUID schoolId, ParentDto dto) {
        Parent parent = Parent.builder()
                .schoolId(schoolId)
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .occupation(dto.getOccupation())
                .relationship(dto.getRelationship())
                .build();
        parent = parentRepository.save(parent);

        if (dto.getStudents() != null) {
            for (ParentStudentInfo info : dto.getStudents()) {
                parentStudentRepository.save(ParentStudent.builder()
                        .parentId(parent.getId())
                        .studentId(info.getStudentId())
                        .isPrimary(info.getIsPrimary())
                        .build());
            }
        }

        return mapToDto(parent);
    }

    public Page<ParentDto> listParents(UUID schoolId, Pageable pageable) {
        return parentRepository.findBySchoolId(schoolId, pageable).map(this::mapToDto);
    }

    public List<ParentDto> getParentsByStudent(UUID studentId) {
        List<UUID> parentIds = parentStudentRepository.findByStudentId(studentId).stream()
                .map(ParentStudent::getParentId).collect(Collectors.toList());
        return parentRepository.findAllById(parentIds).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public List<ParentStudentInfo> getStudentChildren(UUID parentId) {
        List<UUID> studentIds = parentStudentRepository.findByParentId(parentId).stream()
                .map(ParentStudent::getStudentId).collect(Collectors.toList());
        return studentRepository.findAllById(studentIds).stream().map(this::mapStudentInfo).collect(Collectors.toList());
    }

    private ParentDto mapToDto(Parent parent) {
        ParentDto dto = new ParentDto();
        dto.setId(parent.getId());
        dto.setUserId(parent.getUserId());
        dto.setFullName(parent.getFullName());
        dto.setEmail(parent.getEmail());
        dto.setPhone(parent.getPhone());
        dto.setAddress(parent.getAddress());
        dto.setOccupation(parent.getOccupation());
        dto.setRelationship(parent.getRelationship());
        dto.setIsActive(parent.getIsActive());
        dto.setStudents(getStudentChildren(parent.getId()));
        return dto;
    }

    private ParentStudentInfo mapStudentInfo(Student s) {
        ParentStudentInfo info = new ParentStudentInfo();
        info.setStudentId(s.getId());
        info.setStudentName(s.getFullName());
        info.setAdmissionNumber(s.getAdmissionNumber());
        info.setStudentClass(s.getSchoolClass() != null ? s.getSchoolClass().getName() : null);
        return info;
    }
}
