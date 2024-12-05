package org.example.po;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.example.util.BookStatus;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "charging_pile_book")
public class ChargingPileBookPo {
    @Id
    @GeneratedValue
    private Long id;
    private Long userId;
    private Long chargingPileId;
    private LocalDateTime bookTime;
    private BookStatus bookStatus;
}
