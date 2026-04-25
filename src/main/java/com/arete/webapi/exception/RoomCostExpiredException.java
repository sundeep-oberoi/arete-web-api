package com.arete.webapi.exception;

import java.time.LocalDate;

public class RoomCostExpiredException extends RuntimeException {

    public RoomCostExpiredException(LocalDate expiredDate) {
        super("Room cost configuration expired on " + expiredDate + ". Please update the room cost data.");
    }
}
