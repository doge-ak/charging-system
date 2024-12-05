package org.example.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GetBookResponseVo {
    private Long bookId;
    private Long userId;
    private Long chargingPileId;
    private Long chargingStationId;
    private LocalDateTime bookTime;
    private Double maxPowerKw;
    private Double longitude;
    private Double latitude;
    private Double currentElectricityPrice;
}
