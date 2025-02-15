package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;

public interface DishService {

    /**
     * 新增菜品
     * @param dishDTO
     */
    void addDish(DishDTO dishDTO);

    /**
     * 分页查询菜品列表
     * @param dishPageQueryDTO
     * @return
     */
    PageResult page(DishPageQueryDTO dishPageQueryDTO);
}
