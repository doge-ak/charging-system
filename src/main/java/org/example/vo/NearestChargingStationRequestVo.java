package org.example.vo;

import lombok.Data;

@Data
public class NearestChargingStationRequestVo {
    private Double longitude;
    private Double latitude;
    private Double radiusKm;
    private Integer count = 5;
}
