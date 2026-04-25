package com.arete.webapi.controller;

import com.arete.webapi.dto.OfferResponse;
import com.arete.webapi.dto.RoomCostResponse;
import com.arete.webapi.exception.RoomCostExpiredException;
import com.arete.webapi.service.FormService;
import com.arete.webapi.service.RoomCostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FormController.class)
class FormControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void postOffer_returns200_withPremiumDetails() throws Exception {
        OfferResponse offerResponse = new OfferResponse(
                100.0, 1000.0, "EUR",
                List.of("Optical: Standard glasses or contact lenses", "Dental: Just maintenance")
        );
        when(formService.calculateOffer(any())).thenReturn(offerResponse);

        mockMvc.perform(post("/offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FORM_DATA_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPremium").value(100.0))
                .andExpect(jsonPath("$.annualPremium").value(1000.0))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.coverageDetails[0]").value("Optical: Standard glasses or contact lenses"));
    }

    @Test
    void postOffer_returns500_whenServiceThrowsException() throws Exception {
        when(formService.calculateOffer(any())).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/offer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FORM_DATA_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }
}