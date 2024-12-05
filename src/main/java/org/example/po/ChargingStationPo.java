package org.example.po;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 充电站数据库实体
 */
@Data
@Entity
@Table(name = "charging_station")
public class ChargingStationPo {
    /**
     * 充电站id
     */
    @Id
    @GeneratedValue
    private Long id;

    /**
     * 充电站的经度
     */
    private Double longitude;

    /**
     * 充电站的纬度
     */
    private Double latitude;

    /**
     * 充电站排队人数
     */
    private Integer queuedVehicleCount;

    /**
     * 充电站当前电价
     */
    private Double currentElectricityPrice;
}
