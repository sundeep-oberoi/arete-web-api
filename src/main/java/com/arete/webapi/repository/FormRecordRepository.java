package com.arete.webapi.repository;

import com.arete.webapi.model.FormRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FormRecordRepository extends JpaRepository<FormRecord, Long> {
    Optional<FormRecord> findByEmailAddress(String emailAddress);
    Optional<FormRecord> findByFormNumber(String formNumber);
}
