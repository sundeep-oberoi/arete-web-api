package com.arete.webapi.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "room_cost_config")
public class RoomCostConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "average_daily_room_cost", nullable = false)
    private BigDecimal averageDailyRoomCost;

    @Column(name = "valid_upto_date", nullable = false)
    private LocalDate validUptoDate;

    @Column(name = "currency", nullable = false)
    private String currency;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getAverageDailyRoomCost() { return averageDailyRoomCost; }
    public void setAverageDailyRoomCost(BigDecimal averageDailyRoomCost) {
        this.averageDailyRoomCost = averageDailyRoomCost;
    }

    public LocalDate getValidUptoDate() { return validUptoDate; }
    public void setValidUptoDate(LocalDate validUptoDate) { this.validUptoDate = validUptoDate; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
