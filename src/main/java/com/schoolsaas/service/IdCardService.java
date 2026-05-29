package com.schoolsaas.service;

import com.schoolsaas.dto.idcard.IdCardTemplateDto;
import com.schoolsaas.dto.idcard.StudentIdCardDto;
import com.schoolsaas.model.IdCardTemplate;
import com.schoolsaas.model.Student;
import com.schoolsaas.model.StudentIdCard;
import com.schoolsaas.repository.IdCardTemplateRepository;
import com.schoolsaas.repository.StudentIdCardRepository;
import com.schoolsaas.repository.StudentRepository;
import com.schoolsaas.security.SecurityUtils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IdCardService {

    private final IdCardTemplateRepository templateRepository;
    private final StudentIdCardRepository studentIdCardRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public IdCardTemplateDto createTemplate(UUID schoolId, IdCardTemplateDto dto) {
        IdCardTemplate template = IdCardTemplate.builder()
                .schoolId(schoolId)
                .name(dto.getName())
                .layoutConfig(dto.getLayoutConfig())
                .frontDesign(dto.getFrontDesign())
                .backDesign(dto.getBackDesign())
                .isDefault(dto.getIsDefault())
                .createdBy(SecurityUtils.getCurrentUserId())
                .build();
        template = templateRepository.save(template);
        return mapTemplateDto(template);
    }

    public List<IdCardTemplateDto> listTemplates(UUID schoolId) {
        return templateRepository.findBySchoolIdAndIsActiveTrue(schoolId).stream()
                .map(this::mapTemplateDto).collect(Collectors.toList());
    }

    @Transactional
    @SneakyThrows
    public StudentIdCardDto generateIdCard(UUID schoolId, UUID studentId, UUID templateId) {
        Student student = studentRepository.findById(studentId).orElseThrow();
        IdCardTemplate template = templateRepository.findById(templateId).orElseThrow();

        String cardNumber = schoolId.toString().substring(0, 4).toUpperCase() + "-" +
                student.getAdmissionNumber() + "-" + LocalDate.now().getYear();

        // Generate QR Code
        String qrData = "STUDENT:" + studentId + ":" + student.getAdmissionNumber();
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, 150, 150);
        ByteArrayOutputStream qrStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrStream);
        String qrBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(qrStream.toByteArray());

        // Generate PDF
        ByteArrayOutputStream pdfStream = generateIdCardPdf(student, template, cardNumber, qrBase64);
        String pdfBase64 = "data:application/pdf;base64," + Base64.getEncoder().encodeToString(pdfStream.toByteArray());

        StudentIdCard card = StudentIdCard.builder()
                .schoolId(schoolId)
                .studentId(studentId)
                .templateId(templateId)
                .cardNumber(cardNumber)
                .issueDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusYears(1))
                .qrCode(qrBase64)
                .generatedPdfUrl(pdfBase64)
                .createdBy(SecurityUtils.getCurrentUserId())
                .build();
        card = studentIdCardRepository.save(card);

        return mapCardDto(card, student);
    }

    public Page<StudentIdCardDto> listIdCards(UUID schoolId, Pageable pageable) {
        return studentIdCardRepository.findBySchoolId(schoolId, pageable).map(card -> {
            Student student = studentRepository.findById(card.getStudentId()).orElse(null);
            return mapCardDto(card, student);
        });
    }

    @SneakyThrows
    private ByteArrayOutputStream generateIdCardPdf(Student student, IdCardTemplate template, String cardNumber, String qrBase64) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(new Rectangle(252, 396)); // ID card size in points (~3.5 x 5.5 inches)
        PdfWriter writer = PdfWriter.getInstance(document, baos);
        document.open();

        // Header
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, new java.awt.Color(0, 51, 153));
        Paragraph header = new Paragraph("STUDENT ID CARD", headerFont);
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);
        document.add(Chunk.NEWLINE);

        // Student info
        Font labelFont = new Font(Font.HELVETICA, 8, Font.BOLD);
        Font valueFont = new Font(Font.HELVETICA, 8);

        document.add(new Paragraph("Name: " + student.getFullName(), valueFont));
        document.add(new Paragraph("Admission No: " + student.getAdmissionNumber(), valueFont));
        document.add(new Paragraph("Class: " + (student.getSchoolClass() != null ? student.getSchoolClass().getName() : "N/A"), valueFont));
        document.add(new Paragraph("Card No: " + cardNumber, valueFont));
        document.add(new Paragraph("Valid Until: " + LocalDate.now().plusYears(1), valueFont));

        document.close();
        return baos;
    }

    private IdCardTemplateDto mapTemplateDto(IdCardTemplate t) {
        IdCardTemplateDto dto = new IdCardTemplateDto();
        dto.setId(t.getId());
        dto.setName(t.getName());
        dto.setLayoutConfig(t.getLayoutConfig());
        dto.setFrontDesign(t.getFrontDesign());
        dto.setBackDesign(t.getBackDesign());
        dto.setIsDefault(t.getIsDefault());
        return dto;
    }

    private StudentIdCardDto mapCardDto(StudentIdCard card, Student student) {
        StudentIdCardDto dto = new StudentIdCardDto();
        dto.setId(card.getId());
        dto.setStudentId(card.getStudentId());
        if (student != null) {
            dto.setStudentName(student.getFullName());
            dto.setStudentClass(student.getSchoolClass() != null ? student.getSchoolClass().getName() : null);
            dto.setAdmissionNumber(student.getAdmissionNumber());
        }
        dto.setCardNumber(card.getCardNumber());
        dto.setIssueDate(card.getIssueDate());
        dto.setExpiryDate(card.getExpiryDate());
        dto.setQrCode(card.getQrCode());
        dto.setGeneratedPdfUrl(card.getGeneratedPdfUrl());
        dto.setStatus(card.getStatus());
        return dto;
    }
}
