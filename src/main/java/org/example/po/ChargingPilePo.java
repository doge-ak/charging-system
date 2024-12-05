package org.example.po;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.example.util.ChargingPileStatus;

/**
 * 充电桩数据库实体
 */
@Data
@Entity
@Table(name = "charging_pile")
public class ChargingPilePo {
    /**
     * 充电桩id
     */
    @Id
    @GeneratedValue
    private Long id;

    /**
     * 充电站id
     */
    private Long chargingStationId;

    /**
     * 充电桩状态
     */
    private ChargingPileStatus status;

    /**
     * 充电桩最大功率
     */
    private Double maxPowerKw;
}
