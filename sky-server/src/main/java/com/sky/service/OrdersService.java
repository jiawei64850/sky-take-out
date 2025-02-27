package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrdersService {
    /**
     * C端-添加订单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * C端-订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * C端-支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * C端-历史订单查询
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    PageResult history(int page, int pageSize, Integer status);

    /**
     * C端-查询订单详情
     * @param id
     * @return
     */
    OrderVO getOrderDetail(Long id);

    /**
     * C端-取消订单
     * @param id
     */
    void cancelbyUserId(Long id) throws Exception;

    /**
     * C端-再来一单
     * @param id
     */
    void repeat(Long id);

    /**
     * B端-条件查询
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * B端-各个状态的订单数量统计
     * @return
     */
    OrderStatisticsVO statistics();

    /**
     * B端-查询订单详情
     * @param id
     * @return
     */
    OrderVO getOrderDetailByOrderId(Long id);

    /**
     * B端-接单
     * @param ordersConfirmDTO
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * B端-拒单
     * @param ordersRejectionDTO
     */
    void reject(OrdersRejectionDTO ordersRejectionDTO) throws Exception;

    /**
     * B端-取消订单
     * @param ordersCancelDTO
     */
    void cancel(OrdersCancelDTO ordersCancelDTO);

    /**
     * B端-派送订单
     * @param id
     */
    void delivery(Long id);

    /**
     * B端-完成订单
     * @param id
     */
    void complete(Long id);

    /**
     * C端-催单
     * @param id
     */
    void reminder(Long id);
}
