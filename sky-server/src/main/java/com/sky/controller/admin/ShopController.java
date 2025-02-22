package com.sky.controller.admin;


import com.sky.constant.StatusConstant;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;


@Api(tags="店铺接口")
@Slf4j
@RequestMapping("//admin/shop")
@RestController
public class ShopController {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 设置店铺状态
     * @param status
     * @return
     */
    @ApiOperation("设置店铺状态")
    @PutMapping("/{status}")
    public Result setStatus(@PathVariable Integer status){
        log.info("设置店铺状态: {}", status);
        redisTemplate.opsForValue().set(StatusConstant.SHOP_STATUS, status);
        return Result.success();
    }

    /**
     * 获取店铺状态
     * @return
     */
    @ApiOperation("获取店铺状态")
    @GetMapping("/status")
    public Result getStatus(){
        log.info("获取店铺状态...");
        Integer status = (Integer) redisTemplate.opsForValue().get(StatusConstant.SHOP_STATUS);
        return Result.success(status == null ? 0 : status);
    }
}
