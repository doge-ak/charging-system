package org.example.po;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "charging_station")
public class ChargingStationPo {
    @Id
    @GeneratedValue
    private Long id;
    private Double longitude;
    private Double latitude;
    private Integer queuedVehicleCount;
    private Double currentElectricityPrice;
}
