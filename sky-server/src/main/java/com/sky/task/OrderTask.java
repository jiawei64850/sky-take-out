package com.sky.task;

import com.sky.constant.MessageConstant;
import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务类：处理超时未付款和一直派送中的订单
 */
@Slf4j
@Component
public class OrderTask {
    @Autowired
    private OrdersMapper ordersMapper;

    /**
     * 每分钟检查一个是否存在超时未支付订单（超过15分钟表示超时），需要修改状态为已取消
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void processOverTimeOrder() {
        log.info("检查[超时未支付]订单");
        // 1. check the database of orders table,
        // and get the data which status = 1 and order_time < current time - 15
        LocalDateTime time = LocalDateTime.now().minusMinutes(15);
        List<Orders> ordersList = ordersMapper.getByStatsandOrderTime(Orders.PENDING_PAYMENT, time);
        // 2. update the status to 6 (cancelled)
        if (ordersList != null || ordersList.size() >= 0) {
            ordersList.forEach(orders -> {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelTime(LocalDateTime.now());
                orders.setCancelReason(MessageConstant.CANCEL_BY_OVERTIME);
                ordersMapper.update(orders);
            });
        }
    }

    /**
     * 每天凌晨1点检查一遍订单表，查看是否存在“配送中”的订单，如有修改状态为“已完成”
     */
    @Scheduled(cron = "0 0 1 * * ?")
    // @Scheduled(cron = "0 45 18 * * ?") // for test
    public void processDeliveryOrder() {
        log.info("检查[配送中]订单");
        // 1. check the database of orders table,
        // and get the data which status = 4 and order_time < current time - 1h
        LocalDateTime time = LocalDateTime.now().minusHours(1);
        List<Orders> ordersList = ordersMapper.getByStatsandOrderTime(Orders.DELIVERY_IN_PROGRESS, time);
        // 2. update the status to 6 (cancelled)
        if (ordersList != null || ordersList.size() >= 0) {
            ordersList.forEach(orders -> {
                orders.setStatus(Orders.COMPLETED);
                orders.setDeliveryTime(LocalDateTime.now());
                ordersMapper.update(orders);
            });
        }
    }

}
