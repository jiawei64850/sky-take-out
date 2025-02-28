package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    OrdersMapper ordersMapper;
    @Autowired
    UserMapper userMapper;
    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        // 1. construct DateList
        List<LocalDate> dateList = getDateList(begin, end);
        // 2. construct turnoverList
        List<Double> turnoverList = new ArrayList<>();
        // 2.1 query order table, which status is completed and order_time between the begin and end of a day
        dateList.forEach(date -> {
            Map map = new HashMap();
            map.put("status", Orders.COMPLETED);
            map.put("beginTime", LocalDateTime.of(date, LocalTime.MIN)); // current day 00:00:00
            map.put("endTime", LocalDateTime.of(date, LocalTime.MAX)); // current day 23:59:59:9999999
            Double turnover = ordersMapper.sumByMap(map);
            // for the case of null turnover
            turnover = turnover == null ? 0 : turnover;
            turnoverList.add(turnover);
        });
        // 3. construct TurnoverReportVO and return it
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 获得日期列表数据
     * @param begin
     * @param end
     * @return
     */
    private static List<LocalDate> getDateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        // 1.1 insert date into list recursively
        while (begin.isBefore(end)) {
            // notice: avoid dead-end loop
            dateList.add(begin.plusDays(1));
            begin = begin.plusDays(1);
        }
        log.info("dateList: {}", dateList);
        return dateList;
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        // 1. construct DateList
        List<LocalDate> dateList = getDateList(begin, end);
        // 2. construct newUserList
        List<Integer> newUserList = new ArrayList<>();
        // 3. construct totalUserList
        List<Integer> totalUserList = new ArrayList<>();
        // get the data of new user with loop recursively
        dateList.forEach(date -> {
            Map map = new HashMap();
            map.put("beginTime", LocalDateTime.of(date, LocalTime.MIN));
            map.put("endTime", LocalDateTime.of(date, LocalTime.MAX));
            Integer newUser = userMapper.countByMap(map);
            newUserList.add(newUser);

            map.put("beginTime", null);
            Integer totalUser = userMapper.countByMap(map);
            totalUserList.add(totalUser);
        });
        // 4. construct UserReportVO and return it
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList,","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = getDateList(begin, end);
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        Integer totalOrderCount = 0;
        Integer validTotalOrderCount = 0;
        for (LocalDate date : dateList) {
            Map map = new HashMap();
            map.put("beginTime", LocalDateTime.of(date, LocalTime.MIN));
            map.put("endTime", LocalDateTime.of(date, LocalTime.MAX));
            Integer orderCount = ordersMapper.countByMap(map);
            orderCountList.add(orderCount);
            map.put("status", Orders.COMPLETED);
            Integer validOrderCount = ordersMapper.countByMap(map);
            validOrderCountList.add(validOrderCount);
            totalOrderCount += orderCount;
            validTotalOrderCount += validOrderCount;
        }
        // totalOrderCount = orderCountList.stream().reduce(0, Integer::sum);
        // validTotalOrderCount = validOrderCountList.stream().reduce(0, Integer::sum);

        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = ((double) validTotalOrderCount / totalOrderCount);
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .orderCompletionRate(orderCompletionRate)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validTotalOrderCount)
                .build();
    }

    /**
     * 查询销量排名top10
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {
        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();
        // based on status(5) and begin and end, query orders and order_detail table
        Map map = new HashMap();
        map.put("status", Orders.COMPLETED);
        map.put("beginTime", LocalDateTime.of(begin, LocalTime.MIN));
        map.put("endTime", LocalDateTime.of(end, LocalTime.MAX));
        List<GoodsSalesDTO> list = ordersMapper.sumTop10(map);
        for (GoodsSalesDTO dto : list) {
            nameList.add(dto.getName());
            numberList.add(dto.getNumber());
        }
        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }
}
