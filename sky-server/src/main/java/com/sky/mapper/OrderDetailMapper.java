package com.sky.mapper;

import com.sky.entity.OrderDetail;

import java.util.List;


public interface OrderDetailMapper {
    /**
     * 批量插入订单明细数据
     * @param orderDetailList
     */

    void insertBatch(List<OrderDetail> orderDetailList);
}
