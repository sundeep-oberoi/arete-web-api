package com.arete.webapi.repository;

import com.arete.webapi.model.RoomCostConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomCostConfigRepository extends JpaRepository<RoomCostConfig, Long> {
}
