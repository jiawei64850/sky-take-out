package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrdersService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrdersServiceImpl implements OrdersService {
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Value("${sky.shop.address}")
    private String shopAddress;
    @Value("${sky.baidu.ak}")
    private String ak;
    /**
     * 添加订单 -- 将订单数据存入表中（orders、order_detail）
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional // activate transaction
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        // 0.1 get the information of consignee by searching address book table
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new OrderBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        // 0.1.1 check if out of delivery range
        checkOutofRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());

        // 0.2 get the information of user by searching user table
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new OrderBusinessException(MessageConstant.USER_NOT_LOGIN);
        }
        // 0.3 get the information shopping cart of current user
        List<ShoppingCart> cartList = shoppingCartMapper.list(userId);
        if (cartList == null || cartList.size() == 0) {
            throw new OrderBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        // 1. construct data of order and put those into orders table
        Orders orders = new Orders();
        // 1.1 copy the attribute from DTO
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        // 1.2 add the missed attribute for orders object
        orders.setNumber(System.currentTimeMillis()+"");
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setPhone(addressBook.getPhone());
        orders.setAddress(addressBook.getDetail());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserName(user.getName());
        ordersMapper.insert(orders);
        log.info("订单id: {}", orders.getId());
        // 2. construct data of details of order and put those into order_detail table
        List<OrderDetail> orderDetailList = new ArrayList<>();
        // 2.1 construct order details by for loop
        cartList.forEach(cart -> {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail, "id");
            // associate the id of order
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        });
        // 2.2 insert list into order_detail table in batch
        orderDetailMapper.insertBatch(orderDetailList);
        // 3. clean the shopping cart (only for current user of his own)
        shoppingCartMapper.clean(userId);
        // 4. construct VO object and return it

        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderAmount(orders.getAmount())
                .orderNumber(orders.getNumber())
                .build();
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
//        // 当前登录用户id
//        Long userId = BaseContext.getCurrentId();
//        User user = userMapper.selectById(userId);
//
//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
//
//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));
//
//        return vo;
        // ------------------------------------------------------------------------
        // the above code of block couldn't work due to constriction of payment layer
        // for simulating payment successfully -- change the status of order
        paySuccess(ordersPaymentDTO.getOrderNumber());
        // return an empty order
        return new OrderPaymentVO();
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单号查询当前用户的订单
        Orders ordersDB = ordersMapper.getByNumberAndUserId(outTradeNo, userId);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        ordersMapper.update(orders);
    }

    /**
     * 历史订单查询
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult history(int pageNum, int pageSize, Integer status) {
        // start page query
        PageHelper.startPage(pageNum, pageSize);
        // construct the DTO
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());

        // call the mapper to query
        Page<Orders> page = ordersMapper.pageQuery(ordersPageQueryDTO);

        // add the attribute of orderDetailList
        List<OrderVO> list = new ArrayList<>();
        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                Long orderId = orders.getId();

                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情
     * @param orderId
     * @return
     */
    public OrderVO getOrderDetail(Long orderId) {
        // 1. get the orders by mapper with orders id
        Orders orders = ordersMapper.getById(orderId);
        // 2. get the orderDetailList by order id;
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);
        // 3. construct the OrderVO and copy the attribute from orders
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        // 3.1 add the orderDetailList into orderVO
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     */
    public void cancelbyUserId(Long id) throws Exception {
        Orders ordersDB = ordersMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (ordersDB.getStatus() > 2) { // greater than 2 means abnormal order status
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(id);

        if (ordersDB.getStatus() == Orders.TO_BE_CONFIRMED) {
            // refund if status is unconfirmed
            // but couldn't work due to constriction of payment layer, so comment this code of block
//            weChatPayUtil.refund(
//                    ordersDB.getNumber(), //商户订单号
//                    ordersDB.getNumber(), //商户退款单号
//                    new BigDecimal(0.01),//退款金额，单位 元
//                    new BigDecimal(0.01));//原订单金额
            orders.setStatus(Orders.REFUND);
        }

        // add the other proper attribute to the order
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason("订单取消");
        ordersMapper.update(orders);
    }

    /**
     * 再来一单
     * @param id
     */
    public void repeat(Long id) {
        // simulate the request sending from shopping cart

        // 1. get the userId
        Long userId = BaseContext.getCurrentId();

        // 2. get the orderDetail by orderId
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 3. transfer orderDetailList into ShoppingCartList
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * B端-条件查询
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page page = ordersMapper.pageQuery(ordersPageQueryDTO);
        // need the string of orderDishes sometimes, and transfer orders to orderVO (list)
        List<OrderVO> list = getOrderVOListByIds(page);
        return new PageResult(page.getTotal(), list);
    }

    /**
     * B端-各个状态的订单数量统计
     * @return
     */
    public OrderStatisticsVO statistics() {
        // 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        Integer comfirmed = ordersMapper.getCount(Orders.CONFIRMED);
        Integer deliveryInProgress = ordersMapper.getCount(Orders.DELIVERY_IN_PROGRESS);
        Integer toBeConfirmed = ordersMapper.getCount(Orders.TO_BE_CONFIRMED);
        orderStatisticsVO.setConfirmed(comfirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        return orderStatisticsVO;
    }

    /**
     * B端-查询订单详情
     * @param id
     * @return
     */
    public OrderVO getOrderDetailByOrderId(Long id) {
        Orders orders = ordersMapper.getById(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * B端-接单
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        ordersMapper.update(orders);
    }

    /**
     * B端-拒单
     * @param ordersRejectionDTO
     */
    public void reject(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        Orders orderDB = ordersMapper.getById(ordersRejectionDTO.getId());
        if (orderDB == null && ! orderDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Integer PayStatus = orderDB.getPayStatus();
        if (PayStatus == Orders.PAID) {
//            String refund = weChatPayUtil.refund(
//                    orderDB.getNumber(),
//                    orderDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款: {}", refund);
        }
        Orders orders = new Orders();
        orders.setId(orderDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());
        ordersMapper.update(orders);
    }

    /**
     * B端-取消订单
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        Orders orderDB = ordersMapper.getById(ordersCancelDTO.getId());

        Integer PayStatus = orderDB.getPayStatus();
        if (PayStatus == Orders.PAID) {
//            String refund = weChatPayUtil.refund(
//                    orderDB.getNumber(),
//                    orderDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款: {}", refund);
        }
        Orders orders = new Orders();
        orders.setId(orderDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        ordersMapper.update(orders);
    }

    /**
     * B端-派送订单
     * @param id
     */
    public void delivery(Long id) {
        Orders ordersDB = ordersMapper.getById(id);
        if (ordersDB == null && !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        ordersMapper.update(orders);
    }

    /**
     * B端-完成订单
     * @param id
     */
    public void complete(Long id) {
        Orders ordersDB = ordersMapper.getById(id);
        if (ordersDB == null && !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        ordersMapper.update(orders);
    }

    /**
     * B端-通过订单id获取订单列表
     * @param page
     * @return
     */
    public List<OrderVO> getOrderVOListByIds(Page page) {
        // construct orderVO list
        List<OrderVO> orderVOList = new ArrayList<>();
        // construct orders list and get those from page result
        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // put the common attribute from each order to orderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                // transfer the dishes of order into string
                String orderDishes = getOrderDishesStr(orders);
                // put string into orderVO
                orderVO.setOrderDishes(orderDishes);
                // put each orderVO into list
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * B端-通过订单获取菜品信息字符串
     * @param orders
     * @return
     */
    public String getOrderDishesStr(Orders orders) {
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
        // transfer the orderDishes (orderDetail List) into proper string (list)
        List<String> orderDishesList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());
        // adjoin string
        return String.join(",", orderDishesList);
    }

    /**
     * B端-检查客户的收货地址是否超出配送范围
     * @param address
     */
    public void checkOutofRange(String address){
        //1. get the latitude and longitude for shop address (Geocoder API)
        //1.1 construct parameter of request for Geocoder API
        Map map = new HashMap();
        map.put("address", shopAddress);
        map.put("output", "json");
        map.put("ak", ak);
        // 1.2 using tools class HttpClientUtil to send request
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);
        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException(MessageConstant.SHOP_ADDRESS_PARSE_FAIL);
        }
        // 1.3 parse the shop location
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        String shoplatlng = lat + "," + lng;
        // 2. get the latitude and longitude for user address (Geocoder API)
        // 2.1 update the address of user to the map
        map.put("address", address);
        // 2.2 using tools class HttpClientUtil to send request
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);
        jsonObject = JSON.parseObject(userCoordinate);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException(MessageConstant.USER_ADDRESS_PARSE_FAIL);
        }
        // 2.3 parse the user location
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        String userlatlng = lat + "," + lng;
        // 3. get the distance between user address and shop address based on latitude and longitude (DirectionLite API)
        // 3.1 add the other parameter for the map
        map.put("origin",shoplatlng);
        map.put("destination",userlatlng);
        map.put("steps_info","0");
        // 3.2 using tools class HttpClientUtil to send request
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);
        jsonObject = JSON.parseObject(json);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException(MessageConstant.DELIVERY_NAVIGATION_FAIL);
        }
        // 3.3 parse the distance information
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");
        if (distance != null && distance > 5000) {
            throw new OrderBusinessException(MessageConstant.OUT_OF_RANGE);
        }
    }
}
