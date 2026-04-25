package com.arete.webapi.service;

import com.arete.webapi.dto.RoomCostResponse;
import com.arete.webapi.exception.RoomCostExpiredException;
import com.arete.webapi.model.RoomCostConfig;
import com.arete.webapi.repository.RoomCostConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class RoomCostService {

    private static final Logger log = LoggerFactory.getLogger(RoomCostService.class);

    private final RoomCostConfigRepository roomCostConfigRepository;

    public RoomCostService(RoomCostConfigRepository roomCostConfigRepository) {
        this.roomCostConfigRepository = roomCostConfigRepository;
    }

    public RoomCostResponse getRoomCost() {
        log.debug("Fetching room cost configuration");

        List<RoomCostConfig> configs = roomCostConfigRepository.findAll();
        if (configs.isEmpty()) {
            log.error("No room cost configuration found in database");
            throw new IllegalStateException("No room cost configuration available");
        }

        RoomCostConfig config = configs.get(0);
        LocalDate today = LocalDate.now();

        log.debug("Room cost config: cost={}, validUpto={}, today={}", config.getAverageDailyRoomCost(),
                config.getValidUptoDate(), today);

        if (today.isAfter(config.getValidUptoDate())) {
            log.error("Room cost data expired on {}", config.getValidUptoDate());
            throw new RoomCostExpiredException(config.getValidUptoDate());
        }

        log.info("Returning room cost: {} {}", config.getAverageDailyRoomCost(), config.getCurrency());
        return new RoomCostResponse(config.getAverageDailyRoomCost().doubleValue(), config.getCurrency());
    }
}
