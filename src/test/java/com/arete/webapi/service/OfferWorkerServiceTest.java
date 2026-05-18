package com.arete.webapi.service;

import com.arete.webapi.dto.FormData;
import com.arete.webapi.model.FormRecord;
import com.arete.webapi.repository.FormRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferWorkerServiceTest {

    @Mock
    private FormRecordRepository formRecordRepository;

    private OfferWorkerService workerService;

    private FormData formData;

    @BeforeEach
    void setUp() {
        workerService = new OfferWorkerService(formRecordRepository, new ObjectMapper());
        ReflectionTestUtils.setField(workerService, "minMonthlyPrice", 75.0);
        ReflectionTestUtils.setField(workerService, "maxMonthlyPrice", 100.0);
        ReflectionTestUtils.setField(workerService, "annualDiscount", 0.85);

        formData = new FormData();
        formData.setProfile("employee");
        formData.setAge("35");
        formData.setPostcode("75001");
        formData.setCoverPartner(true);
        formData.setCoverChildren(false);
        formData.setNumberOfChildren(0);
        formData.setOpticalNeeds("standard");
        formData.setDentalNeeds("maintenance");
        formData.setAlternativeMedicine("one_two");
        formData.setHospitalisationPreference("private_preferred");
        formData.setDoctorChoice("gp_specialist");
    }

    @Test
    void computeOffer_savesRandomOfferToDatabase() {
        FormRecord record = new FormRecord();
        record.setFormNumber("test-uuid");
        when(formRecordRepository.findByFormNumber("test-uuid")).thenReturn(Optional.of(record));
        when(formRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        workerService.computeOffer("test-uuid", formData);

        ArgumentCaptor<FormRecord> captor = ArgumentCaptor.forClass(FormRecord.class);
        verify(formRecordRepository).save(captor.capture());

        FormRecord saved = captor.getValue();
        assertThat(saved.getMonthlyPremium()).isBetween(new BigDecimal("75.00"), new BigDecimal("100.00"));
        BigDecimal expectedAnnual = saved.getMonthlyPremium()
                .multiply(BigDecimal.valueOf(12))
                .multiply(BigDecimal.valueOf(0.85))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        assertThat(saved.getAnnualPremium()).isEqualByComparingTo(expectedAnnual);
        assertThat(saved.getCurrency()).isEqualTo("EUR");
        assertThat(saved.getCoverageDetails()).contains("Standard glasses or contact lenses");
    }

    @Test
    void computeOffer_doesNotThrow_whenRecordNotFound() {
        when(formRecordRepository.findByFormNumber("missing")).thenReturn(Optional.empty());

        assertThatCode(() -> workerService.computeOffer("missing", formData))
                .doesNotThrowAnyException();
    }

    @Test
    void generateMonthlyPrice_withinConfiguredRange() {
        for (int i = 0; i < 50; i++) {
            BigDecimal price = workerService.generateMonthlyPrice();
            assertThat(price).isBetween(new BigDecimal("75.00"), new BigDecimal("100.00"));
        }
    }

    @Test
    void generateMonthlyPrice_handlesEqualMinAndMax() {
        ReflectionTestUtils.setField(workerService, "minMonthlyPrice", 90.0);
        ReflectionTestUtils.setField(workerService, "maxMonthlyPrice", 90.0);

        BigDecimal price = workerService.generateMonthlyPrice();

        assertThat(price).isEqualByComparingTo("90.00");
    }

    @Test
    void generateMonthlyPrice_handlesSwappedMinAndMax() {
        ReflectionTestUtils.setField(workerService, "minMonthlyPrice", 200.0);
        ReflectionTestUtils.setField(workerService, "maxMonthlyPrice", 150.0);

        BigDecimal price = workerService.generateMonthlyPrice();

        assertThat(price).isBetween(new BigDecimal("150.00"), new BigDecimal("200.00"));
    }

    // ── buildCoverageDetails ──────────────────────────────────────────────────

    @Test
    void buildCoverageDetails_allFields() {
        List<String> details = workerService.buildCoverageDetails(formData);

        assertThat(details).hasSize(5);
        assertThat(details).anyMatch(d -> d.contains("Standard glasses or contact lenses"));
        assertThat(details).anyMatch(d -> d.contains("Just maintenance"));
        assertThat(details).anyMatch(d -> d.contains("1–2 sessions/year"));
        assertThat(details).anyMatch(d -> d.contains("Private room preferred"));
        assertThat(details).anyMatch(d -> d.contains("GP + Specialist referral"));
    }

    @Test
    void buildCoverageDetails_nothingOptical_noDental_neverMedicine() {
        formData.setOpticalNeeds("nothing");
        formData.setDentalNeeds("none");
        formData.setAlternativeMedicine("never");
        formData.setHospitalisationPreference("shared");
        formData.setDoctorChoice("specialist_private");

        List<String> details = workerService.buildCoverageDetails(formData);

        assertThat(details).anyMatch(d -> d.contains("No optical coverage"));
        assertThat(details).anyMatch(d -> d.contains("No dental coverage"));
        assertThat(details).anyMatch(d -> d.contains("Not included"));
        assertThat(details).anyMatch(d -> d.contains("Shared room"));
        assertThat(details).anyMatch(d -> d.contains("Direct specialist access (private)"));
    }

    @Test
    void buildCoverageDetails_progressiveLenses_majorDental_moreThanThree() {
        formData.setOpticalNeeds("progressive");
        formData.setDentalNeeds("standard");
        formData.setAlternativeMedicine("more_than_three");
        formData.setHospitalisationPreference("private_essential");
        formData.setDoctorChoice("specialist_standard");

        List<String> details = workerService.buildCoverageDetails(formData);

        assertThat(details).anyMatch(d -> d.contains("Progressive lenses"));
        assertThat(details).anyMatch(d -> d.contains("Standard dental care"));
        assertThat(details).anyMatch(d -> d.contains("3+ sessions/year"));
        assertThat(details).anyMatch(d -> d.contains("Private room essential"));
        assertThat(details).anyMatch(d -> d.contains("Direct specialist access (standard)"));
    }

    @Test
    void buildCoverageDetails_surgerySurgeryAndMajorDental() {
        formData.setOpticalNeeds("surgery");
        formData.setDentalNeeds("major");

        List<String> details = workerService.buildCoverageDetails(formData);

        assertThat(details).anyMatch(d -> d.contains("Eye surgery coverage"));
        assertThat(details).anyMatch(d -> d.contains("Major dental works"));
    }

    @Test
    void buildCoverageDetails_nullFields_returnsEmptyList() {
        formData.setOpticalNeeds(null);
        formData.setDentalNeeds(null);
        formData.setAlternativeMedicine(null);
        formData.setHospitalisationPreference(null);
        formData.setDoctorChoice(null);

        List<String> details = workerService.buildCoverageDetails(formData);

        assertThat(details).isEmpty();
    }
}
