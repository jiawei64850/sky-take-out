package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    /**
     * 新增菜品
     * @param dishDTO
     */
    @Transactional // 0. start transaction, involving operation of CRUD
    public void addDish(DishDTO dishDTO) {
        // 1. construct data of basic information of dish, put it into table of dish
        Dish dish = new Dish();
        // copy the attribute
        BeanUtils.copyProperties(dishDTO, dish);
        // call mapper to store method
        dishMapper.insert(dish);
        // TODO get dish id
        log.info("dishId: {}", dish.getId());

        // 2. construct the list of flavor of dish, put it into table of dish_flavor
        List<DishFlavor> dishFlavorList = dishDTO.getFlavors();
        // 2.1 associate the id of dish
        dishFlavorList.forEach(dishFlavor -> {
            dishFlavor.setDishId(dish.getId());
        });
        // 2.2 call the mapper to store method with batch insert
        dishFlavorMapper.insertBatch(dishFlavorList);
    }

    /**
     * 分页查询菜品列表
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult page(DishPageQueryDTO dishPageQueryDTO) {
        // 1. set the parameter of split page
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        // 2. call the mapper of list search method, transfer type to Page compulsorily
        Page<DishVO> page = dishMapper.list(dishPageQueryDTO);
        // 3. get the PageResult encapsulation object and return it
        return new PageResult(page.getTotal(), page.getResult());
    }


}
