package com.schoolsaas.repository;

import com.schoolsaas.model.AdmissionDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdmissionDocumentRepository extends JpaRepository<AdmissionDocument, UUID> {
    List<AdmissionDocument> findByApplicationId(UUID applicationId);
}
