package org.example.vo;

import lombok.Data;

@Data
public class ChargingStationBookAddRequestVo {
    private Integer bookHour;
    private Long stationId;
    private Long userId;
}
