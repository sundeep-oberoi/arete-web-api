package com.arete.webapi.dto;

public class RoomCostResponse {

    private double averageDailyRoomCost;
    private String currency;

    public RoomCostResponse(double averageDailyRoomCost, String currency) {
        this.averageDailyRoomCost = averageDailyRoomCost;
        this.currency = currency;
    }

    public double getAverageDailyRoomCost() { return averageDailyRoomCost; }
    public String getCurrency() { return currency; }
}
