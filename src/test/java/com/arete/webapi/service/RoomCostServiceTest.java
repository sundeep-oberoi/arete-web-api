package com.arete.webapi.service;

import com.arete.webapi.dto.RoomCostResponse;
import com.arete.webapi.exception.RoomCostExpiredException;
import com.arete.webapi.model.RoomCostConfig;
import com.arete.webapi.repository.RoomCostConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomCostServiceTest {

    @Mock
    private RoomCostConfigRepository roomCostConfigRepository;

    @InjectMocks
    private RoomCostService roomCostService;

    private RoomCostConfig validConfig;

    @BeforeEach
    void setUp() {
        validConfig = new RoomCostConfig();
        validConfig.setId(1L);
        validConfig.setAverageDailyRoomCost(BigDecimal.valueOf(85.00));
        validConfig.setValidUptoDate(LocalDate.now().plusYears(1));
        validConfig.setCurrency("EUR");
    }

    @Test
    void getRoomCost_returnsCorrectResponse_whenConfigIsValid() {
        when(roomCostConfigRepository.findAll()).thenReturn(List.of(validConfig));

        RoomCostResponse response = roomCostService.getRoomCost();

        assertThat(response.getAverageDailyRoomCost()).isEqualTo(85.00);
        assertThat(response.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void getRoomCost_throwsRoomCostExpiredException_whenConfigIsExpired() {
        validConfig.setValidUptoDate(LocalDate.now().minusDays(1));
        when(roomCostConfigRepository.findAll()).thenReturn(List.of(validConfig));

        assertThatThrownBy(() -> roomCostService.getRoomCost())
                .isInstanceOf(RoomCostExpiredException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void getRoomCost_throwsIllegalStateException_whenNoConfigExists() {
        when(roomCostConfigRepository.findAll()).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> roomCostService.getRoomCost())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No room cost configuration");
    }

    @Test
    void getRoomCost_returnsCorrectCost_whenValidUptoDateIsToday() {
        validConfig.setValidUptoDate(LocalDate.now());
        when(roomCostConfigRepository.findAll()).thenReturn(List.of(validConfig));

        RoomCostResponse response = roomCostService.getRoomCost();

        assertThat(response.getAverageDailyRoomCost()).isEqualTo(85.00);
    }
}
