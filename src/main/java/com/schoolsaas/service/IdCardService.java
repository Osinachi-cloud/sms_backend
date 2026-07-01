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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        java.awt.Color darkBlue = new java.awt.Color(11, 29, 58);
        java.awt.Color white = new java.awt.Color(255, 255, 255);

        Font whiteBold = new Font(Font.HELVETICA, 9, Font.BOLD, white);
        Font whiteSmall = new Font(Font.HELVETICA, 7, Font.NORMAL, white);
        Font labelFont = new Font(Font.HELVETICA, 7, Font.BOLD, new java.awt.Color(60, 60, 60));
        Font valueFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new java.awt.Color(30, 30, 30));
        Font footerFont = new Font(Font.HELVETICA, 6, Font.NORMAL, new java.awt.Color(100, 100, 100));

        // ===== HEADER BAR =====
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(darkBlue);
        headerCell.setPadding(8);
        headerCell.setBorder(Rectangle.NO_BORDER);
        Paragraph schoolName = new Paragraph(student.getSchool() != null ? student.getSchool().getName() : "SCHOOL", whiteBold);
        schoolName.setAlignment(Element.ALIGN_CENTER);
        headerCell.addElement(schoolName);
        Paragraph idLabel = new Paragraph("STUDENT IDENTITY CARD", whiteSmall);
        idLabel.setAlignment(Element.ALIGN_CENTER);
        headerCell.addElement(idLabel);
        headerTable.addCell(headerCell);
        document.add(headerTable);
        document.add(Chunk.NEWLINE);

        // ===== PHOTO + DETAILS =====
        PdfPTable mainTable = new PdfPTable(2);
        mainTable.setWidthPercentage(100);
        mainTable.setWidths(new float[]{1, 2});

        // Photo cell
        PdfPCell photoCell = new PdfPCell();
        photoCell.setBorder(Rectangle.NO_BORDER);
        photoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        photoCell.setVerticalAlignment(Element.ALIGN_TOP);

        Image photoImage = loadStudentPhoto(student.getPhotoUrl());
        if (photoImage != null) {
            photoImage.scaleToFit(65, 80);
            photoImage.setAlignment(Element.ALIGN_CENTER);
            photoCell.addElement(photoImage);
        } else {
            // Placeholder box
            PdfPTable placeholder = new PdfPTable(1);
            placeholder.setWidthPercentage(80);
            PdfPCell phCell = new PdfPCell(new Paragraph("PHOTO", new Font(Font.HELVETICA, 7, Font.NORMAL, new java.awt.Color(150, 150, 150))));
            phCell.setFixedHeight(75);
            phCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            phCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            phCell.setBorder(Rectangle.BOX);
            phCell.setBorderColor(new java.awt.Color(180, 180, 180));
            placeholder.addCell(phCell);
            photoCell.addElement(placeholder);
        }
        mainTable.addCell(photoCell);

        // Details cell
        PdfPCell detailsCell = new PdfPCell();
        detailsCell.setBorder(Rectangle.NO_BORDER);
        detailsCell.setPaddingLeft(6);
        detailsCell.setVerticalAlignment(Element.ALIGN_TOP);

        addDetailRow(detailsCell, "Name:", student.getFullName(), labelFont, valueFont);
        addDetailRow(detailsCell, "Adm. No:", student.getAdmissionNumber(), labelFont, valueFont);
        addDetailRow(detailsCell, "Class:", student.getSchoolClass() != null ? student.getSchoolClass().getName() : "N/A", labelFont, valueFont);
        addDetailRow(detailsCell, "Gender:", student.getGender() != null ? student.getGender() : "N/A", labelFont, valueFont);
        addDetailRow(detailsCell, "DOB:", student.getDateOfBirth() != null ? student.getDateOfBirth().toString() : "N/A", labelFont, valueFont);

        mainTable.addCell(detailsCell);
        document.add(mainTable);
        document.add(Chunk.NEWLINE);

        // ===== QR CODE =====
        Image qrImage = decodeQrImage(qrBase64);
        if (qrImage != null) {
            qrImage.scaleToFit(55, 55);
            qrImage.setAlignment(Element.ALIGN_CENTER);
            document.add(qrImage);
        }
        document.add(Chunk.NEWLINE);

        // ===== FOOTER =====
        Paragraph cardNum = new Paragraph("Card No: " + cardNumber, new Font(Font.HELVETICA, 7, Font.BOLD, darkBlue));
        cardNum.setAlignment(Element.ALIGN_CENTER);
        document.add(cardNum);

        Paragraph validity = new Paragraph("Valid until: " + LocalDate.now().plusYears(1), footerFont);
        validity.setAlignment(Element.ALIGN_CENTER);
        document.add(validity);

        document.close();
        return baos;
    }

    private void addDetailRow(PdfPCell cell, String label, String value, Font labelFont, Font valueFont) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + " ", labelFont));
        p.add(new Chunk(value, valueFont));
        p.setLeading(11);
        cell.addElement(p);
    }

    @SneakyThrows
    private Image decodeQrImage(String qrBase64) {
        if (qrBase64 == null || qrBase64.isBlank()) return null;
        String data = qrBase64;
        if (data.contains(",")) {
            data = data.substring(data.indexOf(",") + 1);
        }
        byte[] bytes = Base64.getDecoder().decode(data);
        return Image.getInstance(bytes);
    }

    @SneakyThrows
    private Image loadStudentPhoto(String photoUrl) {
        if (photoUrl == null || photoUrl.isBlank()) return null;
        if (photoUrl.startsWith("/uploads/")) {
            Path path = Paths.get("." + photoUrl);
            if (Files.exists(path)) {
                return Image.getInstance(path.toAbsolutePath().toString());
            }
            // Try from project root
            path = Paths.get(photoUrl.substring(1));
            if (Files.exists(path)) {
                return Image.getInstance(path.toAbsolutePath().toString());
            }
        }
        if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
            return Image.getInstance(new URL(photoUrl));
        }
        return null;
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
            dto.setPhotoUrl(student.getPhotoUrl());
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
