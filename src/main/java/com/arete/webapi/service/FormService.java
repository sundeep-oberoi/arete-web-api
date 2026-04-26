package com.arete.webapi.service;

import com.arete.webapi.dto.FormData;
import com.arete.webapi.dto.OfferResponse;
import com.arete.webapi.dto.ai.PremiumResult;
import com.arete.webapi.model.FormRecord;
import com.arete.webapi.repository.FormRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FormService {

    private static final Logger log = LoggerFactory.getLogger(FormService.class);
    private static final String CURRENCY = "EUR";

    private final FormRecordRepository formRecordRepository;
    private final AiModelService aiModelService;

    public FormService(FormRecordRepository formRecordRepository, AiModelService aiModelService) {
        this.formRecordRepository = formRecordRepository;
        this.aiModelService = aiModelService;
    }

    public void saveLeaveEmail(String email, FormData formData) {
        log.info("Saving form state for email: {}", email);

        FormRecord record = toFormRecord(formData);
        record.setFormNumber(UUID.randomUUID().toString());
        record.setEmailAddress(email);

        formRecordRepository.save(record);
        log.debug("Saved form record with form_number={}", record.getFormNumber());
    }

    public OfferResponse calculateOffer(FormData formData) {
        log.info("Calculating offer for profile={}, age={}", formData.getProfile(), formData.getAge());

        PremiumResult premium = aiModelService.fetchPremium(formData);
        List<String> coverageDetails = buildCoverageDetails(formData);

        FormRecord record = toFormRecord(formData);
        record.setFormNumber(UUID.randomUUID().toString());
        record.setEmailAddress(formData.getEmail());
        record.setMonthlyPremium(BigDecimal.valueOf(premium.monthlyPremium()));
        record.setAnnualPremium(BigDecimal.valueOf(premium.annualPremium()));

        formRecordRepository.save(record);
        log.debug("Saved offer record with form_number={}", record.getFormNumber());

        return new OfferResponse(premium.monthlyPremium(), premium.annualPremium(), CURRENCY, coverageDetails);
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

    private List<String> buildCoverageDetails(FormData formData) {
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

    private String describeOptical(String opticalNeeds) {
        return switch (opticalNeeds) {
            case "nothing"     -> "No optical coverage";
            case "standard"    -> "Standard glasses or contact lenses";
            case "progressive" -> "Progressive lenses";
            case "surgery"     -> "Eye surgery coverage";
            default            -> opticalNeeds;
        };
    }

    private String describeDental(String dentalNeeds) {
        return switch (dentalNeeds) {
            case "none"        -> "No dental coverage";
            case "maintenance" -> "Just maintenance";
            case "standard"    -> "Standard dental care";
            case "major"       -> "Major dental works";
            default            -> dentalNeeds;
        };
    }

    private String describeAlternativeMedicine(String alternativeMedicine) {
        return switch (alternativeMedicine) {
            case "never"           -> "Not included";
            case "one_two"         -> "1–2 sessions/year";
            case "more_than_three" -> "3+ sessions/year";
            default                -> alternativeMedicine;
        };
    }

    private String describeHospitalisation(String hospitalisationPreference) {
        return switch (hospitalisationPreference) {
            case "shared"            -> "Shared room";
            case "private_preferred" -> "Private room preferred";
            case "private_essential" -> "Private room essential";
            default                  -> hospitalisationPreference;
        };
    }

    private String describeDoctorChoice(String doctorChoice) {
        return switch (doctorChoice) {
            case "gp_specialist"       -> "GP + Specialist referral";
            case "specialist_standard" -> "Direct specialist access (standard)";
            case "specialist_private"  -> "Direct specialist access (private)";
            default                    -> doctorChoice;
        };
    }
}
