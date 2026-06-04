package com.schoolsaas.repository;

import com.schoolsaas.model.SchoolPaymentGatewayConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolPaymentGatewayConfigRepository extends JpaRepository<SchoolPaymentGatewayConfig, UUID> {

    Optional<SchoolPaymentGatewayConfig> findBySchoolId(UUID schoolId);
}
