package com.arete.webapi.controller;

import com.arete.webapi.dto.OfferResponse;
import com.arete.webapi.dto.RoomCostResponse;
import com.arete.webapi.exception.OfferNotFoundException;
import com.arete.webapi.exception.OfferNotReadyException;
import com.arete.webapi.exception.RoomCostExpiredException;
import com.arete.webapi.service.FormService;
import com.arete.webapi.service.RoomCostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FormController.class)
class FormControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomCostService roomCostService;

    @MockBean
    private FormService formService;

    private static final String FORM_DATA_JSON = """
            {
                "profile": "employee",
                "coverPartner": true,
                "coverChildren": false,
                "numberOfChildren": 0,
                "age": "35",
                "postcode": "75001",
                "opticalNeeds": "standard",
                "dentalNeeds": "maintenance",
                "alternativeMedicine": "one_two",
                "hospitalisationPreference": "private_preferred",
                "doctorChoice": "gp_specialist",
                "email": "test@example.com",
                "phoneNumber": "0612345678"
            }
            """;

    @Test
    void postRoomCost_returns200_withCostAndCurrency() throws Exception {
        when(roomCostService.getRoomCost()).thenReturn(new RoomCostResponse(85.0, "EUR"));

        mockMvc.perform(post("/room-cost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FORM_DATA_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageDailyRoomCost").value(85.0))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void postRoomCost_returns500_whenConfigIsExpired() throws Exception {
        when(roomCostService.getRoomCost()).thenThrow(
                new RoomCostExpiredException(LocalDate.of(2020, 1, 1)));

        mockMvc.perform(post("/room-cost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FORM_DATA_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void postSaveLeaveEmail_returns204_onSuccess() throws Exception {
        String requestBody = """
                {
                    "email": "test@example.com",
                    "formData": %s
                }
                """.formatted(FORM_DATA_JSON);

        doNothing().when(formService).saveLeaveEmail(anyString(), any());

        mockMvc.perform(post("/save-leave-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());
    }

    @Test
    void postSaveLeaveEmail_returns400_whenEmailIsMissing() throws Exception {
        String requestBody = """
                {
                    "email": "",
                    "formData": %s
                }
                """.formatted(FORM_DATA_JSON);

        mockMvc.perform(post("/save-leave-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email is required"));
    }

    @Test
    void postSaveLeaveEmail_returns400_whenFormDataIsMissing() throws Exception {
        String requestBody = """
                {
                    "email": "test@example.com"
                }
                """;

        mockMvc.perform(post("/save-leave-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Form data is required"));
    }

    @Test
    void postSaveForm_returns200_withUuid() throws Exception {
        when(formService.saveForm(any())).thenReturn("test-uuid-1234");

        mockMvc.perform(post("/save-form")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FORM_DATA_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value("test-uuid-1234"));
    }

    @Test
    void postSaveForm_returns500_whenServiceThrowsException() throws Exception {
        when(formService.saveForm(any())).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/save-form")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FORM_DATA_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getOffer_returns200_withPremiumDetails() throws Exception {
        OfferResponse offerResponse = new OfferResponse(
                85.0, 1020.0, "EUR",
                List.of("Optical: Standard glasses or contact lenses", "Dental: Just maintenance")
        );
        when(formService.getOffer(eq("test-uuid-1234"))).thenReturn(offerResponse);

        mockMvc.perform(get("/offer/test-uuid-1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPremium").value(85.0))
                .andExpect(jsonPath("$.annualPremium").value(1020.0))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.coverageDetails[0]").value("Optical: Standard glasses or contact lenses"));
    }

    @Test
    void getOffer_returns404_whenNotFound() throws Exception {
        when(formService.getOffer(eq("nonexistent"))).thenThrow(new OfferNotFoundException("nonexistent"));

        mockMvc.perform(get("/offer/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getOffer_returns202_whenOfferNotReady() throws Exception {
        when(formService.getOffer(eq("not-ready"))).thenThrow(new OfferNotReadyException("not-ready"));

        mockMvc.perform(get("/offer/not-ready"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("pending"));
    }
}
