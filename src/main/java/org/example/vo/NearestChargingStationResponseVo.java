package org.example.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class NearestChargingStationResponseVo {
    private Long id;
    private Double longitude;
    private Double latitude;
    private Integer queuedVehicleCount;
    private BigDecimal currentElectricityPrice;
}
