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

    /**
     * 这个方法是充电站的推荐模块
     * 方法参数有用户的经度，用户的纬度，以用户为圆心的圆的半径，返回的记录数
     * 方法的返回值为推荐的充电桩列表
     */
    @Transactional
    public List<NearestChargingStationResponseVo> nearestChargingStation(NearestChargingStationRequestVo param) {
        // 查询数据库中所有的充电站
        List<ChargingStationPo> chargingStationPoList = chargingStationRepository.findAll();
        // 把所有的充电站数据放到Redis的数据结构Geo中，每个Location的name为充电站的id，point为充电站的经纬度(实际上是KV结构)
        stringRedisTemplate.opsForGeo()
            .add("charging_system:all_station_geo",
                chargingStationPoList.stream()
                    .map(it -> {
                        RedisGeoCommands.GeoLocation<String> location =
                            new RedisGeoCommands.GeoLocation<>(it.getId().toString(),
                                new Point(it.getLongitude(), it.getLatitude()));
                        return location;
                    })
                    .toList()
            );
        /*
          通过Redis的Geo的数据结构，调用Redis的Radius命令查询以用户的经纬度为圆心，给定的半径的园内的充电站列表
          按照升序排序，第一个为推荐的充电站
        */
        List<Long> ids = stringRedisTemplate.opsForGeo().radius("charging_system:all_station_geo",
                new Circle(new Point(param.getLongitude(), param.getLatitude()),
                    new Distance(param.getRadiusKm(), RedisGeoCommands.DistanceUnit.KILOMETERS)),
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                    .limit(param.getCount())
                    .sortAscending())
            .getContent()
            .stream()
            .map(it -> Long.parseLong(it.getContent().getName()))
            .toList();
        // 删除Redis中的充电站数据
        stringRedisTemplate.delete("charging_system:all_station_geo");
        // 组装返回的对象
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

    /**
     * 这个方法是获取充电站的可预约的时间列表
     * 方法参数为充电站id
     * 返回值为充电站的可预约的时间列表
     */
    @Transactional
    public ChargingPileBookResponseVo getBookList(Long chargingStationId) {
        // 查询当前充电站的所有充电桩列表
        List<ChargingPilePo> chargingPiles = chargingPileRepository.findByChargingStationId(chargingStationId);

        // 查询这些充电桩的今天的已预约列表
        List<ChargingPileBookPo> chargingPileBooks = chargingPileBookRepository.findAllByChargingPileIdInAndBookTime_Date(
            chargingPiles.stream().map(ChargingPilePo::getId).toList(),
            LocalDate.now()
        );

        /*
          通过已预约的列表计算可预约的时间，每个小时都是可预约的一个时间段，
          每个时间段只要存在充电桩为预约就可以认为这个充电站的当前时间段可预约
        */
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
        // 组转返回值对象
        ChargingPileBookResponseVo responseVo = new ChargingPileBookResponseVo();
        responseVo.setId(chargingStationId);
        responseVo.setUnScheduledHours(unScheduledHours);
        return responseVo;
    }

    /**
     * 这个方法是添加预约模块
     * 方法参数有充电站id，用户id，准备预约当天的哪一个小时
     * 方法无返回值，默认为成功预约
     */
    @Transactional
    public void addBook(ChargingStationBookAddRequestVo param) {
        // 通过用户id查询当前用户是否因为缺席次数过多被惩罚无法预约
        UserPo userPo = userRepository.findById(param.getUserId()).orElseThrow();
        if (userPo.getPunished()) {
            throw new ServiceException(500, "用户缺席次数过多，无法预定！");
        }

        // 生成预约数据库对象，添加一些参数
        ChargingPileBookPo chargingPileBookPo = new ChargingPileBookPo();
        chargingPileBookPo.setBookTime(LocalDateTime.of(LocalDate.now(), LocalTime.of(param.getBookHour(), 0, 0)));
        chargingPileBookPo.setUserId(param.getUserId());

        // 查询充电站的所有充电桩
        List<ChargingPilePo> chargingPiles = chargingPileRepository.findByChargingStationId(param.getStationId());

        // 查询这些充电桩的今天的预约列表
        List<ChargingPileBookPo> chargingPileBooks = chargingPileBookRepository.findAllByChargingPileIdInAndBookTime_Date(
            chargingPiles.stream().map(ChargingPilePo::getId).toList(),
            LocalDate.now()
        );

        // 通过今天的预约列表移除充电站的准备预约的时间段的不可预约的充电桩
        chargingPileBooks.stream()
            .filter(it -> it.getBookTime().getHour() == param.getBookHour())
            .forEach(it -> chargingPiles.removeIf(it1 -> it.getChargingPileId().equals(it1.getId())));

        // 如果可预约的充电桩列表数量为空，即为没有可预约的充电桩
        if (chargingPiles.isEmpty()) {
            throw new ServiceException(300, "没有剩余的充电桩可预定");
        }

        // 随机挑选一个充电桩，预约这个充电桩，并且把预约信息保存在数据库中
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

    /**
     * 这个方法是定时任务，每5分钟执行一次，
     */
    @Transactional
    @Scheduled(cron = "0 */5 * * * *")
    public void autoCancelBook() {
        // 查询超过65分钟预约状态为未使用的预约列表
        List<ChargingPileBookPo> books = chargingPileBookRepository.findAllByBookStatusAndBookTimeBefore(BookStatus.BOOKED, LocalDateTime.now().minusMinutes(65));

        // 把这些预约设置为自动取消，保存在数据库中
        books.forEach(it -> it.setBookStatus(BookStatus.AUTO_CANCELLED));
        chargingPileBookRepository.saveAll(books);

        // 把自动取消的预约列表按照用户id分组
        Map<Long, List<ChargingPileBookPo>> booksGroupByUserId = chargingPileBookRepository.findAllByBookStatus(BookStatus.AUTO_CANCELLED)
            .stream()
            .collect(Collectors.groupingBy(ChargingPileBookPo::getUserId, Collectors.toList()));

        // 如果用户的违约次数达到3次，则设置用户无法预约
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
