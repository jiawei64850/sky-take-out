package com.sky.controller.admin;


import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Api(tags = "套餐相关接口")
@RestController
@RequestMapping("/admin/setmeal")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;

    /**
     * 新增套餐
     * @param setmealDTO
     * @return
     */
    // CacheEvict: 清理指定分类下面的套餐缓存
    @CacheEvict(cacheNames = "setmeal", key = "#setmealDTO.categoryId")
    @PostMapping
    @ApiOperation("新增套餐")
    public Result addSetmeal(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐: {}", setmealDTO);
        setmealService.addSetmeal(setmealDTO);
        return Result.success(setmealDTO);
    }

    @GetMapping("/page")
    @ApiOperation("分页查询套餐列表")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("分页查询套餐列表: {}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.page(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐")
    public Result getById(@PathVariable Long id){
        log.info("根据id查询套餐: {}", id);
        SetmealVO setmealVO = setmealService.getById(id);
        log.info("根据id查询套餐: {}", setmealVO);
        return Result.success(setmealVO);
    }

    @CacheEvict(cacheNames = "setmeal", allEntries = true)
    @PutMapping
    @ApiOperation("修改套餐")
    public Result update(@RequestBody SetmealDTO setmealDTO){
        log.info("修改套餐: {}", setmealDTO);
        setmealService.update(setmealDTO);
        return Result.success();
    }

    /**
     * 启用/禁用套餐
     * @param status
     * @param id
     * @return
     */
    @CacheEvict(cacheNames = "setmeal", allEntries = true)
    @PostMapping("/status/{status}")
    @ApiOperation("启用/禁用套餐")
    public Result enableOrDisable(@PathVariable Integer status, Long id){
        log.info("启用/禁用套餐: {}, {}", status, id);
        setmealService.enableOrDisable(status, id);
        return Result.success();
    }

    /**
     * 删除套餐
     * @param ids
     * @return
     */
    // 清除setmeal下全部缓存数据
    @CacheEvict(cacheNames = "setmeal", allEntries = true)
    @DeleteMapping
    @ApiOperation("删除套餐")
    public Result delete(@RequestParam List<Long> ids){
        log.info("删除套餐: {}", ids);
        setmealService.delete(ids);
        return Result.success(ids);
    }
}
