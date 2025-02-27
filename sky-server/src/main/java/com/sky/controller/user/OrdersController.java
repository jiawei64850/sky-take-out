package com.sky.controller.user;

import com.sky.dto.OrdersDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrdersService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController("userOrdersController")
@RequestMapping("/user/order")
@Api(tags = "订单相关接口")
public class OrdersController {
    @Autowired
    private OrdersService ordersService;

    /**
     * 添加订单
     * @param ordersSubmitDTO
     * @return
     */
    @ApiOperation("添加订单")
    @PostMapping("/submit")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("添加订单: {}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = ordersService.submit(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = ordersService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 历史订单查询
     * @param page
     * @param pageSize
     * @param status
     * @return
     * @throws Exception
     */
    @ApiOperation("历史订单查询")
    @GetMapping("/historyOrders")
    public Result<PageResult> history(int page, int pageSize, Integer status) throws Exception {
        log.info("历史订单查询: {}, {}, {}", page, pageSize, status);
        PageResult pageResult = ordersService.history(page, pageSize, status);
        return Result.success(pageResult);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     * @throws Exception
     */
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> getOrderDetail(@PathVariable Long id) throws Exception {
        log.info("查询订单详情: {}", id);
        OrderVO orderVO = ordersService.getOrderDetail(id);
        return Result.success(orderVO);
    }

    /**
     * 取消订单
     * @param id
     * @throws Exception
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result cancel(@PathVariable Long id) throws Exception {
        log.info("取消订单: {}", id);
        ordersService.cancelbyUserId(id);
        return Result.success();
    }

    /**
     * 再来一单
     * @param id
     * @return
     * @throws Exception
     */
    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result repeat(@PathVariable Long id) throws Exception {
        log.info("再来一单: {}", id);
        ordersService.repeat(id);
        return Result.success();
    }

    /**
     * 催单
     * @param id
     * @return
     * @throws Exception
     */
    @GetMapping("/reminder/{id}")
    @ApiOperation("催单")
    public Result reminder(@PathVariable Long id) throws Exception {
        log.info("催单: {}", id);
        ordersService.reminder(id);
        return Result.success();
    }
}
