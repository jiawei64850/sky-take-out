package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import io.lettuce.core.dynamic.annotation.Key;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface OrdersMapper {
    
    /**
     * 添加订单
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号和用户id查询订单
     * @param orderNumber
     * @param userId
     */
    @Select("select * from orders where number = #{orderNumber} and user_id= #{userId}")
    Orders getByNumberAndUserId(String orderNumber, Long userId);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 订单页面查询
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 通过id查询订单
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 根据订单状态计算订单总数
     * @param status
     * @return
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer getCount(int status);

    /**
     * 根据状态和下单时间查询订单
     * @param status
     * @param time
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{time}")
    List<Orders> getByStatsandOrderTime(Integer status, LocalDateTime time);

    /**
     * 通过状态和下单时间获得当日总营业额
     * @param map
     * @return
     */
    @Select("select sum(amount) from orders where status = #{status} and order_time between #{beginTime} and #{endTime}")
    Double sumByMap(Map map);

    /**
     * 计算订单数据
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    /**
     * 查询销量排名top10
     * @param map
     * @return
     */
    @MapKey("name")
    List<GoodsSalesDTO> sumTop10(Map map);
}
