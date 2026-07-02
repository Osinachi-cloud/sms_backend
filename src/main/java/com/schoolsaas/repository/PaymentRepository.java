package com.schoolsaas.repository;

import com.schoolsaas.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Page<Payment> findBySchoolId(UUID schoolId, Pageable pageable);

    Page<Payment> findByStudentId(UUID studentId, Pageable pageable);

    Optional<Payment> findByPaymentReference(String paymentReference);

    Optional<Payment> findByPaystackReference(String paystackReference);

    Page<Payment> findBySchoolIdAndStatus(UUID schoolId, String status, Pageable pageable);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.schoolId = :schoolId AND p.status = 'SUCCESS'")
    BigDecimal sumSuccessfulPaymentsBySchoolId(UUID schoolId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.studentId = :studentId AND p.status = 'SUCCESS'")
    BigDecimal sumSuccessfulPaymentsByStudentId(UUID studentId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.schoolId = :schoolId AND p.status = 'SUCCESS'")
    long countSuccessfulBySchoolId(UUID schoolId);

    java.util.List<Payment> findBySchoolId(UUID schoolId);

    java.util.List<Payment> findTop5BySchoolIdOrderByCreatedAtDesc(UUID schoolId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.schoolId = :schoolId AND p.status = :status")
    BigDecimal sumAmountBySchoolIdAndStatus(UUID schoolId, String status);

    @Modifying
    @Query("UPDATE Payment p SET p.studentId = null, p.studentFeeId = null WHERE p.studentId = :studentId")
    void unlinkByStudentId(@Param("studentId") UUID studentId);
}
