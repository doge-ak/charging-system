package org.example.dao;

import org.example.po.ChargingPilePo;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public interface ChargingPileRepository extends ListCrudRepository<ChargingPilePo, Long> {
    List<ChargingPilePo> findByChargingStationId(Long chargingStationId);
}
