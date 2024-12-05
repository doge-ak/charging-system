package org.example.po;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.example.util.ChargingPileStatus;

@Data
@Entity
@Table(name = "charging_pile")
public class ChargingPilePo {
    @Id
    @GeneratedValue
    private Long id;
    private Long chargingStationId;
    private ChargingPileStatus status;
    private Double maxPowerKw;
}
