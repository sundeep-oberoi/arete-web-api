package com.arete.webapi.service;

import com.arete.webapi.dto.FormData;
import com.arete.webapi.dto.OfferResponse;
import com.arete.webapi.dto.ai.PremiumResult;
import com.arete.webapi.model.FormRecord;
import com.arete.webapi.repository.FormRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormServiceTest {

    @Mock
    private FormRecordRepository formRecordRepository;

    @Mock
    private AiModelService aiModelService;

    @InjectMocks
    private FormService formService;

    private FormData formData;

    @BeforeEach
    void setUp() {
        formData = new FormData();
        formData.setProfile("employee");
        formData.setCoverPartner(true);
        formData.setCoverChildren(false);
        formData.setNumberOfChildren(0);
        formData.setAge("35");
        formData.setPostcode("75001");
        formData.setOpticalNeeds("standard");
        formData.setDentalNeeds("maintenance");
        formData.setAlternativeMedicine("one_two");
        formData.setHospitalisationPreference("private_preferred");
        formData.setDoctorChoice("gp_specialist");
        formData.setEmail("test@example.com");
        formData.setPhoneNumber("0612345678");
    }

    @Test
    void saveLeaveEmail_savesRecordWithEmailAndFormNumber() {
        when(formRecordRepository.save(any(FormRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        formService.saveLeaveEmail("test@example.com", formData);

        ArgumentCaptor<FormRecord> captor = ArgumentCaptor.forClass(FormRecord.class);
        verify(formRecordRepository).save(captor.capture());

        FormRecord saved = captor.getValue();
        assertThat(saved.getEmailAddress()).isEqualTo("test@example.com");
        assertThat(saved.getFormNumber()).isNotBlank();
        assertThat(saved.getProfile()).isEqualTo("employee");
        assertThat(saved.isCoverPartner()).isTrue();
        assertThat(saved.getMonthlyPremium()).isNull();
        assertThat(saved.getAnnualPremium()).isNull();
    }

    @Test
    void calculateOffer_returnsPremiumFromAiModel() {
        when(aiModelService.fetchPremium(any())).thenReturn(new PremiumResult(100.0, 1000.0));
        when(formRecordRepository.save(any(FormRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        OfferResponse response = formService.calculateOffer(formData);

        assertThat(response.getMonthlyPremium()).isEqualTo(100.0);
        assertThat(response.getAnnualPremium()).isEqualTo(1000.0);
        assertThat(response.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void calculateOffer_savesPremiumsToDatabase() {
        when(aiModelService.fetchPremium(any())).thenReturn(new PremiumResult(100.0, 1000.0));
        when(formRecordRepository.save(any(FormRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        formService.calculateOffer(formData);

        ArgumentCaptor<FormRecord> captor = ArgumentCaptor.forClass(FormRecord.class);
        verify(formRecordRepository).save(captor.capture());

        FormRecord saved = captor.getValue();
        assertThat(saved.getMonthlyPremium()).isEqualByComparingTo(BigDecimal.valueOf(100.0));
        assertThat(saved.getAnnualPremium()).isEqualByComparingTo(BigDecimal.valueOf(1000.0));
    }

    @Test
    void calculateOffer_buildsCoverageDetails_forAllFields() {
        when(aiModelService.fetchPremium(any())).thenReturn(new PremiumResult(100.0, 1000.0));
        when(formRecordRepository.save(any(FormRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        OfferResponse response = formService.calculateOffer(formData);

        assertThat(response.getCoverageDetails()).hasSize(5);
        assertThat(response.getCoverageDetails()).anyMatch(d -> d.contains("Standard glasses or contact lenses"));
        assertThat(response.getCoverageDetails()).anyMatch(d -> d.contains("Just maintenance"));
        assertThat(response.getCoverageDetails()).anyMatch(d -> d.contains("1–2 sessions/year"));
        assertThat(response.getCoverageDetails()).anyMatch(d -> d.contains("Private room preferred"));
        assertThat(response.getCoverageDetails()).anyMatch(d -> d.contains("GP + Specialist referral"));
    }

    @Test
    void calculateOffer_buildsCoverageDetails_forOptionalVariants() {
        formData.setOpticalNeeds("nothing");
        formData.setDentalNeeds("none");
        formData.setAlternativeMedicine("never");
        formData.setHospitalisationPreference("shared");
        formData.setDoctorChoice("specialist_private");
        when(aiModelService.fetchPremium(any())).thenReturn(new PremiumResult(80.0, 960.0));
        when(formRecordRepository.save(any(FormRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        OfferResponse response = formService.calculateOffer(formData);

        assertThat(response.getCoverageDetails()).anyMatch(d -> d.contains("No optical coverage"));
        assertThat(response.getCoverageDetails()).anyMatch(d -> d.contains("No dental coverage"));
        assertThat(response.getCoverageDetails()).anyMatch(d -> d.contains("Not included"));
        assertThat(response.getCoverageDetails()).anyMatch(d -> d.contains("Shared room"));
        assertThat(response.getCoverageDetails()).anyMatch(d -> d.contains("Direct specialist access (private)"));
    }

    @Test
    void calculateOffer_buildsCoverageDetails_forProgressiveAndMajorVariants() {
        formData.setOpticalNeeds("progressive");
        formData.setDentalNeeds("standard");
        formData.setAlternativeMedicine("more_than_three");
        formData.setHospitalisationPreference("private_essential");
        formData.setDoctorChoice("specialist_standard");
        when(aiModelService.fetchPremium(any())).thenReturn(new PremiumResult(130.0, 1560.0));
        when(formRecordRepository.save(any(FormRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        OfferResponse response = formService.calculateOffer(formData);

        assertThat(response.getCoverageDetails()).anyMatch(d -> d.contains("Progressive lenses"));
        assertThat(response.getCoverageDetails()).anyMatch(d -> d.contains("Standard dental care"));
        assertThat(response.getCoverageDetails()).anyMatch(d -> d.contains("3+ sessions/year"));
        assertThat(response.getCoverageDetails()).anyMatch(d -> d.contains("Private room essential"));
        assertThat(response.getCoverageDetails()).anyMatch(d -> d.contains("Direct specialist access (standard)"));
    }

    @Test
    void calculateOffer_buildsCoverageDetails_forSurgeryAndMajorDental() {
        formData.setOpticalNeeds("surgery");
        formData.setDentalNeeds("major");
        when(aiModelService.fetchPremium(any())).thenReturn(new PremiumResult(150.0, 1800.0));
        when(formRecordRepository.save(any(FormRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        OfferResponse response = formService.calculateOffer(formData);

        assertThat(response.getCoverageDetails()).anyMatch(d -> d.contains("Eye surgery coverage"));
        assertThat(response.getCoverageDetails()).anyMatch(d -> d.contains("Major dental works"));
    }

    @Test
    void calculateOffer_handlesNullOptionalFields() {
        formData.setOpticalNeeds(null);
        formData.setDentalNeeds(null);
        formData.setAlternativeMedicine(null);
        formData.setHospitalisationPreference(null);
        formData.setDoctorChoice(null);
        when(aiModelService.fetchPremium(any())).thenReturn(new PremiumResult(70.0, 840.0));
        when(formRecordRepository.save(any(FormRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        OfferResponse response = formService.calculateOffer(formData);

        assertThat(response.getCoverageDetails()).isEmpty();
    }
}
