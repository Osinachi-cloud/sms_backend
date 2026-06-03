package com.schoolsaas.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "book_borrowals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookBorrowal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "borrow_date")
    @Builder.Default
    private LocalDate borrowDate = LocalDate.now();

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Column(nullable = false)
    @Builder.Default
    private String status = "BORROWED";

    @Column(name = "fine_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal fineAmount = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
