package org.example.po;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.example.util.BookStatus;

import java.time.LocalDateTime;


/**
 * 充电桩预定数据库实体
 */
@Data
@Entity
@Table(name = "charging_pile_book")
public class ChargingPileBookPo {
    /**
     * 预定id
     */
    @Id
    @GeneratedValue
    private Long id;

    /**
     * 预定的用户id
     */
    private Long userId;

    /**
     * 预定的充电桩id
     */
    private Long chargingPileId;

    /**
     * 预定的时间
     */
    private LocalDateTime bookTime;

    /**
     * 预定的状态
     */
    private BookStatus bookStatus;
}
