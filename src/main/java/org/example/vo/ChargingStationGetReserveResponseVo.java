package org.example.vo;

import lombok.Data;

import java.util.List;

@Data
public class ChargingStationGetReserveResponseVo {
    private Long id;
    private List<Integer> unScheduledTime;
}
