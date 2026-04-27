package com.arete.webapi.service;

import com.arete.webapi.dto.FormData;
import com.arete.webapi.dto.OfferResponse;
import com.arete.webapi.exception.OfferNotFoundException;
import com.arete.webapi.exception.OfferNotReadyException;
import com.arete.webapi.model.FormRecord;
import com.arete.webapi.repository.FormRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormServiceTest {

    @Mock
    private FormRecordRepository formRecordRepository;

    @Mock
    private OfferWorkerService offerWorkerService;

    @InjectMocks
    private FormService formService;

    private FormData formData;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(formService, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(formService, "offerWaitMs", 0L);

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

    // ── saveLeaveEmail ────────────────────────────────────────────────────────

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

    // ── saveForm ──────────────────────────────────────────────────────────────

    @Test
    void saveForm_savesRecordAndReturnsUuid() {
        when(formRecordRepository.save(any(FormRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        String uuid = formService.saveForm(formData);

        assertThat(uuid).isNotBlank();
        ArgumentCaptor<FormRecord> captor = ArgumentCaptor.forClass(FormRecord.class);
        verify(formRecordRepository).save(captor.capture());

        FormRecord saved = captor.getValue();
        assertThat(saved.getFormNumber()).isEqualTo(uuid);
        assertThat(saved.getEmailAddress()).isEqualTo("test@example.com");
        assertThat(saved.getMonthlyPremium()).isNull();
    }

    @Test
    void saveForm_triggersAsyncWorker() {
        when(formRecordRepository.save(any(FormRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        String uuid = formService.saveForm(formData);

        verify(offerWorkerService).computeOffer(eq(uuid), eq(formData));
    }

    // ── getOffer ──────────────────────────────────────────────────────────────

    @Test
    void getOffer_returnsOffer_whenPremiumIsReady() {
        when(formRecordRepository.findByFormNumber("test-uuid")).thenReturn(Optional.of(buildRecordWithOffer("test-uuid")));

        OfferResponse response = formService.getOffer("test-uuid");

        assertThat(response.getMonthlyPremium()).isEqualTo(85.0);
        assertThat(response.getAnnualPremium()).isEqualTo(1020.0);
        assertThat(response.getCurrency()).isEqualTo("EUR");
        assertThat(response.getCoverageDetails()).contains("Optical: Standard glasses");
    }

    @Test
    void getOffer_waitsAndReturnsOffer_whenOfferBecomesReady() {
        FormRecord noOffer = new FormRecord();
        noOffer.setFormNumber("wait-uuid");

        when(formRecordRepository.findByFormNumber("wait-uuid"))
                .thenReturn(Optional.of(noOffer))
                .thenReturn(Optional.of(buildRecordWithOffer("wait-uuid")));

        OfferResponse response = formService.getOffer("wait-uuid");

        assertThat(response.getMonthlyPremium()).isEqualTo(85.0);
        verify(formRecordRepository, times(2)).findByFormNumber("wait-uuid");
    }

    @Test
    void getOffer_throwsNotReady_whenOfferStillNullAfterWait() {
        FormRecord noOffer = new FormRecord();
        noOffer.setFormNumber("slow-uuid");

        when(formRecordRepository.findByFormNumber("slow-uuid")).thenReturn(Optional.of(noOffer));

        assertThatThrownBy(() -> formService.getOffer("slow-uuid"))
                .isInstanceOf(OfferNotReadyException.class)
                .hasMessageContaining("slow-uuid");
    }

    @Test
    void getOffer_throws404_whenFormNotFound() {
        when(formRecordRepository.findByFormNumber("bad-uuid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> formService.getOffer("bad-uuid"))
                .isInstanceOf(OfferNotFoundException.class)
                .hasMessageContaining("bad-uuid");
    }

    @Test
    void getOffer_returnsCurrencyFromRecord() {
        FormRecord record = buildRecordWithOffer("c-uuid");
        record.setCurrency("USD");
        when(formRecordRepository.findByFormNumber("c-uuid")).thenReturn(Optional.of(record));

        OfferResponse response = formService.getOffer("c-uuid");

        assertThat(response.getCurrency()).isEqualTo("USD");
    }

    @Test
    void getOffer_returnsEurFallback_whenCurrencyIsNull() {
        FormRecord record = buildRecordWithOffer("null-cur-uuid");
        record.setCurrency(null);
        when(formRecordRepository.findByFormNumber("null-cur-uuid")).thenReturn(Optional.of(record));

        OfferResponse response = formService.getOffer("null-cur-uuid");

        assertThat(response.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void getOffer_returnsEmptyCoverage_whenCoverageDetailsIsNull() {
        FormRecord record = buildRecordWithOffer("no-cov-uuid");
        record.setCoverageDetails(null);
        when(formRecordRepository.findByFormNumber("no-cov-uuid")).thenReturn(Optional.of(record));

        OfferResponse response = formService.getOffer("no-cov-uuid");

        assertThat(response.getCoverageDetails()).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private FormRecord buildRecordWithOffer(String formNumber) {
        FormRecord record = new FormRecord();
        record.setFormNumber(formNumber);
        record.setMonthlyPremium(BigDecimal.valueOf(85.0));
        record.setAnnualPremium(BigDecimal.valueOf(1020.0));
        record.setCurrency("EUR");
        record.setCoverageDetails("[\"Optical: Standard glasses\", \"Dental: Just maintenance\"]");
        return record;
    }
}
