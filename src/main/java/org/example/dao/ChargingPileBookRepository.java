package org.example.dao;

import org.example.po.ChargingPileBookPo;
import org.example.util.BookStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ChargingPileBookRepository extends ListCrudRepository<ChargingPileBookPo, Long> {
    @Query("select p from ChargingPileBookPo p where p.chargingPileId in :chargingPileIds and function('date',p.bookTime) = :bookDate")
    List<ChargingPileBookPo> findAllByChargingPileIdInAndBookTime_Date(Collection<Long> chargingPileIds, LocalDate bookDate);

    List<ChargingPileBookPo> findAllByUserId(Long userId);

    List<ChargingPileBookPo> findAllByBookStatusAndBookTimeBefore(BookStatus bookStatus, LocalDateTime bookTimeBefore);

    List<ChargingPileBookPo> findAllByBookStatus(BookStatus bookStatus);
}
