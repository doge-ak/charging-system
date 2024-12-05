package org.example.dao;

import org.example.po.ChargingStationPo;
import org.springframework.data.repository.ListCrudRepository;

public interface ChargingStationRepository extends ListCrudRepository<ChargingStationPo, Long> {
}
