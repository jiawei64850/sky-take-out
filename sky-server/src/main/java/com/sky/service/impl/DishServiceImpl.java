package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
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

    /**
     * 删除菜品
     * @param ids
     */
    @Transactional // multiple operation
    public void delete(List<Long> ids) {
        // 1. check if status is on before delete
        ids.forEach(id -> {
            Dish dish = dishMapper.selectById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        });
        // 2. check if associate with setmeal or not before delete
        Integer count = setmealDishMapper.countByDishId(ids);
        if (count > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        // 3. delete the basic information of dish
        dishMapper.deleteBatch(ids);

        // 4. delete the information of list of flavor of dish
        dishFlavorMapper.deleteBatch(ids);

    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    public DishVO getById(Long id) {
        // 1. construct DishVO
        DishVO dishVO = new DishVO();
        // 2. get the basic information for dish based on id, and encapsulate it into DishVO
        Dish dish = dishMapper.selectById(id);
        BeanUtils.copyProperties(dish, dishVO);
        // 3. get the flavor list based on dishId, and encapsulate it into DishVO
        List<DishFlavor> flavors = dishFlavorMapper.selectByDishId(id);
        dishVO.setFlavors(flavors);

        // 4. return DishVO
        return dishVO;
    }

    /**
     * 修改菜品
     * @param dishDTO
     */
    @Transactional // 0. multiple operation need transactional anno
    public void update(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 1. update the basic information for dish: dish table
        dishMapper.update(dish);
        // 2. update or delete flavor list of dish: dish_flavor table
        // For simplification, it could delete old flavors and insert the new ones,
        // due to that user probably create, update and delete flavors
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            // associate the id of dish
            flavors.forEach(flavor -> {
                flavor.setDishId(dish.getId());
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<DishVO> getByCategoryId(Long categoryId) {
        // 1. get the information for dishes based on category_id
        List<Dish> dishes = dishMapper.getByCategoryId(categoryId);
        // 2. encapsulate each dish into DishVO
        List<DishVO> dishVOes = dishes.stream().map(dish -> {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);
            return dishVO;
        }).collect(Collectors.toList());
        // 3. return the whole dishVO list
        return dishVOes;
    }




}
