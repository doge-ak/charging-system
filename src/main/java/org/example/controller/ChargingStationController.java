package org.example.controller;

import jakarta.annotation.Resource;
import org.example.service.ChargingStationService;
import org.example.util.Result;
import org.example.vo.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
public class ChargingStationController {
    @Resource
    private ChargingStationService chargingStationService;

    @GetMapping("/v1/station/getAllStationInfo")
    public Result<List<NearestChargingStationResponseVo>> nearestChargingStation(NearestChargingStationRequestVo param) {
        List<NearestChargingStationResponseVo> resp = chargingStationService.nearestChargingStation(param);
        return Result.success(resp);
    }

    @GetMapping("/v1/station/getBookList")
    public Result<ChargingPileBookResponseVo> getBookList(@RequestParam("id") Long chargingStationId) {
        ChargingPileBookResponseVo resp = chargingStationService.getBookList(chargingStationId);
        return Result.success(resp);
    }

    @PostMapping("/v1/station/addBook")
    public Result<Void> addBook(@RequestBody ChargingStationBookAddRequestVo param) {
        chargingStationService.addBook(param);
        return Result.success();
    }

    @GetMapping("/v1/station/getBook")
    public Result<List<GetBookResponseVo>> getBook(@RequestParam("userId") Long userId) {
        List<GetBookResponseVo> book = chargingStationService.getBook(userId);
        return Result.success(book);
    }

    @PostMapping("/v1/station/useBook")
    public Result<Void> useBook(@RequestParam("bookId") Long bookId) {
        chargingStationService.useBook(bookId);
        return Result.success();
    }
}
