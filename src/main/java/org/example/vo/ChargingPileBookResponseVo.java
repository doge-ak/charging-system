package org.example.vo;

import lombok.Data;

import java.util.List;

@Data
public class ChargingPileBookResponseVo {
    private Long id;
    private List<Integer> unScheduledHours;
}
