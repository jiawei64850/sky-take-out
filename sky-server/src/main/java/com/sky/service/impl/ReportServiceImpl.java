package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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
    private OrdersMapper ordersMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;
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

    /**
     * 导出运营数据报表
     * @param response
     */
    public void export(HttpServletResponse response) {
        // 1. get the overview data from database for 30 days
        LocalDate beginTime = LocalDate.now().minusDays(30);
        LocalDate endTime = LocalDate.now().minusDays(1);
        // 1.2 get the data of overview
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(beginTime, LocalTime.MIN), LocalDateTime.of(endTime, LocalTime.MAX));
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        // 2. write data into Excel files through POI
        this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            // 2.1 create new Excel based on template
            XSSFWorkbook excel = new XSSFWorkbook(in);
            // 2.2 fill the timescale
            XSSFSheet sheet = excel.getSheet("sheet1");
            sheet.getRow(1).getCell(1).setCellValue("时间：" + beginTime + "至" + endTime);
            // 2.3 fill the turnover
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());
            // 2.4 fill the detail data
            for (int i = 0; i < 30; i++) {
                LocalDate date = beginTime.plusDays(i);
                // query business data for someday
                businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                // get the specific row
                row = sheet.getRow(7 + i);
                // set data to each cell
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }
            // 3. download file into client browser through file stream
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            // 3.1 release resource
            out.close();
            excel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
