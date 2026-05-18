package com.arete.webapi.service;

import com.arete.webapi.dto.FormData;
import com.arete.webapi.model.FormRecord;
import com.arete.webapi.repository.FormRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OfferWorkerService {

    private static final Logger log = LoggerFactory.getLogger(OfferWorkerService.class);
    private static final String CURRENCY = "EUR";

    @Value("${offer.price.min.monthly:75}")
    private double minMonthlyPrice;

    @Value("${offer.price.max.monthly:100}")
    private double maxMonthlyPrice;

    @Value("${offer.price.annual.discount:0.85}")
    private double annualDiscount;

    private final FormRecordRepository formRecordRepository;
    private final ObjectMapper objectMapper;

    public OfferWorkerService(FormRecordRepository formRecordRepository,
                              ObjectMapper objectMapper) {
        this.formRecordRepository = formRecordRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    public void computeOffer(String formNumber, FormData formData) {
        log.info("Worker starting offer computation for form_number={}", formNumber);
        try {
            BigDecimal monthly = generateMonthlyPrice();
            BigDecimal annual = monthly.multiply(BigDecimal.valueOf(12))
                    .multiply(BigDecimal.valueOf(annualDiscount))
                    .setScale(2, RoundingMode.HALF_UP);

            List<String> coverage = buildCoverageDetails(formData);
            String coverageJson = objectMapper.writeValueAsString(coverage);

            FormRecord record = formRecordRepository.findByFormNumber(formNumber)
                    .orElseThrow(() -> new IllegalStateException("Form record not found: " + formNumber));
            record.setMonthlyPremium(monthly);
            record.setAnnualPremium(annual);
            record.setCurrency(CURRENCY);
            record.setCoverageDetails(coverageJson);
            formRecordRepository.save(record);
            log.info("Worker saved offer for form_number={}: monthly={} annual={}", formNumber, monthly, annual);
        } catch (Exception e) {
            log.error("Worker failed for form_number={}", formNumber, e);
        }
    }

    BigDecimal generateMonthlyPrice() {
        double min = Math.min(minMonthlyPrice, maxMonthlyPrice);
        double max = Math.max(minMonthlyPrice, maxMonthlyPrice);
        double value = (min == max) ? min : ThreadLocalRandom.current().nextDouble(min, max);
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    List<String> buildCoverageDetails(FormData formData) {
        List<String> details = new ArrayList<>();
        if (formData.getOpticalNeeds() != null) {
            details.add("Optical: " + describeOptical(formData.getOpticalNeeds()));
        }
        if (formData.getDentalNeeds() != null) {
            details.add("Dental: " + describeDental(formData.getDentalNeeds()));
        }
        if (formData.getAlternativeMedicine() != null) {
            details.add("Alternative medicine: " + describeAlternativeMedicine(formData.getAlternativeMedicine()));
        }
        if (formData.getHospitalisationPreference() != null) {
            details.add("Hospitalisation: " + describeHospitalisation(formData.getHospitalisationPreference()));
        }
        if (formData.getDoctorChoice() != null) {
            details.add("Doctor access: " + describeDoctorChoice(formData.getDoctorChoice()));
        }
        return details;
    }

    private String describeOptical(String v) {
        return switch (v) {
            case "nothing"     -> "No optical coverage";
            case "standard"    -> "Standard glasses or contact lenses";
            case "progressive" -> "Progressive lenses";
            case "surgery"     -> "Eye surgery coverage";
            default            -> v;
        };
    }

    private String describeDental(String v) {
        return switch (v) {
            case "none"        -> "No dental coverage";
            case "maintenance" -> "Just maintenance";
            case "standard"    -> "Standard dental care";
            case "major"       -> "Major dental works";
            default            -> v;
        };
    }

    private String describeAlternativeMedicine(String v) {
        return switch (v) {
            case "never"           -> "Not included";
            case "one_two"         -> "1–2 sessions/year";
            case "more_than_three" -> "3+ sessions/year";
            default                -> v;
        };
    }

    private String describeHospitalisation(String v) {
        return switch (v) {
            case "shared"            -> "Shared room";
            case "private_preferred" -> "Private room preferred";
            case "private_essential" -> "Private room essential";
            default                  -> v;
        };
    }

    private String describeDoctorChoice(String v) {
        return switch (v) {
            case "gp_specialist"       -> "GP + Specialist referral";
            case "specialist_standard" -> "Direct specialist access (standard)";
            case "specialist_private"  -> "Direct specialist access (private)";
            default                    -> v;
        };
    }
}
