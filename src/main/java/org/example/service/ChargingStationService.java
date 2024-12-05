package org.example.service;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.example.dao.ChargingPileBookRepository;
import org.example.dao.ChargingPileRepository;
import org.example.dao.ChargingStationRepository;
import org.example.dao.UserRepository;
import org.example.po.ChargingPileBookPo;
import org.example.po.ChargingPilePo;
import org.example.po.ChargingStationPo;
import org.example.po.UserPo;
import org.example.util.BookStatus;
import org.example.util.ServiceException;
import org.example.vo.*;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 1.推荐充电桩
 * 2.预定充电桩，自动取消
 */
@Service
public class ChargingStationService {
    @Resource
    private ChargingStationRepository chargingStationRepository;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ChargingPileBookRepository chargingPileBookRepository;

    @Resource
    private ChargingPileRepository chargingPileRepository;

    @Resource
    private UserRepository userRepository;

    @Transactional
    public List<NearestChargingStationResponseVo> nearestChargingStation(NearestChargingStationRequestVo param) {
        List<ChargingStationPo> chargingStationPoList = chargingStationRepository.findAll();
        stringRedisTemplate.opsForGeo().add("charging_system:all_station_geo", chargingStationPoList.stream()
            .map(it -> {
                RedisGeoCommands.GeoLocation<String> location =
                    new RedisGeoCommands.GeoLocation<>(it.getId().toString(),
                        new Point(it.getLongitude(), it.getLatitude()));
                return location;
            })
            .toList());
        List<Long> ids = stringRedisTemplate.opsForGeo().radius("charging_system:all_station_geo",
                new Circle(new Point(param.getLongitude(), param.getLatitude()), new Distance(param.getRadiusKm(), RedisGeoCommands.DistanceUnit.KILOMETERS)), RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                    .limit(param.getCount())
                    .sortAscending())
            .getContent()
            .stream()
            .map(it -> Long.parseLong(it.getContent().getName()))
            .toList();
        stringRedisTemplate.delete("charging_system:all_station_geo");
        return ids.stream()
            .map(it -> chargingStationRepository.findById(it))
            .map(it -> {
                try {
                    return objectMapper.updateValue(new NearestChargingStationResponseVo(), it);
                } catch (JsonMappingException e) {
                    throw new RuntimeException(e);
                }
            })
            .toList();
    }

    @Transactional
    public ChargingPileBookResponseVo getBookList(Long chargingStationId) {
        List<ChargingPilePo> chargingPiles = chargingPileRepository.findByChargingStationId(chargingStationId);
        List<ChargingPileBookPo> chargingPileBooks = chargingPileBookRepository.findAllByChargingPileIdInAndBookTime_Date(
            chargingPiles.stream().map(ChargingPilePo::getId).toList(),
            LocalDate.now()
        );
        int[] unScheduledTimeTemp = new int[24];
        Arrays.fill(unScheduledTimeTemp, chargingPiles.size());
        for (ChargingPileBookPo chargingPileBook : chargingPileBooks) {
            unScheduledTimeTemp[chargingPileBook.getBookTime().getHour()]--;
        }
        List<Integer> unScheduledHours = new ArrayList<>();
        for (int i = 0; i < unScheduledTimeTemp.length; i++) {
            if (unScheduledTimeTemp[i] > 0) {
                unScheduledHours.add(i);
            }
        }
        ChargingPileBookResponseVo responseVo = new ChargingPileBookResponseVo();
        responseVo.setId(chargingStationId);
        responseVo.setUnScheduledHours(unScheduledHours);
        return responseVo;
    }

    @Transactional
    public void addBook(ChargingStationBookAddRequestVo param) {
        UserPo userPo = userRepository.findById(param.getUserId()).orElseThrow();
        if (userPo.getPunished()) {
            throw new ServiceException(500, "用户缺席次数过多，无法预定！");
        }

        ChargingPileBookPo chargingPileBookPo = new ChargingPileBookPo();
        chargingPileBookPo.setBookTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(param.getBookHour(), 0, 0)));
        chargingPileBookPo.setUserId(param.getUserId());

        List<ChargingPilePo> chargingPiles = chargingPileRepository.findByChargingStationId(param.getStationId());
        List<ChargingPileBookPo> chargingPileBooks = chargingPileBookRepository.findAllByChargingPileIdInAndBookTime_Date(
            chargingPiles.stream().map(ChargingPilePo::getId).toList(),
            LocalDate.now()
        );
        chargingPileBooks.stream()
            .filter(it -> it.getBookTime().getHour() == param.getBookHour())
            .forEach(it -> chargingPiles.removeIf(it1 -> it.getChargingPileId().equals(it1.getId())));
        if (chargingPiles.isEmpty()) {
            throw new ServiceException(300, "没有剩余的充电桩可预定");
        }
        chargingPileBookPo.setChargingPileId(chargingPiles.get(new Random().nextInt(0, chargingPiles.size())).getId());
        chargingPileBookPo.setBookStatus(BookStatus.BOOKED);
        chargingPileBookRepository.save(chargingPileBookPo);
    }

    @Transactional
    public List<GetBookResponseVo> getBook(Long userId) {
        List<ChargingPileBookPo> chargingPileBooks = chargingPileBookRepository.findAllByUserId(userId);
        List<GetBookResponseVo> resp = chargingPileBooks.stream()
            .map(it -> {
                GetBookResponseVo getBookResponseVo = new GetBookResponseVo();
                getBookResponseVo.setBookId(it.getId());
                getBookResponseVo.setUserId(userId);
                getBookResponseVo.setChargingPileId(it.getChargingPileId());
                getBookResponseVo.setBookTime(it.getBookTime());
                ChargingPilePo chargingPilePo = chargingPileRepository.findById(it.getChargingPileId()).orElseThrow();
                getBookResponseVo.setMaxPowerKw(chargingPilePo.getMaxPowerKw());
                ChargingStationPo chargingStationPo = chargingStationRepository.findById(chargingPilePo.getChargingStationId()).orElseThrow();
                getBookResponseVo.setChargingStationId(chargingStationPo.getId());
                getBookResponseVo.setLongitude(chargingStationPo.getLongitude());
                getBookResponseVo.setLatitude(chargingStationPo.getLatitude());
                getBookResponseVo.setCurrentElectricityPrice(chargingStationPo.getCurrentElectricityPrice());
                return getBookResponseVo;
            })
            .toList();
        return resp;
    }

    @Transactional
    public void useBook(Long bookId) {
        ChargingPileBookPo chargingPileBookPo = chargingPileBookRepository.findById(bookId).orElseThrow();
        LocalDateTime bookTime = chargingPileBookPo.getBookTime();
        if (!(bookTime.isBefore(LocalDateTime.now()) && bookTime.isAfter(LocalDateTime.now().plusHours(1)))) {
            throw new ServiceException(400, "不在使用充电桩时间内");
        }
        chargingPileBookPo.setBookStatus(BookStatus.USED);
        chargingPileBookRepository.save(chargingPileBookPo);
    }

    @Transactional
    @Scheduled(cron = "0 */5 * * * *")
    public void autoCancelBook() {
        List<ChargingPileBookPo> books = chargingPileBookRepository.findAllByBookStatusAndBookTimeBefore(BookStatus.BOOKED, LocalDateTime.now().minusMinutes(65));
        books.forEach(it -> it.setBookStatus(BookStatus.AUTO_CANCELLED));
        chargingPileBookRepository.saveAll(books);
        Map<Long, List<ChargingPileBookPo>> booksGroupByUserId = chargingPileBookRepository.findAllByBookStatus(BookStatus.AUTO_CANCELLED)
            .stream()
            .collect(Collectors.groupingBy(ChargingPileBookPo::getUserId, Collectors.toList()));
        for (final Map.Entry<Long, List<ChargingPileBookPo>> userBook : booksGroupByUserId.entrySet()) {
            if (userBook.getValue().size() >= 3) {
                UserPo userPo = userRepository.findById(userBook.getKey()).orElseThrow();
                if (!userPo.getPunished()) {
                    userPo.setPunished(true);
                    userRepository.save(userPo);
                }
            }
        }
    }

}
