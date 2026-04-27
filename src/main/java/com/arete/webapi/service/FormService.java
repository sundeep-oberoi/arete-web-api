package com.arete.webapi.service;

import com.arete.webapi.dto.FormData;
import com.arete.webapi.dto.OfferResponse;
import com.arete.webapi.exception.OfferNotFoundException;
import com.arete.webapi.exception.OfferNotReadyException;
import com.arete.webapi.model.FormRecord;
import com.arete.webapi.repository.FormRecordRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FormService {

    private static final Logger log = LoggerFactory.getLogger(FormService.class);

    @Value("${offer.wait.ms:10000}")
    private long offerWaitMs;

    private final FormRecordRepository formRecordRepository;
    private final OfferWorkerService offerWorkerService;
    private final ObjectMapper objectMapper;

    public FormService(FormRecordRepository formRecordRepository,
                       OfferWorkerService offerWorkerService,
                       ObjectMapper objectMapper) {
        this.formRecordRepository = formRecordRepository;
        this.offerWorkerService = offerWorkerService;
        this.objectMapper = objectMapper;
    }

    public void saveLeaveEmail(String email, FormData formData) {
        log.info("Saving form state for email: {}", email);
        FormRecord record = toFormRecord(formData);
        record.setFormNumber(UUID.randomUUID().toString());
        record.setEmailAddress(email);
        formRecordRepository.save(record);
        log.debug("Saved leave-email record form_number={}", record.getFormNumber());
    }

    public String saveForm(FormData formData) {
        String uuid = UUID.randomUUID().toString();
        log.info("Saving form, form_number={}", uuid);
        FormRecord record = toFormRecord(formData);
        record.setFormNumber(uuid);
        record.setEmailAddress(formData.getEmail());
        formRecordRepository.save(record);
        offerWorkerService.computeOffer(uuid, formData);
        return uuid;
    }

    public OfferResponse getOffer(String uuid) {
        FormRecord record = formRecordRepository.findByFormNumber(uuid)
                .orElseThrow(() -> new OfferNotFoundException(uuid));

        if (record.getMonthlyPremium() == null) {
            log.info("Offer not ready for uuid={}, waiting {}ms", uuid, offerWaitMs);
            try {
                Thread.sleep(offerWaitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            record = formRecordRepository.findByFormNumber(uuid)
                    .orElseThrow(() -> new OfferNotFoundException(uuid));
        }

        if (record.getMonthlyPremium() == null) {
            log.warn("Offer still not ready for uuid={} after wait", uuid);
            throw new OfferNotReadyException(uuid);
        }

        List<String> coverage = parseCoverageDetails(record.getCoverageDetails());
        return new OfferResponse(
                record.getMonthlyPremium().doubleValue(),
                record.getAnnualPremium().doubleValue(),
                record.getCurrency() != null ? record.getCurrency() : "EUR",
                coverage);
    }

    private List<String> parseCoverageDetails(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse coverage_details: {}", json);
            return List.of();
        }
    }

    private FormRecord toFormRecord(FormData formData) {
        FormRecord record = new FormRecord();
        record.setProfile(formData.getProfile());
        record.setCoverPartner(formData.isCoverPartner());
        record.setCoverChildren(formData.isCoverChildren());
        record.setNumberOfChildren(formData.getNumberOfChildren());
        record.setAge(formData.getAge());
        record.setPostcode(formData.getPostcode());
        record.setOpticalNeeds(formData.getOpticalNeeds());
        record.setDentalNeeds(formData.getDentalNeeds());
        record.setAlternativeMedicine(formData.getAlternativeMedicine());
        record.setHospitalisationPreference(formData.getHospitalisationPreference());
        record.setDoctorChoice(formData.getDoctorChoice());
        record.setPhoneNumber(formData.getPhoneNumber());
        return record;
    }
}
